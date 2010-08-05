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
package org.teiid.cache.jboss;

import java.io.Serializable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.teiid.cache.Cache;
import org.teiid.cache.CacheConfiguration;
import org.teiid.cache.CacheFactory;
import org.teiid.cache.DefaultCacheFactory;
import org.teiid.cache.Cache.Type;
import org.teiid.core.TeiidRuntimeException;

public class ClusterableCacheFactory implements CacheFactory, Serializable {
	private static final long serialVersionUID = -1992994494154581234L;
	private CacheFactory delegate;
	private String cacheName;
	private boolean enabled = false;
	
	@Override
	public <K, V> Cache<K, V> get(Type type, CacheConfiguration config) {
		if (this.delegate == null) {
			Object cacheManager = getClusteredCache();
			if (cacheManager == null) {
				this.delegate = new DefaultCacheFactory();
			}
			else {
				try {
					this.delegate = new JBossCacheFactory(this.cacheName, cacheManager);
				} catch (Exception e) {
					throw new TeiidRuntimeException("Failed to obtain the clusted cache"); //$NON-NLS-1$
				}
			}
		}
		return delegate.get(type, config);
	}

	public void setClusteredCacheName(String name) {
		this.cacheName = name;
	}
	
	@Override
	public void destroy() {
		if (this.delegate != null) {
			this.delegate.destroy();
		}
	}
	
	private Object getClusteredCache() {
		if (this.enabled) {
			try {
				Context ctx = new InitialContext();
				return ctx.lookup("java:CacheManager"); //$NON-NLS-1$
			} catch (NamingException e) {
				return null;
			}
		}
		return null;
	}
	
	public void setEnabled(boolean value) {
		this.enabled = value;
	}
}
