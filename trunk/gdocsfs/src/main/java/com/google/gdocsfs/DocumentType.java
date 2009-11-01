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

import java.net.MalformedURLException;
import java.net.URL;

/**
 * TODO make doc
 *
 * @author  Jonhnny Weslley
 * @version 1.00, 10/08/2008
 * @since   1.0
 */
public enum DocumentType {
	DOCUMENT("odt", "application/vnd.oasis.opendocument.text",
	"https://docs.google.com/feeds/download/documents/Export?exportFormat=odt&docID="),
	SPREADSHEET("ods", "application/vnd.oasis.opendocument.spreadsheet",
	"https://spreadsheets.google.com/feeds/download/spreadsheets/Export?exportFormat=ods&key="),
	PRESENTATION("ppt", "application/vnd.oasis.opendocument.presentation",
	"https://docs.google.com/feeds/download/presentations/Export?exportFormat=ppt&docID="),
	PDF("pdf", "application/pdf", "https://docs.google.com/gb?export=download&id=");

	private final String suffix;
	private final String baseURL;
	private final String mimetype;

	private DocumentType(String suffix, String mimetype, String baseURL) {
		this.suffix = suffix;
		this.mimetype = mimetype;
		this.baseURL = baseURL;
	}

	public String getSuffix() {
		return suffix;
	}

	public String getMimetype() {
		return mimetype;
	}

	public URL getDownloadURL(Document document) {
		try {
			return new URL(baseURL + document.getId());

		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

}
