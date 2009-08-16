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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.util.UUID;

/**
 * TODO make doc
 * 
 * @author Jonhnny Weslley
 * @version 1.00, 10/08/2008
 * @since 1.0
 */
public class DocumentHandler {

	private File tempFile;
	private boolean wasModified;
	private FileChannel channel;
	private final Document document;

	public DocumentHandler(Document document) {
		this.document = document;
	}

	public final boolean isOpen() {
		return (channel != null) && channel.isOpen();
	}

	private void checkIsOpen() throws IOException {
		if (!isOpen()) {
			throw new ClosedChannelException();
		}
	}

	public final DocumentHandler open() throws IOException {

		String uuid = UUID.randomUUID().toString();
		tempFile = File.createTempFile(uuid, document.getDocumentType().getSuffix());
		document.downloadTo(tempFile);
		channel = (new RandomAccessFile(tempFile, "rw")).getChannel();
		return this;
	}

	public final void read(ByteBuffer buffer, long offset) throws IOException {
		checkIsOpen();
		channel.position(offset);
		channel.read(buffer);
	}

	public final void write(ByteBuffer buffer, long offset) throws IOException {
		checkIsOpen();
		channel.position(offset);
		channel.write(buffer);
		document.setSize(channel.size());
		wasModified = true;
	}

	public final void truncate(long size) throws IOException {
		checkIsOpen();
		channel.truncate(size);
		document.setSize(channel.size());
		wasModified = true;
	}

	public final void release() throws IOException {
		checkIsOpen();

		try {
			if (wasModified) {
				document.uploadFrom(tempFile);
			}

			tempFile.delete();

		} finally {
			channel.close();
			channel = null;
		}
	}

	@Override
	public final String toString() {
		return "DocumentHandler[" + document + ", hashCode=" + hashCode() + "]";
	}

}