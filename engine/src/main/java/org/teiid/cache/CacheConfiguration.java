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

package org.teiid.cache;

import org.jboss.managed.api.annotation.ManagementComponent;
import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementObjectID;
import org.jboss.managed.api.annotation.ManagementProperties;
import org.jboss.managed.api.annotation.ManagementProperty;

@ManagementObject(componentType=@ManagementComponent(type="teiid",subtype="dqp"), properties=ManagementProperties.EXPLICIT)
public class CacheConfiguration {
	
	public static CacheConfiguration DEFAULT = new CacheConfiguration(Policy.LRU, 60*60, 100); // 1 hours with 100 nodes.
		
	public enum Policy {
		LRU,  // Least Recently Used
		EXPIRATION
	}
	
	private Policy policy;
	private int maxage;
	private int maxEntries;
	private boolean enabled = true;
	private String name;
	
	public CacheConfiguration() {
	}
	
	public CacheConfiguration(Policy policy, int maxAgeInSeconds, int maxNodes) {
		this.policy = policy;
		this.maxage = maxAgeInSeconds;
		this.maxEntries = maxNodes;
	}
	
	public Policy getPolicy() {
		return this.policy;
	}

	@ManagementProperty(description="The maximum age of a result set cache entry in seconds. -1 indicates no max. (default 7200)")
	public int getMaxAgeInSeconds(){
		return maxage;
	}

	public void setMaxAgeInSeconds(int maxage){
		this.maxage = maxage;
	}
	
	@ManagementProperty(description="The maximum number of result set cache entries. -1 indicates no limit. (default 1024)")
	public int getMaxEntries() {
		return this.maxEntries;
	}

	public void setMaxEntries(int entries) {
		this.maxEntries = entries;
	}

	public void setType (String type) {
		this.policy = Policy.valueOf(type);
	}
	
	@ManagementProperty(description="Name of the configuration", readOnly=true)
	@ManagementObjectID(type="cache")	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + maxage;
		result = prime * result + maxEntries;
		result = prime * result + ((policy == null) ? 0 : policy.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CacheConfiguration other = (CacheConfiguration) obj;
		if (maxage != other.maxage)
			return false;
		if (maxEntries != other.maxEntries)
			return false;
		if (policy == null) {
			if (other.policy != null)
				return false;
		} else if (!policy.equals(other.policy))
			return false;
		return true;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
