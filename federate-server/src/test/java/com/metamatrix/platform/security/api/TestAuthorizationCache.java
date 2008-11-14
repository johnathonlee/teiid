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

package com.metamatrix.platform.security.api;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.jboss.cache.Cache;
import org.jboss.cache.CacheFactory;
import org.jboss.cache.DefaultCacheFactory;

import com.metamatrix.cache.FakeCache;
import com.metamatrix.platform.security.authorization.cache.AuthorizationCache;

public class TestAuthorizationCache extends TestCase {

	Cache cacheStore;
	
	@Override
	protected void setUp() throws Exception {
		CacheFactory factory = new DefaultCacheFactory();
		cacheStore = factory.createCache();
	}

	@Override
	protected void tearDown() throws Exception {
		cacheStore.stop();
	}
	
    public void testFindPolicyIdsWithNoResult() throws Exception {
        AuthorizationCache cache = new AuthorizationCache(new FakeCache(), new FakeCache(), null); 
        Collection result = cache.findPolicyIDs(new MetaMatrixPrincipalName("a", MetaMatrixPrincipal.TYPE_USER), null); //$NON-NLS-1$
        assertTrue(result.isEmpty());
    }
    
    public void testFindPolicyIds() throws Exception {
        AuthorizationCache cache = new AuthorizationCache(new FakeCache(), new FakeCache(),null);
        List policyIDs = new LinkedList();
        policyIDs.add(new Integer(1));
        SessionToken token =  new SessionToken(new MetaMatrixSessionID(1), "none", "dummy", new Properties()); //$NON-NLS-1$ //$NON-NLS-2$
        cache.cachePolicyIDsForPrincipal(new MetaMatrixPrincipalName("a", MetaMatrixPrincipal.TYPE_USER), token, policyIDs); //$NON-NLS-1$
        Collection result = cache.findPolicyIDs(new MetaMatrixPrincipalName("a", MetaMatrixPrincipal.TYPE_USER), new SessionToken(new MetaMatrixSessionID(2), "none", "dummy", new Properties())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        //different session, result should be empty
        assertTrue(result.isEmpty());
        result = cache.findPolicyIDs(new MetaMatrixPrincipalName("a", MetaMatrixPrincipal.TYPE_USER), token); //$NON-NLS-1$
        assertEquals(result.iterator().next(), new Integer(1));
    }
    
}
