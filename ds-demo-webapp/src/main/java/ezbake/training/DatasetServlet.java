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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;
//import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

//import ezbake.configuration.EzConfiguration;
//import ezbake.security.client.EzbakeSecurityClient;
//import ezbake.thrift.ThriftClientPool;

public class DatasetServlet extends HttpServlet {
	public static final String COLLECTION_NAME = "ds_dataset_demo";
	public static final String USER_FIELD_NAME = "user";
	public static final String USERNAME_FIELD_NAME = "userName";
	public static final String SCREEN_NAME_FIELD_NAME = "screenName";
	public static final String SECONDARY_SCREEN_NAME_FIELD_NAME = "screen_name";
	public static final String TEXT_FIELD_NAME = "text";

	private static final long serialVersionUID = 9051600090960237717L;

	protected static Logger logger = LoggerFactory
			.getLogger(DatasetServlet.class);

	// private static EzbakeSecurityClient securityClient;
	// private ThriftClientPool pool;

	public void destroy() {
		try {
			MongoDatasetClient.getInstance().close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void init() throws ServletException {
		try {
			MongoDatasetClient client = MongoDatasetClient.getInstance();

			logger.info("Initializing mongo db client, COLLECTION_NAME: {}",
					COLLECTION_NAME);

			// if collection doesn't exist, create it.
			if (!client.collectionExists(COLLECTION_NAME)) {
				logger.info("collection doesn't exist, we need to create the collection.");
				client.createCollection(COLLECTION_NAME);
			}

			createTextIndex(client);

			// final Properties props = new EzConfiguration().getProperties();
			// this.pool = new ThriftClientPool(props);
			// this.securityClient = new EzbakeSecurityClient(props);
		} catch (Exception e) {
			logger.error("Error during initialization", e);
			throw new ServletException(e.getMessage());
		}
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		doPost(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		String action = request.getParameter("action");

		String result;
		if ("insertText".equalsIgnoreCase(action)) {
			result = insertText(request, response);
		} else if ("search".equalsIgnoreCase(action)) {
			result = search(request, response);
		} else {
			result = "Unknown action: " + action;
		}

		response.setHeader("Content-Type", "text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();
		out.println(result);
	}

	/**
	 * Creates a text index on the "text" field if it doesn't exist
	 *
	 * @throws TException
	 */
	private void createTextIndex(MongoDatasetClient client) throws TException {
		boolean hasTextIndex = false;
		String namespace = null;

		logger.info("getting index info..");

		List<String> indexList = client.getIndexInfo(COLLECTION_NAME);
		for (String index : indexList) {
			logger.info("we have an index: {}", index);

			DBObject indexObj = (DBObject) JSON.parse(index);
			String indexName = (String) indexObj.get("name");
			if (namespace == null) {
				namespace = (String) indexObj.get("ns");
			}

			if (indexName.equals(TEXT_FIELD_NAME + "_text")) {
				hasTextIndex = true;
			}
		}

		if (!hasTextIndex) {
			DBObject obj = new BasicDBObject();
			// we are putting a text index on the "text" field in the mongo
			// collection
			obj.put(TEXT_FIELD_NAME, "text");
			String jsonKeys = JSON.serialize(obj);

			logger.info(
					"creating text index with jsonKeys: {}, COLLECTION_NAME: {}",
					jsonKeys, COLLECTION_NAME);

			client.createIndex(COLLECTION_NAME, jsonKeys, null);

			logger.info("DatasetServlet: created text index: {}", jsonKeys);
		} else {
			logger.info("DatasetServlet: we already have the text index.");
		}
	}

	private String search(HttpServletRequest request,
			HttpServletResponse response) {
		String searchText = request.getParameter("searchText");
		String dataset = request.getParameter("dataset");
		String result;
		List<String> data = new ArrayList<String>();

		logger.info("searchText: {}", searchText);

		try {
			switch (dataset) {
			case "mongo":
			{
				MongoDatasetClient client = MongoDatasetClient.getInstance();
				createTextIndex(client);
				data = client.searchText(COLLECTION_NAME, searchText);
				break;
			}
			case "elastic":
			{
				ElasticDatasetClient client = ElasticDatasetClient.getInstance();
				data = client.searchText(COLLECTION_NAME, searchText);
				break;
			}
			case "postgres":
			{
				PostgresDatasetClient client = PostgresDatasetClient.getInstance();
				data = client.searchText(searchText);
				break;
			}
			default:
				throw new IllegalArgumentException("Invalid dataset: "
						+ dataset);
			}

			if (data.size() == 0) {
				result = "No results found.";
			} else {
				StringBuilder buffer = new StringBuilder();

				for (String recordJSON : data) {

					buffer.append("<tr>");
					buffer.append("<td>");
					buffer.append(recordJSON);
					buffer.append("</td>");
					buffer.append("</tr>");
				}

				result = buffer.toString();
				logger.info(result);
			}
		} catch (Exception e) {
			result = "Unable to retrieve any results: " + e.getMessage();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	private String insertText(HttpServletRequest request,
			HttpServletResponse response) {
		String textContent = request.getParameter("content");
		String dataset = request.getParameter("dataset");
		String visibility = request.getParameter("visibility");
		String group = request.getParameter("group");
		String result = null;

		visibility = visibility.isEmpty() ? "U" : visibility;

		try {
			switch (dataset) {
			case "mongo":
			{
				MongoDatasetClient client = MongoDatasetClient.getInstance();
				client.insertText(COLLECTION_NAME, textContent, visibility, group);
				break;
			}
			case "elastic":
			{
				ElasticDatasetClient client = ElasticDatasetClient.getInstance();
				client.insertText(textContent, visibility, group);
				break;
			}
			case "postgres":
			{
				PostgresDatasetClient client = PostgresDatasetClient.getInstance();
				client.insertText(textContent, visibility, group);
				break;
			}
			default:
				throw new IllegalArgumentException("Invalid dataset: "
						+ dataset);
			}

			result = "Successfully added the text (" + textContent + ") to "
					+ dataset;
		} catch (Exception e) {
			result = "Failed to insert data: " + e.toString();
			logger.error("Failed to insert data", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

		return result;
	}

}
