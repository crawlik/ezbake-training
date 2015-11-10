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

import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TSimpleJSONProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ezbake.configuration.EzConfiguration;
import ezbake.configuration.constants.EzBakePropertyConstants;
import ezbake.data.common.ThriftClient;
import ezbake.data.mongo.thrift.EzMongo;
import ezbake.data.mongo.thrift.MongoEzbakeDocument;
import ezbake.groups.thrift.EzGroups;
import ezbake.groups.thrift.EzGroupsConstants;
import ezbake.groups.thrift.Group;
import ezbake.security.client.EzbakeSecurityClient;
import ezbake.thrift.ThriftClientPool;
import ezbake.base.thrift.AdvancedMarkings;
import ezbake.base.thrift.EzSecurityToken;
import ezbake.base.thrift.PlatformObjectVisibilities;
import ezbake.base.thrift.Visibility;

public class MongoDatasetClient {
	private static final String EZMONGO_SERVICE_NAME = "ezmongo";
	private static final String APP_NAME = "ds_demo";
	private static final Logger logger = LoggerFactory
			.getLogger(MongoDatasetClient.class);

	private static MongoDatasetClient instance;
	private String app_name;

	private ThriftClientPool pool;
	private EzbakeSecurityClient securityClient;

	private MongoDatasetClient() {
		createClient();
	}

	public static MongoDatasetClient getInstance() {
		if (instance == null) {
			instance = new MongoDatasetClient();
		}
		return instance;
	}

	public EzMongo.Client getThriftClient() throws TException {
		return pool.getClient(this.app_name, EZMONGO_SERVICE_NAME,
				EzMongo.Client.class);
	}

	public void close() throws Exception {
		ThriftClient.close();
	}

	public void createIndex(String collectionName, String jsonKeys,
			String jsonOptions) throws TException {
		EzMongo.Client c = null;

		try {
			EzSecurityToken token = securityClient.fetchTokenForProxiedUser();

			c = getThriftClient();
			logger.info("Calling EzMongo creating index for {}...",
					collectionName);
			c.createIndex(collectionName, jsonKeys, jsonOptions, token);
			logger.info("Index created.");
		} finally {
			if (c != null) {
				pool.returnToPool(c);
			}
		}
	}

	public List<String> getIndexInfo(String collectionName) throws TException {
		EzMongo.Client c = null;

		try {
			EzSecurityToken token = securityClient.fetchTokenForProxiedUser();

			c = getThriftClient();

			logger.info("Calling EzMongo getting index info for {}...",
					collectionName);
			return c.getIndexInfo(collectionName, token);
		} finally {
			if (c != null) {
				pool.returnToPool(c);
			}
		}
	}

	public boolean collectionExists(String collectionName) throws TException {
		EzMongo.Client c = null;

		try {
			EzSecurityToken token = securityClient.fetchTokenForProxiedUser();

			c = getThriftClient();

			logger.info("Calling EzMongo checking collection {}...",
					collectionName);
			boolean exists = c.collectionExists(collectionName, token);
			logger.info("collection {} exists: {}", collectionName, exists);

			return exists;
		} finally {
			if (c != null) {
				pool.returnToPool(c);
			}
		}
	}

	public void createCollection(String collectionName) throws TException {
		EzMongo.Client c = null;

		try {
			EzSecurityToken token = securityClient.fetchTokenForProxiedUser();

			c = getThriftClient();

			logger.info("Calling EzMongo creating collection {}...",
					collectionName);
			c.createCollection(collectionName, token);
			logger.info("Created collection {}", collectionName);
		} finally {
			if (c != null) {
				pool.returnToPool(c);
			}
		}
	}

	public List<String> searchText(String collectionName, String searchText)
			throws TException {
		EzMongo.Client c = null;
		List<String> results;

		try {
			EzSecurityToken token = securityClient.fetchTokenForProxiedUser();

			c = getThriftClient();

			logger.info("Calling EzMongo searching text for {}...", searchText);
			results = c.textSearch(collectionName, searchText, token);
			logger.info("Text search results: {}", results);
		} finally {
			if (c != null) {
				pool.returnToPool(c);
			}
		}
		return results;
	}

	public void insertText(String collectionName, String text,
			String inputVisibility, String group) throws TException {
		EzMongo.Client c = null;
		EzGroups.Client groupClient = null;

		try {
			EzSecurityToken token = securityClient.fetchTokenForProxiedUser();
			c = getThriftClient();
			logger.info("Calling EzMongo insertText for {}...", collectionName);

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

			Visibility visibility = new Visibility();

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
				} catch (Exception ex) {
					ex.printStackTrace();
					throw ex;
				}
			}

			visibility.setFormalVisibility(inputVisibility);
			String result = c.insert(collectionName, new MongoEzbakeDocument(
					jsonContent, visibility), token);
			logger.info("Successful mongo client insert {}", result);
		} finally {
			if (c != null) {
				pool.returnToPool(c);
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
