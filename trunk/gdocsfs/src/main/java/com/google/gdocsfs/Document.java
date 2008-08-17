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

import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import fuse.FuseFtypeConstants;

/**
 * TODO make doc
 * 
 * @author Jonhnny Weslley
 * @version 1.00, 10/08/2008
 * @since 1.0
 */
public class Document {

	private String id;
	private String name;
	private long lastUpdated;
	private DocumentType type;
	private long size;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(long lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public DocumentType getDocumentType() {
		return type;
	}

	public void setDocumentType(DocumentType type) {
		this.type = type;
	}

	public URL getDownloadURL() {
		return type.getURL(this);
	}

	public String getMimetype() {
		return type.getMimetype();
	}

	public int getFileType() {
		return FuseFtypeConstants.TYPE_FILE;
	}

	public Collection<Document> getDocuments() {
		return Collections.emptyList();
	}

	@Override
	public String toString() {
		return getClass().getName() + "[ name=" + name + " ]";
	}

}
