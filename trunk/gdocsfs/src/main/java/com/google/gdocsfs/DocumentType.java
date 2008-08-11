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
DOCUMENT("https://docs.google.com/MiscCommands?command=saveasdoc&exportformat=oo&docID="),
SPREADSHEET("https://spreadsheets.google.com/ccc?output=ods&key="),
PRESENTATION("https://docs.google.com/MiscCommands?command=saveasdoc&exportFormat=ppt&docID="), 
PDF("https://docs.google.com/gb?export=download&id=");

	private String baseURL;

	private DocumentType(String baseURL) {
		this.baseURL = baseURL;
	}

	public URL getURL(Document document) {
		try {
			return new URL(baseURL + document.getId());

		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

}
