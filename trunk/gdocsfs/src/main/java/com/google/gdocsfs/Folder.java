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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import fuse.FuseFtypeConstants;

/**
 * TODO make doc
 *
 * @author  Jonhnny Weslley
 * @version 1.00, 10/08/2008
 * @since   1.0
 */
public class Folder extends Document {

	private final Map<String, Document> documents;

	public Folder(String name) throws IOException {
		super(null, null);
		setName(name);
		documents = new LinkedHashMap<String, Document>();
	}

	public void addDocument(Document document) {
		documents.put(document.getFullName(), document);
	}

	public Document getDocument(String documentName) {
		return documents.get(documentName);
	}

	@Override
	public String getFullName() {
		return getName();
	}

	@Override
	public Collection<Document> getDocuments() {
		return documents.values();
	}

	@Override
	public long getSize() {
		return getDocuments().size();
	}

	@Override
	public void setSize(long size) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getFileType() {
		return FuseFtypeConstants.TYPE_DIR;
	}

	@Override
	public String toString() {
		return getClass().getName()
		+ "[ name=" + getFullName() + " ]"
		+ " with " + documents.size() + " documents";
	}

}
