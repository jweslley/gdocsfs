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
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gdata.client.docs.DocsService;
import com.google.gdata.data.docs.DocumentListEntry;
import com.google.gdata.data.docs.DocumentListFeed;
import com.google.gdata.util.ServiceException;

import fuse.Errno;
import fuse.Filesystem3;
import fuse.FuseDirFiller;
import fuse.FuseException;
import fuse.FuseGetattrSetter;
import fuse.FuseOpenSetter;
import fuse.FuseSizeSetter;
import fuse.FuseStatfsSetter;
import fuse.XattrLister;
import fuse.XattrSupport;

/**
 * TODO make doc
 * 
 * @author Jonhnny Weslley
 * @version 1.00, 10/08/2008
 * @since 1.0
 */
public class GoogleDocsFS implements Filesystem3, XattrSupport {


	private static final String ATTR_MIMETYPE = "mimetype";
	private static final int DEFAULT_MODE = 0666;
	private static final int NAME_LENGTH = 1024;
	static final String DOCUMENTS_FEED = "http://docs.google.com/feeds/documents/private/full";
	static final int BLOCK_SIZE = 1024 * 8;

	final Log log = LogFactory.getLog(GoogleDocsFS.class);
	private final Folder root;
	private final DocsService service;
	private final URL documentListFeedUrl;

	public GoogleDocsFS(String username, String password) throws IOException,
			ServiceException {
		service = new DocsService("gdocsfs");
		service.setUserCredentials(username, password);
		documentListFeedUrl = new URL(DOCUMENTS_FEED);

		DocumentListFeed feed = service.getFeed(documentListFeedUrl,
				DocumentListFeed.class);
		List<DocumentListEntry> entries = feed.getEntries();

		root = new Folder("");

		for (DocumentListEntry entry : entries) {
			Document document = new Document();
			String shortId = entry.getId().substring(
					entry.getId().lastIndexOf('/') + 1);
			String id = shortId.split("%3A")[1];
			String type = shortId.split("%3A")[0];
			document.setId(id);
			document.setName(entry.getTitle().getPlainText());
			document.setLastUpdated(entry.getUpdated().getValue());
			document.setDocumentType(DocumentType.valueOf(type.toUpperCase()));
			document.setSize(getDocumentSize(document));
			root.addDocument(document);
		}
		root.setSize(getDocumentSize(root));

		log.info("created");
	}


	// Supported operations

	public int getattr(String path, FuseGetattrSetter getattrSetter) {
		Document document = getDocument(path);

		if (document == null) {
			return Errno.ENOENT;
		}

		long size = document.getSize();
		int time = (int) (System.currentTimeMillis() / 1000L);
		int mtime = (int) (document.getLastUpdated() / 1000);
		getattrSetter.set(document.hashCode(), 
				document.getFileType() | DEFAULT_MODE,
				1, 0, 0, 0,
				size , (size + BLOCK_SIZE - 1) / BLOCK_SIZE,
				mtime, mtime, time);
		return 0;
	}

	public int getdir(String path, FuseDirFiller filler) {
		Document document = getDocument(path);

		if (document == null) {
			return Errno.ENOENT;
		}

		for (Document child : document.getDocuments()) {
			filler.add(child.getName(), child.hashCode(),
					child.getFileType() | DEFAULT_MODE);
		}
		return 0;
	}

	public int statfs(FuseStatfsSetter statfsSetter) {
		statfsSetter.set(BLOCK_SIZE, 1000, 200, 180,
				root.getDocuments().size(), 0, NAME_LENGTH);
		return 0;
	}

	public int open(String path, int flags, FuseOpenSetter openSetter) {
		Document document = getDocument(path);

		if (document == null) {
			return Errno.ENOENT;
		}

		openSetter.setKeepCache(true);
		openSetter.setFh(new DocumentHandler(service, document, log));
		return 0;
	}

	public int read(String path, Object fh, ByteBuffer buf, long offset) {
		if (fh instanceof DocumentHandler) {
			try {
				((DocumentHandler) fh).read(buf, offset);
				return 0;

			} catch (IOException e) {
				return Errno.EIO;
			}
		}
		return Errno.EBADF;
	}

	public int flush(String path, Object fh) {
		return fsync(path, fh, false);
	}

	public int fsync(String path, Object fh, boolean isDatasync) {
		return (fh instanceof DocumentHandler) ? 0 : Errno.EBADF;
	}

	public int release(String path, Object fh, int flags) {
		if (fh instanceof DocumentHandler) {
			((DocumentHandler) fh).release();
			System.runFinalization();
			return 0;
		}

		return Errno.EBADF;
	}

	private Document getDocument(String path) {
		if (path.equals("/")) {
			return root;
		}

		File file = new File(path);
		Document parent = getDocument(file.getParent());
		Document node = (parent instanceof Folder) ? ((Folder) parent)
				.getDocument(file.getName()) : null;

		if (log.isDebugEnabled()) {
			log.info("  lookup(\"" + path + "\") returning: " + node);
		}

		return node;
	}

	private long getDocumentSize(Document document) throws IOException {
		if (document instanceof Folder) {
			return document.getDocuments().size() * NAME_LENGTH;
		}

		DocumentHandler handler = new DocumentHandler(service, document, log);
		return handler.getContentLength();
	}


	// Unsupported operations

	public int chmod(String path, int mode) {
		return 0;
	}

	public int chown(String path, int uid, int gid) {
		return 0;
	}

	public int utime(String path, int atime, int mtime) {
		return 0;
	}

	public int readlink(String path, CharBuffer link) {
		return Errno.ENOENT;
	}


	// Read-only file system operations

	public int write(String path, Object fh, boolean isWritepage,
			ByteBuffer buf, long offset) throws FuseException {
		if (fh instanceof DocumentHandler) {
			try {
				((DocumentHandler) fh).write(buf, offset, isWritepage);
				return 0;

			} catch (IOException e) {
				return Errno.EIO;
			}
		}
		return Errno.EBADF;
	}

	public int link(String from, String to) {
		return Errno.EROFS;
	}

	public int mkdir(String path, int mode) {
		return Errno.EROFS;
	}

	public int mknod(String path, int mode, int rdev) {
		return Errno.EROFS;
	}

	public int rename(String from, String to) {
		return Errno.EROFS;
	}

	public int rmdir(String path) {
		return Errno.EROFS;
	}

	public int symlink(String from, String to) {
		return Errno.EROFS;
	}

	public int truncate(String path, long size) {
		return 0;
	}

	public int unlink(String path) {
		return Errno.EROFS;
	}


	// XattrSupport implementation

	@Override
	public int getxattr(String path, String name, ByteBuffer dst) {
		Document document = getDocument(path);

		if (document == null) {
			return Errno.ENOENT;
		}

		if (ATTR_MIMETYPE.equals(name)) {
			dst.put(document.getMimetype().getBytes());
			return 0;
		}

		return Errno.ENOATTR;
	}

	@Override
	public int getxattrsize(String path, String name, FuseSizeSetter sizeSetter) {
		Document document = getDocument(path);

		if (document == null) {
			return Errno.ENOENT;
		}

		if (ATTR_MIMETYPE.equals(name)) {
			sizeSetter.setSize(document.getMimetype().getBytes().length);
			return 0;
		}

		return Errno.ENOATTR;
	}

	@Override
	public int listxattr(String path, XattrLister lister) {
		Document document = getDocument(path);

		if (document == null) {
			return Errno.ENOENT;
		}

		lister.add(ATTR_MIMETYPE);
		return 0;
	}

	@Override
	public int removexattr(String path, String name) {
		return Errno.EROFS;
	}

	@Override
	public int setxattr(String path, String name, ByteBuffer value, int flags) {
		return Errno.EROFS;
	}

}