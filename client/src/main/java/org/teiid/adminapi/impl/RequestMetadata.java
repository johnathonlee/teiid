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

package org.teiid.adminapi.impl;

import java.util.Date;

import org.jboss.managed.api.annotation.ManagementProperty;
import org.jboss.metatype.api.annotations.MetaMapping;
import org.teiid.adminapi.Request;

import com.metamatrix.core.util.HashCodeUtil;


@MetaMapping(RequestMetadataMapper.class)
public class RequestMetadata extends AdminObjectImpl implements Request {

	private static final long serialVersionUID = -2779106368517784259L;
	
	private long executionId;
	private long sessionId;
    private String command;
    private long createdTime;
    private long processTime;
    private boolean sourceRequest;
	private int nodeID = Integer.MIN_VALUE;
    private String transactionId;
    
    @Override
    @ManagementProperty(description="Unique Identifier for Request", readOnly=true)
    public long getExecutionId() {
		return executionId;
	}
    
    public void setExecutionId(long id) {
		this.executionId = id;
	}    
    
    @Override
    @ManagementProperty(description="Session ID", readOnly=true)
    public long getSessionId() {
        return this.sessionId;
    }
    
    public void setSessionId(long session) {
        this.sessionId = session;
    }
    
    @Override
    @ManagementProperty(description="Processing time for the request", readOnly=true)
    public long getProcessingTime() {
        return this.processTime;
    }
    
    public void setProcessingTime(long time) {
        this.processTime = time;
    }    

    @Override
    @ManagementProperty(description="Executing Command", readOnly=true)
    public String getCommand() {
        return this.command;
    }
    
    public void setCommand(String cmd) {
        this.command = cmd;
    }    
    
    @Override
    @ManagementProperty(description="Is this Connector level request", readOnly=true)
    public boolean sourceRequest() {
		return sourceRequest;
	}

	public void setSourceRequest(boolean sourceRequest) {
		this.sourceRequest = sourceRequest;
	}    
        
	@Override
	@ManagementProperty(description="Node Id", readOnly=true)
    public int getNodeId() {
        return this.nodeID;
    }
    
    public void setNodeId(int nodeID) {
        this.nodeID = nodeID;
    }
    
	@Override
	@ManagementProperty(description="Get Transaction XID if transaction involved", readOnly=true)
	public String getTransactionId() {
		return this.transactionId;
	}

	public void setTransactionId(String id) {
		this.transactionId = id;
	}
	
    @Override
	public boolean equals(Object obj) {
    	if (!(obj instanceof RequestMetadata)) {
    		return false;
    	}
    	RequestMetadata value = (RequestMetadata)obj;
    	if (!sourceRequest()) {
    		return sessionId == value.sessionId && executionId == value.executionId;
    	}
		return sessionId == value.sessionId && executionId == value.executionId && nodeID == value.nodeID;
	}
    
    public int hashCode() {
    	return HashCodeUtil.hashCode((int)executionId, (int)sessionId);
    }    
    
    public String toString() {
    	StringBuilder str = new StringBuilder();
    	str.append("Request: sessionid=").append(sessionId);
    	str.append("; executionId=").append(executionId);
    	if (nodeID != Integer.MIN_VALUE) {
    		str.append("; nodeId=").append(nodeID);
    	}
    	if (transactionId != null) {
    		str.append("; transactionId=").append(transactionId);
    	}
    	str.append("; sourceRequest=").append(sourceRequest);
    	str.append("; createdTime=").append(new Date(createdTime));
    	str.append("; processingTime=").append(new Date(processTime));
    	str.append("; command=").append(command); 
    	
    	return str.toString();
    }
}
