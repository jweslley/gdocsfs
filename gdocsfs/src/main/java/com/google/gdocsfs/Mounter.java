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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ResourceBundle;

import fuse.Filesystem3;
import fuse.FuseMount;
import fuse.XattrSupport;

/**
 * @author Jonhnny Weslley
 */
public class Mounter {

	public static void main(String[] args) {
		ResourceBundle properties = ResourceBundle.getBundle("gdocsfs");

		String username = properties.getString("username");
		String password = properties.getString("password");
		if (password == null || password.trim().isEmpty()) {
			char[] pass = System.console().readPassword("Google account password(%s): ", username);
			password = new String(pass);
		}

		String[] fuseArgs = new String[] { "-f", "-s", args[0] };
		try {
			GoogleDocsFS gdocsfs = new GoogleDocsFS(username, password);
			FuseMount.mount(fuseArgs, gdocsfs, gdocsfs.log);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static Filesystem3 intercept(final GoogleDocsFS gDocsFS) {
		return (Filesystem3) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), 
				new Class[] {Filesystem3.class, XattrSupport.class}, new InvocationHandler() {
	
					@Override
					public Object invoke(Object proxy, Method method,
							Object[] args) throws Throwable {
						System.out.println(">>" + method.getName());
						return method.invoke(gDocsFS, args);
					}
		});
	}
	
	
}

