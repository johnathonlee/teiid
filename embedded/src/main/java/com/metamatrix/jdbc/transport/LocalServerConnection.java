/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package com.metamatrix.jdbc.transport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Properties;

import org.teiid.dqp.internal.process.DQPWorkContext;

import com.metamatrix.api.exception.MetaMatrixComponentException;
import com.metamatrix.client.ExceptionUtil;
import com.metamatrix.common.api.MMURL;
import com.metamatrix.common.comm.api.ServerConnection;
import com.metamatrix.common.comm.api.ServerConnectionListener;
import com.metamatrix.dqp.client.ClientSideDQP;
import com.metamatrix.jdbc.JDBCPlugin;
import com.metamatrix.platform.security.api.LogonResult;
import com.metamatrix.platform.security.api.MetaMatrixSessionID;
import com.metamatrix.platform.security.api.SessionToken;

public class LocalServerConnection implements ServerConnection {
	
	private final LogonResult result;
	private boolean shutdown;
	private DQPWorkContext workContext;
	private ClientSideDQP dqp;
	private ServerConnectionListener listener;
	private ClassLoader classLoader;

	public LocalServerConnection(MetaMatrixSessionID sessionId, Properties connectionProperties, ClientSideDQP dqp, ServerConnectionListener listener) {
		result = new LogonResult(new SessionToken(sessionId, connectionProperties.getProperty(MMURL.CONNECTION.USER_NAME)), connectionProperties, "local"); //$NON-NLS-1$
		
		//Initialize the workContext
		workContext = new DQPWorkContext();
		workContext.setSessionToken(result.getSessionToken());
		workContext.setVdbName(connectionProperties.getProperty(MMURL.JDBC.VDB_NAME));
		workContext.setVdbVersion(connectionProperties.getProperty(MMURL.JDBC.VDB_VERSION));
		DQPWorkContext.setWorkContext(workContext);
		
		this.dqp = dqp;
		this.listener = listener;
		
		if (this.listener != null) {
			this.listener.connectionAdded(this);
		}
		
		this.classLoader = Thread.currentThread().getContextClassLoader();
	}

	@SuppressWarnings("unchecked")
	public <T> T getService(Class<T> iface) {
		if (iface != ClientSideDQP.class) {
			throw new IllegalArgumentException("unknown service"); //$NON-NLS-1$
		}
		return (T) Proxy.newProxyInstance(this.classLoader, new Class[] {ClientSideDQP.class}, new InvocationHandler() {

			public Object invoke(Object arg0, Method arg1, Object[] arg2)
					throws Throwable {
				if (!isOpen()) {
					throw ExceptionUtil.convertException(arg1, new MetaMatrixComponentException(JDBCPlugin.Util.getString("LocalTransportHandler.session_inactive"))); //$NON-NLS-1$
				}
				ClassLoader current = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(classLoader);
				DQPWorkContext.setWorkContext(workContext);
				try {
					return arg1.invoke(dqp, arg2);
				} catch (InvocationTargetException e) {
					throw e.getTargetException();
				} finally {
					Thread.currentThread().setContextClassLoader(current);
				}
			}
		});
	}

	public boolean isOpen() {
		return !shutdown;
	}

	public void shutdown() {
		if (shutdown) {
			return;
		}
		if (this.listener != null) {
			this.listener.connectionRemoved(this);
		}
		this.shutdown = true;
	}

	public LogonResult getLogonResult() {
		return result;
	}
}
