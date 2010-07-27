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

package org.teiid.query.metadata;

import org.teiid.core.TeiidComponentException;
import org.teiid.query.optimizer.capabilities.BasicSourceCapabilities;
import org.teiid.query.optimizer.capabilities.CapabilitiesFinder;
import org.teiid.query.optimizer.capabilities.SourceCapabilities;
import org.teiid.query.optimizer.capabilities.SourceCapabilities.Capability;

public class TempCapabilitiesFinder implements CapabilitiesFinder {

	private static BasicSourceCapabilities tempCaps;
	private final CapabilitiesFinder delegate;
	
	public TempCapabilitiesFinder(CapabilitiesFinder delegate) {
		this.delegate = delegate;
	}

	@Override
	public SourceCapabilities findCapabilities(String modelName)
			throws TeiidComponentException {
		if (TempMetadataAdapter.TEMP_MODEL.getID().equals(modelName)) {
    		if (tempCaps == null) {
	    		tempCaps = new BasicSourceCapabilities();
	    		tempCaps.setCapabilitySupport(Capability.BULK_UPDATE, true);
	    		tempCaps.setCapabilitySupport(Capability.QUERY_ORDERBY, true);
    		}
    		return tempCaps;
    	}
		return delegate.findCapabilities(modelName);
	}

}
