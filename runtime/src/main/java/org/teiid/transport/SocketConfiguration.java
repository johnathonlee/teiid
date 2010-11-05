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
package org.teiid.transport;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.jboss.managed.api.annotation.ManagementComponent;
import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementObjectID;
import org.jboss.managed.api.annotation.ManagementProperties;
import org.jboss.managed.api.annotation.ManagementProperty;
import org.teiid.core.TeiidRuntimeException;


@ManagementObject(componentType=@ManagementComponent(type="teiid",subtype="dqp"), properties=ManagementProperties.EXPLICIT)
public class SocketConfiguration {
	
	private int outputBufferSize;
	private int inputBufferSize;
	private int maxSocketThreads;
	private int portNumber;
	private InetAddress hostAddress;
	private SSLConfiguration sslConfiguration;
	private boolean enabled;
	private String hostName;
	private String name;
	
	@ManagementProperty(description="Name of the configuration", readOnly=true)
	@ManagementObjectID(type="socket")
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setBindAddress(String addr) {
		this.hostName = addr;
	}
	
	public void setPortNumber(int port) {
		this.portNumber = port;
	}
	
	public void setMaxSocketThreads(int value) {
		this.maxSocketThreads = value;
	}
	
	public void setInputBufferSize(int value) {
		this.inputBufferSize = value;
	}
	
	public void setOutputBufferSize(int value) {
		this.outputBufferSize = value;
	}
	
	public void setSSLConfiguration(SSLConfiguration value) {
		this.sslConfiguration = value;
	}	
 	
	private void resolveHostName() {
		try {
			// if not defined then see if can bind to local address; if supplied resolve it by name
			if (this.hostName == null) {
				this.hostName = InetAddress.getLocalHost().getHostName();
			}
		} catch (UnknownHostException e) {
			throw new TeiidRuntimeException("Failed to resolve the bind address"); //$NON-NLS-1$
		}
	}

 	@ManagementProperty(description="enabled")
	public boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@ManagementProperty(description="SO_SNDBUF size, 0 indicates that system default should be used (default 0)")
	public int getOutputBufferSize() {
		return outputBufferSize;
	}

	@ManagementProperty(description="SO_RCVBUF size, 0 indicates that system default should be used (default 0)")
	public int getInputBufferSize() {
		return inputBufferSize;
	}

	@ManagementProperty(description="Max NIO threads")
	public int getMaxSocketThreads() {
		return maxSocketThreads;
	}

	@ManagementProperty(description="Port Number")
	public int getPortNumber() {
		return portNumber;
	}

	public InetAddress getHostAddress() {
		resolveHostName();
		
		if (this.hostAddress != null) {
			return hostAddress;
		}
    	try {
    		//only cache inetaddresses if they represent the ip. 
			InetAddress addr = InetAddress.getByName(this.hostName);
			if (addr.getHostAddress().equalsIgnoreCase(this.hostName)) {
				this.hostAddress = addr;
			}
			return addr;
		} catch (UnknownHostException e) {
			throw new TeiidRuntimeException("Failed to resolve the bind address"); //$NON-NLS-1$
		}		
	}
	
	@ManagementProperty(description="Host Name")
	public String getHostName() {
		resolveHostName();
		return this.hostName;
	}

	public SSLConfiguration getSSLConfiguration() {
		return sslConfiguration;
	}
	
	@ManagementProperty(description="SSL enabled")
	public boolean getSslEnabled() {
		return this.sslConfiguration != null && this.sslConfiguration.isSslEnabled();
	}
}
