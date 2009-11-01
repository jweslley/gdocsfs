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
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

import fuse.Errno;
import fuse.Filesystem3;
import fuse.FilesystemConstants;
import fuse.FuseContext;
import fuse.FuseDirFiller;
import fuse.FuseException;
import fuse.FuseGetattrSetter;
import fuse.FuseMount;
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
public final class GoogleDocsFS implements Filesystem3, XattrSupport {

	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(GoogleDocsFS.class);

	private static final String ATTR_MIMETYPE = "mimetype";
	private static final int DEFAULT_MODE = 0777;
	private static final int NAME_LENGTH = 1024;
	private static final int BLOCK_SIZE = 1024 * 8;

	private final GoogleDocs docs;

	public GoogleDocsFS(GoogleDocs docs) {
		this.docs = docs;
	}

	private Document getDocument(String path) {
		return docs.getDocument(path);
	}


	// Supported operations

	public int statfs(FuseStatfsSetter statfsSetter) {
		statfsSetter.set(BLOCK_SIZE, 1000, 200, 180,
				getDocument("/").getDocuments().size(), 0, NAME_LENGTH);
		return 0;
	}

	public int getattr(String path, FuseGetattrSetter getattrSetter) {
		Document document = getDocument(path);
		if (document == null) {
			return Errno.ENOENT;
		}

		try {
			long size = document.getSize();
			int time = (int) (System.currentTimeMillis() / 1000);
			int mtime = (int) (document.getLastUpdated() / 1000);
			FuseContext context = FuseContext.get();
			getattrSetter.set(document.hashCode(),
					document.getFileType() | DEFAULT_MODE,
					1, context.uid, context.gid, 0,
					size , (size + BLOCK_SIZE - 1) / BLOCK_SIZE,
					mtime, mtime, time);
			return 0;

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return Errno.EIO;
		}
	}

	public int getdir(String path, FuseDirFiller filler) {
		Document document = getDocument(path);
		if (document == null) {
			return Errno.ENOENT;
		}

		for (Document child : document.getDocuments()) {
			filler.add(child.getFullName(), child.hashCode(),
					child.getFileType() | DEFAULT_MODE);
		}
		return 0;
	}

	public int open(String path, int flags, FuseOpenSetter openSetter) {
		Document document = getDocument(path);
		if (document == null) {
			return Errno.ENOENT;
		}

		if ((flags & FilesystemConstants.O_RDWR) != 0) {
			log.info("open rw");
		} else if ((flags & FilesystemConstants.O_WRONLY) != 0) {
			log.info("open w");
		} else {
			log.info("open r");
		}

		openSetter.setKeepCache(true);
		try {
			openSetter.setFh(DocumentHandler.open(document));

		} catch (IOException e) {
			return Errno.EIO;
		}
		return 0;
	}

	public int read(String path, Object fh, ByteBuffer buf, long offset) {
		if (!(fh instanceof DocumentHandler)) {
			return Errno.EBADF;
		}

		try {
			((DocumentHandler) fh).read(buf, offset);
			return 0;

		} catch (IOException e) {
			return Errno.EIO;
		}
	}

	public int write(String path, Object fh, boolean isWritepage,
			ByteBuffer buf, long offset) throws FuseException {
		if (!(fh instanceof DocumentHandler)) {
			return Errno.EBADF;
		}

		try {
			((DocumentHandler) fh).write(buf, offset);
			return 0;

		} catch (IOException e) {
			log.error(e);
			return Errno.EIO;
		}
	}

	public int flush(String path, Object fh) {
		return fsync(path, fh, false);
	}

	public int fsync(String path, Object fh, boolean isDatasync) {
		return (fh instanceof DocumentHandler) ? 0 : Errno.EBADF;
	}

	public int release(String path, Object fh, int flags) {
		if (!(fh instanceof DocumentHandler)) {
			return Errno.EBADF;
		}

		try {
			((DocumentHandler) fh).release();
			return 0;

		} catch (IOException e) {
			log.error(e);
			return Errno.EIO;
		}
	}

	public int truncate(String path, long size) {
		Document document = getDocument(path);
		if (document == null) {
			return Errno.ENOENT;
		}

		try {
			DocumentHandler handler = DocumentHandler.open(document);
			handler.truncate(size);
			handler.release();
			return 0;

		} catch (IOException e) {
			return Errno.EIO;
		}
	}

	public int mknod(String path, int mode, int rdev) {
		try {
			docs.newDocument(path);
			return 0;

		} catch (MalformedURLException e) {
			e.printStackTrace();
			return Errno.EIO;

		} catch (IOException e) {
			e.printStackTrace();
			return Errno.EIO;

		} catch (ServiceException e) {
			e.printStackTrace();
			return Errno.ENETDOWN;

		} catch (Exception e) {
			e.printStackTrace();
			return Errno.EIO;
		}
	}

	public final int unlink(String path) {
		Document document = getDocument(path);
		if (document == null) {
			return Errno.ENOENT;
		}

		try {
			document.delete();
			return 0;

		} catch (IOException e) {
			return Errno.EIO;
		}
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

	public int link(String from, String to) {
		return Errno.EROFS;
	}

	public int symlink(String from, String to) {
		return Errno.EROFS;
	}

	public int readlink(String path, CharBuffer link) {
		return Errno.ENOENT;
	}


	// Read-only file system operations

	public int mkdir(String path, int mode) {
		return Errno.EROFS;
	}

	public int rename(String from, String to) {
		return Errno.EROFS;
	}

	public int rmdir(String path) {
		return Errno.EROFS;
	}


	// XattrSupport implementation

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

	public int listxattr(String path, XattrLister lister) {
		Document document = getDocument(path);

		if (document == null) {
			return Errno.ENOENT;
		}

		lister.add(ATTR_MIMETYPE);
		return 0;
	}

	public int removexattr(String path, String name) {
		return Errno.EROFS;
	}

	public int setxattr(String path, String name, ByteBuffer value, int flags) {
		return Errno.EROFS;
	}


	// the entry point

	public static void main(String[] args) {
		ResourceBundle properties = ResourceBundle.getBundle("gdocsfs");

		String username = properties.getString("username");
		String password = properties.getString("password");
		if (password == null || password.trim().isEmpty()) {
			char[] pass = System.console().readPassword("Google account password(%s): ", username);
			password = new String(pass);
		}

		String mountPoint = args[0];
		String[] fuseArgs = new String[] { "-f", "-s", "-ofsname=gdocsfs", "-ouse_ino", mountPoint };
		GoogleDocs googleDocs;
		try {
			googleDocs = new GoogleDocs(username, password);
			GoogleDocsFS gdocsfs = new GoogleDocsFS(googleDocs);
			FuseMount.mount(fuseArgs, gdocsfs, log);

		} catch (AuthenticationException e) {
			error(1, e, "Unable to connect. Ckeck your username and/or password.");

		} catch (ServiceException e) {
			error(2, e, "Ckeck your Internet connection: " + e.getMessage());

		} catch (IOException e) {
			error(3, e, "Ckeck your Internet connection: " + e.getMessage());

		} catch (Exception e) {
			error(4, e, "Unable to mount at " + mountPoint,
						"Error: " + e.getMessage());
		}
	}

	private static void error(int exitCode, Exception error, String... msgs) {
		for (String msg : msgs) {
			System.err.println(msg);
		}

		log.error(msgs, error);
		if (exitCode > 3) {
			System.err.println("Please, report this bug here: http://code.google.com/p/gdocsfs/issues/entry");
		}
		System.exit(exitCode);
	}

}