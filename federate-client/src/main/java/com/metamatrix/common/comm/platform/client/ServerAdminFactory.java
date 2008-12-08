/*
 * JBoss, Home of Professional Open Source.
 * Copyright (C) 2008 Red Hat, Inc.
 * Copyright (C) 2000-2007 MetaMatrix, Inc.
 * Licensed to Red Hat, Inc. under one or more contributor 
 * license agreements.  See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package com.metamatrix.common.comm.platform.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Properties;

import com.metamatrix.admin.AdminPlugin;
import com.metamatrix.admin.api.exception.AdminComponentException;
import com.metamatrix.admin.api.exception.AdminException;
import com.metamatrix.admin.api.server.ServerAdmin;
import com.metamatrix.api.exception.security.LogonException;
import com.metamatrix.common.api.MMURL;
import com.metamatrix.common.comm.exception.CommunicationException;
import com.metamatrix.common.comm.exception.ConnectionException;
import com.metamatrix.common.comm.platform.CommPlatformPlugin;
import com.metamatrix.common.comm.platform.socket.client.SocketServerConnection;
import com.metamatrix.common.comm.platform.socket.client.SocketServerConnectionFactory;
import com.metamatrix.common.util.MetaMatrixProductNames;
import com.metamatrix.core.MetaMatrixRuntimeException;

/** 
 * Singleton factory for ServerAdmins.
 * @since 4.3
 */
public class ServerAdminFactory {
	
    private static final int BOUNCE_WAIT = 2000;
        
    private final static class ReconnectingProxy implements InvocationHandler {

    	private ServerAdmin target;
    	private SocketServerConnection registry;
    	private Properties p;
    	private boolean closed;
    	
    	public ReconnectingProxy(Properties p) {
    		this.p = p;
		}
    	
    	private synchronized ServerAdmin getTarget() throws AdminComponentException, CommunicationException {
    		if (closed) {
    			throw new AdminComponentException(CommPlatformPlugin.Util.getString("ERR.014.001.0001")); //$NON-NLS-1$
    		}
    		if (target != null && registry.isOpen()) {
    			return target;
    		}
    		try {
    			registry = SocketServerConnectionFactory.getInstance().createConnection(p);
    		} catch (ConnectionException e) {
    			throw new AdminComponentException(e.getMessage());
    		}
    		target = registry.getService(ServerAdmin.class);
    		return target;
    	}
    	
		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			if (method.getName().equals("close")) {
				close();
				return null;
			}
			Throwable t = null;
			for (int i = 0; i < 3; i++) {
				try {
					return method.invoke(getTarget(), args);
				} catch (InvocationTargetException e) {
					throw e.getTargetException();
				} catch (CommunicationException e) {
					t = e;
				}
			}
			if (method.getName().endsWith("bounceSystem")) {
				bounceSystem(((Boolean)args[1]).booleanValue());
				return null;
			}
			throw t;
		}
		
		public synchronized void close() {
			if (closed) {
				return;
			}
			this.closed = true;
			if (registry != null) {
				registry.shutdown();
			}
		}
		
		public void bounceSystem(boolean waitUntilDone) throws AdminException {
	        if (waitUntilDone) {
	        	//we'll wait 2 seconds for the server to go down
	        	try {
					Thread.sleep(BOUNCE_WAIT);
				} catch (InterruptedException e) {
					throw new MetaMatrixRuntimeException(e);
				}
				//we'll wait 30 seconds for the server to come back up
	        	for (int i = 0; i < 15; i++) {
	        		try {
	        			getTarget().getSystem();
	        		} catch (Exception e) {
	        			
	        		} finally {
	                    //reestablish a connection and retry
	                    try {
							Thread.sleep(BOUNCE_WAIT);
						} catch (InterruptedException e) {
							throw new MetaMatrixRuntimeException(e);
						}                                        
	                }
	        	}
	        }
		}
    }

	public static final String DEFAULT_APPLICATION_NAME = "Admin"; //$NON-NLS-1$

    /**Singleton instance*/
    private static ServerAdminFactory instance = new ServerAdminFactory();
    
    private ServerAdminFactory() {        
    }
    
    /**Get the singleton instance*/
    public static ServerAdminFactory getInstance() {
        return instance;
    }
    
    
    /**
     * Creates a ServerAdmin with the specified connection properties. 
     * Uses the DEFAULT_APPLICATION_NAME as the application name.
     * @param userName
     * @param password
     * @param serverURL
     * @return
     * @throws LogonException
     * @throws AdminException
     * @throws CommunicationException 
     * @throws LogonException 
     * @since 4.3
     */
    public ServerAdmin createAdmin(String userName,
                             char[] password,
                             String serverURL) throws AdminException {
        
        return createAdmin(userName, password, serverURL, DEFAULT_APPLICATION_NAME);
        
    }
    
    /**
     * Creates a ServerAdmin with the specified connection properties. 
     * @param userName
     * @param password
     * @param serverURL
     * @return
     * @throws LogonException
     * @throws AdminException
     * @throws CommunicationException 
     * @throws LogonException 
     * @since 4.3
     */
    public ServerAdmin createAdmin(String userName,
                                   char[] password,
                                   String serverURL,
                                   String applicationName) throws AdminException {
        
        if (userName == null || userName.trim().length() == 0) {
            throw new IllegalArgumentException(AdminPlugin.Util.getString("ERR.014.001.0099")); //$NON-NLS-1$
        }
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException(AdminPlugin.Util.getString("ERR.014.001.00100")); //$NON-NLS-1$
        }
        
    	final Properties p = new Properties();
    	p.setProperty(MMURL.CONNECTION.APP_NAME, applicationName);
    	p.setProperty(MMURL.CONNECTION.USER_NAME, userName);
    	p.setProperty(MMURL.CONNECTION.PASSWORD, new String(password));
    	p.setProperty(MMURL.CONNECTION.SERVER_URL, serverURL);
    	return createAdmin(p);
    }

	public ServerAdmin createAdmin(final Properties p)
			throws AdminComponentException, AdminException {
		p.setProperty(MMURL.CONNECTION.PRODUCT_NAME, MetaMatrixProductNames.Platform.PRODUCT_NAME);
    	
		ServerAdmin serverAdmin = (ServerAdmin)Proxy.newProxyInstance(Thread.currentThread()
				.getContextClassLoader(), new Class[] { ServerAdmin.class }, new ReconnectingProxy(p));
    	
        //make a method call, to test that we are connected 
    	serverAdmin.getSystem();
        
        return serverAdmin;
    }
    
}
