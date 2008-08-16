/*
 * Copyright 2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gdocsfs;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;

import com.google.gdata.client.docs.DocsService;
import com.google.gdata.client.http.HttpAuthToken;

/**
 * TODO make doc
 * 
 * @author Jonhnny Weslley
 * @version 1.00, 10/08/2008
 * @since 1.0
 */
public class DocumentHandler {

	private static final String USER_AGENT = "gdocsfs";

	private final Log log;
	private final Document document;
	private final DocsService service;

	public DocumentHandler(DocsService service, Document document, Log log) {
		this.log = log;
		this.document = document;
		this.service = service;
	}

	public int getContentLength() throws IOException {
		HttpURLConnection connection = null;
		try {
			connection = getConnection("HEAD");
			connection.connect();
			return getContentLength(connection);

		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private int getContentLength(HttpURLConnection conn) {
		String contentLengthStr = conn.getHeaderField("Content-Length");
		int contentLength = -1;
		if (contentLengthStr != null) {
			try {
				contentLength = Integer.parseInt(contentLengthStr);
			} catch (NumberFormatException e) {
				log.warn("Can't parse the content lenght.", e);
			}
		}
		return contentLength;
	}

	public void read(ByteBuffer buf, long offset) throws IOException {
		HttpURLConnection connection = getConnection("GET");
		connection.connect();
		InputStream is = connection.getInputStream();

		int capacity = buf.capacity();
		byte[] buffer = new byte[capacity];
		int readCount = is.read(buffer, (int) offset, capacity);
		buf.put(buffer, (int) offset, readCount);
	}

	private HttpURLConnection getConnection(String httpMethod) throws IOException {
		HttpURLConnection.setDefaultAllowUserInteraction(true);

		URL source = document.getDownloadURL();
		HttpURLConnection connection = (HttpURLConnection) source.openConnection();
		HttpAuthToken authToken = (HttpAuthToken) service.getAuthTokenFactory().getAuthToken();
		String header = authToken.getAuthorizationHeader(source, httpMethod);
		connection.setRequestProperty("Authorization", header);
		connection.setRequestProperty("User-Agent", USER_AGENT);
		connection.setRequestMethod(httpMethod);
		return connection;
	}

	public void release() {
		log.debug("  " + this + " released");
	}

	@Override
	protected void finalize() {
		log.debug("  " + this + " finalized");
	}

	@Override
	public String toString() {
		return "DocumentHandler[" + document + ", hashCode=" + hashCode() + "]";
	}

}