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

package com.metamatrix.common.comm.platform.socket.client;

import java.lang.reflect.Method;

import junit.framework.TestCase;

import com.metamatrix.admin.api.exception.AdminException;
import com.metamatrix.admin.api.server.ServerAdmin;
import com.metamatrix.api.exception.MetaMatrixComponentException;
import com.metamatrix.client.ExceptionUtil;
import com.metamatrix.common.xa.XATransactionException;
import com.metamatrix.core.MetaMatrixRuntimeException;
import com.metamatrix.dqp.client.ClientSideDQP;

public class TestSocketServiceRegistry extends TestCase {

	public void testExceptionConversionNoException() throws Exception {
		
		Method m = ServerAdmin.class.getMethod("close", new Class[] {});
		
		Throwable t = ExceptionUtil.convertException(m, new MetaMatrixComponentException());
		
		assertTrue(t instanceof MetaMatrixRuntimeException);
	}
	
	public void testAdminExceptionConversion() throws Exception {
		
		Method m = ServerAdmin.class.getMethod("getHosts", new Class[] {String.class});
		
		Throwable t = ExceptionUtil.convertException(m, new MetaMatrixComponentException());
		
		assertTrue(t instanceof AdminException);
	}
	
	public void testComponentExceptionConversion() throws Exception {
		
		Method m = ClientSideDQP.class.getMethod("getMetadata", new Class[] {Long.TYPE});
		
		Throwable t = ExceptionUtil.convertException(m, new NullPointerException());
		
		assertTrue(t instanceof MetaMatrixComponentException);
	}
	
	public void testXATransactionExceptionConversion() throws Exception {
		
		Method m = ClientSideDQP.class.getMethod("recover", new Class[] {Integer.TYPE});
		
		Throwable t = ExceptionUtil.convertException(m, new MetaMatrixComponentException());
		
		assertTrue(t instanceof XATransactionException);
	}
	
}
