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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gdata.client.docs.DocsService;
import com.google.gdata.client.http.HttpAuthToken;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.docs.DocumentEntry;
import com.google.gdata.data.docs.DocumentListEntry;
import com.google.gdata.data.docs.DocumentListFeed;
import com.google.gdata.data.docs.PresentationEntry;
import com.google.gdata.data.docs.SpreadsheetEntry;
import com.google.gdata.data.media.MediaFileSource;
import com.google.gdata.util.ServiceException;

/**
 * TODO make doc
 *
 * @author Jonhnny Weslley
 * @version 1.00, 10/08/2008
 * @since 1.0
 */
public class GoogleDocs {

	static {
		URLConnection.setDefaultAllowUserInteraction(true);
	}

	private static final Log log = LogFactory.getLog(GoogleDocs.class);
	private static final String DOCUMENTS_URL = "http://docs.google.com/feeds/documents/private/full";
	private static final String USER_AGENT = "gdocsfs";
	private static final int BLOCK_SIZE = 4096 * 8;

	private final Folder root;
	private final DocsService service;

	public GoogleDocs(String username, String password) throws IOException, ServiceException {
		service = new DocsService("gdocsfs");
		service.setUserCredentials(username, password);
		URL documentListFeedUrl = new URL(DOCUMENTS_URL);

		DocumentListFeed feed = service.getFeed(documentListFeedUrl, DocumentListFeed.class);
		List<DocumentListEntry> entries = feed.getEntries();

		log.info(username + " has " + entries.size() + " documents");
		root = new Folder("");
		root.setLastUpdated(System.currentTimeMillis());
		for (DocumentListEntry entry : entries.subList(0, 1)) {
			root.addDocument(new Document(this, entry));
		}

		log.info("ready to use");
	}

	public Document getDocument(String path) {
		if (path.equals("/")) {
			return root;
		}

		File file = new File(path);
		Document parent = getDocument(file.getParent());
		return (parent instanceof Folder) ? ((Folder) parent).getDocument(file.getName()) : null;
	}

	public void newDocument(String path) throws MalformedURLException, IOException, ServiceException {
		File file = new File(path);
		String name = file.getName();
		int indexOf = name.indexOf('.');
		String suffix = indexOf > 0 ? name.substring(indexOf + 1) : "";
		name = indexOf > 0 ? name.substring(0, indexOf) : "";
		System.out.println(name);
		System.out.println(suffix);
		DocumentListEntry newEntry = null;
		if (DocumentType.DOCUMENT.getSuffix().equals(suffix)) {
			newEntry = new DocumentEntry();

		} else if (DocumentType.PRESENTATION.getSuffix().equals(suffix)) {
			newEntry = new PresentationEntry();

		} else if (DocumentType.SPREADSHEET.getSuffix().equals(suffix)) {
			newEntry = new SpreadsheetEntry();
		}
		System.out.println(newEntry);
		newEntry.setTitle(new PlainTextConstruct(name));

		DocumentListEntry entry = service.insert(new URL(DOCUMENTS_URL), newEntry);
		root.addDocument(new Document(this, entry));
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