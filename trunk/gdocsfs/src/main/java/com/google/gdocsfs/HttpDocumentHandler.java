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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.logging.Log;

import com.google.gdata.client.docs.DocsService;
import com.google.gdata.client.http.HttpAuthToken;
import com.google.gdata.data.docs.DocumentListEntry;
import com.google.gdata.data.media.MediaFileSource;
import com.google.gdata.util.ServiceException;

/**
 * TODO make doc
 * 
 * @author Jonhnny Weslley
 * @version 1.00, 10/08/2008
 * @since 1.0
 */
public class HttpDocumentHandler {

	static {
		URLConnection.setDefaultAllowUserInteraction(true);
	}

	private static final int BLOCK_SIZE = 4096 * 4;
	private static final String USER_AGENT = "gdocsfs";

	private final Log log;
	private final DocsService service;

	public HttpDocumentHandler(DocsService service, Log log) {
		this.log = log;
		this.service = service;
	}

	public final void download(URL source, File target) throws IOException {

		InputStream is = null;
		FileOutputStream fos = null;
		HttpURLConnection connection = null;

		try {
			connection = getConnection(source, "GET");
			connection.connect();
			is = connection.getInputStream();
			fos = new FileOutputStream(target);
			int readCount;
			byte[] data = new byte[BLOCK_SIZE];
			while ((readCount = is.read(data)) > 0) {
				fos.write(data, 0, readCount);
			}

		} finally {
			if (fos != null) { fos.flush(); fos.close(); }
			if (is != null) { is.close(); }
			if (connection != null) { connection.disconnect(); }
		}
	}

	public final void upload(File source, DocumentListEntry target, String mimetype) throws IOException {

		service.getRequestFactory().setHeader("If-Match", "*");

		target.setMediaSource(new MediaFileSource(source, mimetype));
		try {
			target.updateMedia(false);

		} catch (ServiceException e) {
			throw new IOException(e);
		}
	}

	public final int getContentLength(URL source) throws IOException {
		HttpURLConnection connection = null;
		try {
			connection = getConnection(source, "HEAD");
			connection.connect();

			{ // get content length
				int contentLength = -1;
				String contentLengthStr = connection.getHeaderField("Content-Length");
				if (contentLengthStr != null) {
					try {
						contentLength = Integer.parseInt(contentLengthStr);
					} catch (NumberFormatException e) {
						log.warn("Can't parse the content lenght.", e);
					}
				}
				return contentLength;
			}

		} finally {
			if (connection != null) { connection.disconnect(); }
		}
	}

	private HttpURLConnection getConnection(URL source, String httpMethod) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) source.openConnection();
		HttpAuthToken authToken = (HttpAuthToken) service.getAuthTokenFactory().getAuthToken();
		String header = authToken.getAuthorizationHeader(source, httpMethod);
		connection.setRequestProperty("Authorization", header);
		connection.setRequestProperty("User-Agent", USER_AGENT);
		connection.setRequestMethod(httpMethod);
		return connection;
	}

}