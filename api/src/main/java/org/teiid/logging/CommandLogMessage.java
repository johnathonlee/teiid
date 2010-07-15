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

package org.teiid.logging;

import java.sql.Timestamp;

import org.teiid.translator.ExecutionContext;

/**
 * Log Message for source and user command events.
 */
public class CommandLogMessage {
    
	public enum Event {
		NEW,
		END,
		CANCEL,
		ERROR
	}
	
    private boolean source;
    private Event event;
    private long timestamp;
    
    // Transaction info
    private String transactionID;
    
    // Session info
    private String sessionID;
    private String applicationName;
    private String principal;
    private String vdbName;
    private int vdbVersion;
    
    // RequestInfo
    private String requestID;
    private Long sourceCommandID;
    private String sql;
    private Integer rowCount;
    private String modelName;
    private String connectorBindingName;
    private ExecutionContext executionContext;
        
    public CommandLogMessage(long timestamp,
                                String requestID,
                                String transactionID,
                                String sessionID,
                                String applicationName,
                                String principal,
                                String vdbName,
                                int vdbVersion,
                                String sql) {
        // userCommandStart
    	this(timestamp, requestID, transactionID, sessionID, principal, vdbName, vdbVersion, null, Event.NEW);
        this.applicationName = applicationName;
        this.sql = sql;
    }
    public CommandLogMessage(long timestamp,
                                String requestID,
                                String transactionID,
                                String sessionID,
                                String principal,
                                String vdbName,
                                int vdbVersion, 
                                Integer finalRowCount,
                                Event event) {
        // userCommandEnd
        this.event = event;
        this.timestamp = timestamp;
        this.requestID = requestID;
        this.transactionID = transactionID;
        this.sessionID = sessionID;
        this.principal = principal;
        this.vdbName = vdbName;
        this.vdbVersion = vdbVersion;
        this.rowCount = finalRowCount;
    }
    public CommandLogMessage(long timestamp,
                                String requestID,
                                long sourceCommandID,
                                String transactionID,
                                String modelName, 
                                String connectorBindingName,
                                String sessionID,
                                String principal,
                                String sql,
                                ExecutionContext context) {
        // dataSourceCommandStart
    	this(timestamp, requestID, sourceCommandID, transactionID, modelName, connectorBindingName, sessionID, principal, null, Event.NEW, context);
        this.sql = sql;
    }
    public CommandLogMessage(long timestamp,
                                String requestID,
                                long sourceCommandID,
                                String transactionID,
                                String modelName, 
                                String connectorBindingName,
                                String sessionID,
                                String principal,
                                Integer finalRowCount,
                                Event event,
                                ExecutionContext context) {
        // dataSourceCommandEnd
    	this.source = true;
        this.event = event;
        this.timestamp = timestamp;
        this.requestID = requestID;
        this.sourceCommandID = sourceCommandID;
        this.transactionID = transactionID;
        this.modelName = modelName;
        this.connectorBindingName = connectorBindingName;
        this.sessionID = sessionID;
        this.principal = principal;
        this.rowCount = finalRowCount;
        this.executionContext = context;
    }
    
    public String toString() {
    	if (!source && event == Event.NEW) {
    		return "\tSTART USER COMMAND:\tstartTime=" + new Timestamp(timestamp) + "\trequestID=" + requestID + "\ttxID=" + transactionID + "\tsessionID=" + sessionID + "\tapplicationName=" + applicationName + "\tprincipal=" + principal + "\tvdbName=" + vdbName + "\tvdbVersion=" + vdbVersion + "\tsql=" + sql;  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$//$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
    	}
    	if (!source) {
    		return "\t"+ event +" USER COMMAND:\tendTime=" + new Timestamp(timestamp) + "\trequestID=" + requestID + "\ttxID=" + transactionID + "\tsessionID=" + sessionID + "\tprincipal=" + principal + "\tvdbName=" + vdbName + "\tvdbVersion=" + vdbVersion + "\tfinalRowCount=" + rowCount;  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$//$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
    	}
    	if (event == Event.NEW) {
    		return "\tSTART DATA SRC COMMAND:\tstartTime=" + new Timestamp(timestamp) + "\trequestID=" + requestID + "\tsourceCommandID="+ sourceCommandID + "\ttxID=" + transactionID + "\tmodelName="+ modelName + "\tconnectorBindingName=" + connectorBindingName + "\tsessionID=" + sessionID + "\tprincipal=" + principal + "\tsql=" + sql;  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$//$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
    	}
		return "\t"+ event +" SRC COMMAND:\tendTime=" + new Timestamp(timestamp) + "\trequestID=" + requestID + "\tsourceCommandID="+ sourceCommandID + "\ttxID=" + transactionID + "\tmodelName="+ modelName + "\tconnectorBindingName=" + connectorBindingName + "\tsessionID=" + sessionID + "\tprincipal=" + principal + "\tfinalRowCount=" + rowCount;  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$//$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$
  	}

	public long getTimestamp() {
		return timestamp;
	}
	public String getTransactionID() {
		return transactionID;
	}
	public String getSessionID() {
		return sessionID;
	}
	public String getApplicationName() {
		return applicationName;
	}
	public String getPrincipal() {
		return principal;
	}
	public String getVdbName() {
		return vdbName;
	}
	public int getVdbVersion() {
		return vdbVersion;
	}
	public String getRequestID() {
		return requestID;
	}
	public Long getSourceCommandID() {
		return sourceCommandID;
	}
	/**
	 * Returns the command.  Only valid for {@link Event#NEW}
	 * @return
	 */
	public String getSql() {
		return sql;
	}
	/**
	 * Returns the command.  Only valid for {@link Event#END}
	 * @return
	 */
	public Integer getRowCount() {
		return rowCount;
	}
	public String getModelName() {
		return modelName;
	}
	public String getConnectorBindingName() {
		return connectorBindingName;
	}
	public Event getStatus() {
		return event;
	}
	public boolean isSource() {
		return source;
	}
	/**
	 * Only available for source commands
	 * @return
	 */
	public ExecutionContext getExecutionContext() {
		return executionContext;
	}    
}
