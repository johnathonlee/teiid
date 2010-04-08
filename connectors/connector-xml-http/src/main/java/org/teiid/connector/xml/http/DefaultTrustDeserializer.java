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
 
package org.teiid.connector.xml.http;


import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.teiid.connector.api.ConnectorException;
import org.teiid.connector.xml.XMLConnectorState;
import org.teiid.connector.xml.base.TrustedPayloadBridge;

import com.metamatrix.core.MetaMatrixRuntimeException;

public class DefaultTrustDeserializer extends TrustedPayloadBridge implements HTTPTrustDeserializer {
	
	protected HTTPConnectorState m_state;
	public void setConnectorState(XMLConnectorState state) {
		m_state = (HTTPConnectorState) state;
	}

	public void modifyRequest(HttpClient client, HttpMethod method) throws ConnectorException {
		if(getUser() != null) {
			updateCredentials(client, method, getUser(), getPassword());
		}
	}

	protected void updateCredentials(HttpClient client, HttpMethod method, String user, String password) throws ConnectorException  {
    	if(m_state.useHttpBasicAuth()) {
			AuthScope authScope = new AuthScope(null, -1); // Create AuthScope for any host (null) and any port (-1).
    		Credentials defCred = new UsernamePasswordCredentials(user, password);
			client.getState().setCredentials(authScope, defCred);
			m_logger.logInfo(org.teiid.connector.xml.http.Messages.getString("DefaultTrustDeserializer.trust.processed"));
		} else {
			throw new ConnectorException(org.teiid.connector.xml.http.Messages.getString("HTTPExecutor.bad.security.configuration"));
		}
	}

	@Override
	public void processPayloads() throws Exception {
		// do nothing
		//throw new MetaMatrixRuntimeException("A custom trust payload processor needed based on the subject.");
	}
}
