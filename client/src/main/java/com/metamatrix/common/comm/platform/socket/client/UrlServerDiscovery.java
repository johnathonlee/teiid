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

package com.metamatrix.common.comm.platform.socket.client;

import java.util.List;
import java.util.Properties;

import com.metamatrix.common.api.HostInfo;
import com.metamatrix.common.api.MMURL;
import com.metamatrix.platform.security.api.LogonResult;

/**
 * Simple URL discovery strategy
 */
public class UrlServerDiscovery implements ServerDiscovery {

	private MMURL url;
	
	public UrlServerDiscovery() {
	}
	
	public UrlServerDiscovery(MMURL url) {
		this.url = url;
	}
	
	@Override
	public List<HostInfo> getKnownHosts(LogonResult result,
			SocketServerInstance instance) {
		return url.getHostInfo();
	}

	@Override
	public void init(MMURL url, Properties p) {
		this.url = url;
	}
	
	@Override
	public void connectionSuccessful(HostInfo info) {
		
	}

	@Override
	public void markInstanceAsBad(HostInfo info) {
		
	}
		
	@Override
	public void shutdown() {
		
	}
	
}
