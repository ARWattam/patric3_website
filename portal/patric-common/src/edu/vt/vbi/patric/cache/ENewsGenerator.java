/**
 * ****************************************************************************
 * Copyright 2014 Virginia Polytechnic Institute and State University
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 */
package edu.vt.vbi.patric.cache;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ENewsGenerator {

	String sourceURL = "http://enews.patricbrc.org/php/rssAdapter.php";

	private static final Logger LOGGER = LoggerFactory.getLogger(ENewsGenerator.class);

	public void setSourceURL(String url) {
		sourceURL = url;
	}

	public boolean createCacheFile(String filePath) {

		boolean isSuccess = false;

		try (CloseableHttpClient client = HttpClientBuilder.create().build()) {

			HttpGet httpRequest = new HttpGet(sourceURL);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String strResponseBody = client.execute(httpRequest, responseHandler);

			if (strResponseBody.length() > 0) {
				PrintWriter enewsOut = new PrintWriter(new FileWriter(filePath));
				enewsOut.println(strResponseBody);
				enewsOut.close();
			}
			isSuccess = true;
		}
		catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
		return isSuccess;
	}
}
