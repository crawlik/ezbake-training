/*   Copyright (C) 2013-2014 Computer Sciences Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

package ezbake.training;

import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.ArrayList;

import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TSimpleJSONProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ezbake.configuration.EzConfiguration;
import ezbake.configuration.constants.EzBakePropertyConstants;
import ezbake.data.common.ThriftClient;
import ezbake.data.elastic.thrift.EzElastic;
import ezbake.data.elastic.thrift.Document;
import ezbake.data.elastic.thrift.SearchResult;
import ezbake.data.elastic.thrift.Query;
import ezbake.groups.thrift.EzGroups;
import ezbake.groups.thrift.EzGroupsConstants;
import ezbake.groups.thrift.Group;
import ezbake.security.client.EzbakeSecurityClient;
import ezbake.thrift.ThriftClientPool;
import ezbake.base.thrift.AdvancedMarkings;
import ezbake.base.thrift.EzSecurityToken;
import ezbake.base.thrift.PlatformObjectVisibilities;
import ezbake.base.thrift.Visibility;

import org.elasticsearch.index.query.QueryBuilders;

public class ElasticDatasetClient {
	private static final String EZELASTIC_SERVICE_NAME = "ezelastic";
	private static final String APP_NAME = "ds_demo";
	private static final Logger logger = LoggerFactory
			.getLogger(ElasticDatasetClient.class);

	private static ElasticDatasetClient instance;
	private String app_name;

	private ThriftClientPool pool;
	private EzbakeSecurityClient securityClient;

	private ElasticDatasetClient() {
		createClient();
	}

	public static ElasticDatasetClient getInstance() {
		if (instance == null) {
			instance = new ElasticDatasetClient();
		}
		return instance;
	}

	public EzElastic.Client getThriftClient() throws TException {
		return pool.getClient(this.app_name, EZELASTIC_SERVICE_NAME,
				EzElastic.Client.class);
	}

	public void close() throws Exception {
		ThriftClient.close();
	}

	public List<String> searchText(String collectionName, String searchText)
			throws TException {
		EzElastic.Client elasticClient = null;
		List<String> results = new ArrayList<String>();

		try {
			EzSecurityToken token = securityClient.fetchTokenForProxiedUser();

			elasticClient = getThriftClient();

			logger.info("Query EzElastic text for {}...", searchText);
			final SearchResult result = elasticClient.query(new Query(
					QueryBuilders.termQuery("text", searchText).toString()),
					token);
			for (Document doc : result.getMatchingDocuments()) {
				AdvancedMarkings advanched = doc.visibility
						.getAdvancedMarkings();
				String am = (advanched != null) ? advanched.toString() : "N/A";
				results.add(doc.get_jsonObject() + ":"
						+ doc.visibility.getFormalVisibility() + ":" + am);
			}
			logger.info("Text search results: {}", results);
		} finally {
			if (elasticClient != null) {
				pool.returnToPool(elasticClient);
			}
		}
		return results;
	}

	public void insertText(String text, String inputVisibility, String group)
			throws TException {
		EzElastic.Client elasticClient = null;
		EzGroups.Client groupClient = null;

		try {
			EzSecurityToken token = securityClient.fetchTokenForProxiedUser();
			elasticClient = getThriftClient();

			Tweet tweet = new Tweet();
			tweet.setTimestamp(System.currentTimeMillis());
			tweet.setId(0);
			tweet.setText(text);
			tweet.setUserId(1);
			tweet.setUserName("test");
			tweet.setIsFavorite(new Random().nextBoolean());
			tweet.setIsRetweet(new Random().nextBoolean());

			TSerializer serializer = new TSerializer(
					new TSimpleJSONProtocol.Factory());
			String jsonContent = serializer.toString(tweet);

			final Document doc = new Document();
			doc.set_id(UUID.randomUUID().toString());
			doc.set_type("TEST");
			doc.set_jsonObject(jsonContent);

			Visibility visibility = new Visibility();
			visibility.setFormalVisibility(inputVisibility);
			
			System.out.println(group);

			if (!group.equals("none")) {
				groupClient = pool.getClient(EzGroupsConstants.SERVICE_NAME,
						EzGroups.Client.class);
				try {
					Group ezgroup;
					try {
						EzSecurityToken groupsToken = securityClient
								.fetchTokenForProxiedUser(pool
										.getSecurityId(EzGroupsConstants.SERVICE_NAME));
						ezgroup = groupClient.getGroup(groupsToken, group);
					} catch (org.apache.thrift.transport.TTransportException e) {
						throw new TException("User is not part of : " + group);
					}
					PlatformObjectVisibilities platformObjectVisibilities = new PlatformObjectVisibilities();
					platformObjectVisibilities
							.addToPlatformObjectWriteVisibility(ezgroup.getId());
					platformObjectVisibilities
							.addToPlatformObjectReadVisibility(ezgroup.getId());
					AdvancedMarkings advancedMarkings = new AdvancedMarkings();
					advancedMarkings
							.setPlatformObjectVisibility(platformObjectVisibilities);
					visibility.setAdvancedMarkings(advancedMarkings);
					visibility.setAdvancedMarkingsIsSet(true);
				} catch (TException ex) {
					ex.printStackTrace();
					throw ex;
				}
			}

			doc.setVisibility(visibility);
			elasticClient.put(doc, token);

			logger.info("Successful elastic client insert");
		} finally {
			if (elasticClient != null) {
				pool.returnToPool(elasticClient);
			}
			if (groupClient != null) {
				pool.returnToPool(groupClient);
			}
		}
	}

	void createClient() {
		try {
			EzConfiguration configuration = new EzConfiguration();
			Properties properties = configuration.getProperties();
			logger.info("in createClient, configuration: {}", properties);

			securityClient = new EzbakeSecurityClient(properties);
			pool = new ThriftClientPool(configuration.getProperties());
			this.app_name = properties.getProperty(
					EzBakePropertyConstants.EZBAKE_APPLICATION_NAME, APP_NAME);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
