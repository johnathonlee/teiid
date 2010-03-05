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

package com.metamatrix.connector.loopback;

import org.teiid.connector.api.Connection;
import org.teiid.connector.api.ConnectorCapabilities;
import org.teiid.connector.api.ConnectorEnvironment;
import org.teiid.connector.api.ConnectorException;
import org.teiid.connector.api.ConnectorLogger;
import org.teiid.connector.basic.BasicConnector;

/**
 * Loopback connector.
 */
public class LoopbackConnector extends BasicConnector {

    private LoopbackManagedConnectionFactory config;

	@Override
	public void initialize(ConnectorEnvironment env) throws ConnectorException {
		super.initialize(env);

		this.config = (LoopbackManagedConnectionFactory)env;
		
		ConnectorLogger logger = config.getLogger();
		logger.logInfo("Loaded for LoopbackConnector"); //$NON-NLS-1$
	}
    

    public Connection getConnection() throws ConnectorException {
        return new LoopbackConnection(this.config);
    }

    @Override
    public Class<? extends ConnectorCapabilities> getDefaultCapabilities() {
    	return LoopbackCapabilities.class;
    }
}
