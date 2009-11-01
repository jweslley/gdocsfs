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

	private boolean wasModified;
	private final File tempFile;
	private final FileChannel channel;
	private final Document document;

	private DocumentHandler(Document document, File tempFile) throws IOException {
		this.document = document;
		this.tempFile = tempFile;
		channel = (new RandomAccessFile(tempFile, "rw")).getChannel();
	}

	public final static DocumentHandler open(Document document) throws IOException {
		String uuid = UUID.randomUUID().toString();
		File tempFile = File.createTempFile(uuid, document.getDocumentType().getSuffix());
		document.downloadTo(tempFile);
		return new DocumentHandler(document, tempFile);
	}

	public final boolean isOpen() {
		return channel.isOpen();
	}

	private void checkIsOpen() throws IOException {
		if (!isOpen()) {
			throw new ClosedChannelException();
		}
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
		}
	}

	@Override
	public final String toString() {
		return "DocumentHandler[" + document + ", hashCode=" + hashCode() + "]";
	}

}