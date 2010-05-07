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

/*
 * Date: Aug 25, 2003
 * Time: 3:53:37 PM
 */
package org.teiid.dqp.internal.datamgr.impl;

import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.teiid.connector.metadata.runtime.Datatype;
import org.teiid.connector.metadata.runtime.MetadataFactory;
import org.teiid.connector.metadata.runtime.MetadataStore;
import org.teiid.dqp.internal.cache.DQPContextCache;
import org.teiid.dqp.internal.datamgr.impl.ConnectorWorkItem.PermitMode;
import org.teiid.dqp.internal.process.AbstractWorkItem;
import org.teiid.logging.CommandLogMessage;
import org.teiid.logging.LogConstants;
import org.teiid.logging.LogManager;
import org.teiid.logging.MessageLevel;
import org.teiid.logging.CommandLogMessage.Event;
import org.teiid.resource.ConnectorException;
import org.teiid.resource.cci.ConnectorCapabilities;
import org.teiid.resource.cci.ExecutionContext;
import org.teiid.resource.cci.ExecutionFactory;
import org.teiid.resource.cci.MetadataProvider;

import com.metamatrix.common.buffer.BlockedException;
import com.metamatrix.core.util.Assertion;
import com.metamatrix.dqp.DQPPlugin;
import com.metamatrix.dqp.message.AtomicRequestID;
import com.metamatrix.dqp.message.AtomicRequestMessage;
import com.metamatrix.dqp.service.BufferService;
import com.metamatrix.query.optimizer.capabilities.BasicSourceCapabilities;
import com.metamatrix.query.optimizer.capabilities.SourceCapabilities;
import com.metamatrix.query.optimizer.capabilities.SourceCapabilities.Scope;
import com.metamatrix.query.sql.lang.Command;

/**
 * The <code>ConnectorManager</code> manages a {@link org.teiid.resource.adapter.BasicExecutionFactory Connector}
 * and its associated workers' state.
 */
public class ConnectorManager  {
	
	public static final int DEFAULT_MAX_THREADS = 20;
	private String translatorName;
	private String connectionName;
	
    //services acquired in start
    private BufferService bufferService;
    
    // known requests
    private ConcurrentHashMap<AtomicRequestID, ConnectorWorkItem> requestStates = new ConcurrentHashMap<AtomicRequestID, ConnectorWorkItem>();
	
	private SourceCapabilities cachedCapabilities;
	
	private int currentConnections;
	private int maxConnections = DEFAULT_MAX_THREADS;
	private LinkedList<ConnectorWorkItem> queuedRequests = new LinkedList<ConnectorWorkItem>();
	
	private volatile boolean stopped;

	
    public ConnectorManager(String translatorName, String connectionName) {
    	this.translatorName = translatorName;
    	this.connectionName = connectionName;
    }
    
    public String getStausMessage() {
    	StringBuilder sb = new StringBuilder();
    	ExecutionFactory ef = getExecutionFactory();
    	if(ef != null) {
    		if (ef.isSourceRequired()) {
    			Object conn = getConnectionFactory();
    			if (conn == null) {
    				sb.append(DQPPlugin.Util.getString("datasource_not_found", this.connectionName)); //$NON-NLS-1$
    			}
    		}
    	}
    	else {
    		sb.append(DQPPlugin.Util.getString("translator_not_found", this.translatorName)); //$NON-NLS-1$
    	}
    	return sb.toString();
    }
    
    public synchronized void acquireConnectionLock(ConnectorWorkItem item) throws BlockedException {
    	switch (item.getPermitMode()) {
    	case NOT_ACQUIRED: 
    		if (currentConnections < maxConnections) {
	    		currentConnections++;
	    		item.setPermitMode(PermitMode.ACQUIRED);
	    		return;
	    	}
	    	queuedRequests.add(item);
	    	item.setPermitMode(PermitMode.BLOCKED);	
    	case BLOCKED:
    		throw BlockedException.INSTANCE;
    	}
    }
        
    
    public MetadataStore getMetadata(String modelName, Map<String, Datatype> datatypes, Properties importProperties) throws ConnectorException {
    	MetadataFactory factory = new MetadataFactory(modelName, datatypes, importProperties);
    	ExecutionFactory executionFactory = getExecutionFactory();
    	if (executionFactory instanceof MetadataProvider) {
    		((MetadataProvider)executionFactory).getConnectorMetadata(factory, getConnectionFactory());
    	}
    	return factory.getMetadataStore();
	}    
    
    public SourceCapabilities getCapabilities() throws ConnectorException {
    	if (cachedCapabilities != null) {
    		return cachedCapabilities;
    	}

    	checkStatus();
    	ExecutionFactory connector = getExecutionFactory();
    	ConnectorCapabilities caps = connector.getCapabilities();
        BasicSourceCapabilities resultCaps = CapabilitiesConverter.convertCapabilities(caps, this.translatorName, connector.isXaCapable());
    	resultCaps.setScope(Scope.SCOPE_GLOBAL);
    	cachedCapabilities = resultCaps;
        return resultCaps;
    }
    
    public ConnectorWork executeRequest(AtomicRequestMessage message, AbstractWorkItem awi) throws ConnectorException {
        // Set the connector ID to be used; if not already set. 
    	checkStatus();
    	AtomicRequestID atomicRequestId = message.getAtomicRequestID();
    	LogManager.logDetail(LogConstants.CTX_CONNECTOR, new Object[] {atomicRequestId, "Create State"}); //$NON-NLS-1$

    	ConnectorWorkItem item = new ConnectorWorkItem(message, awi, this);
        Assertion.isNull(requestStates.put(atomicRequestId, item), "State already existed"); //$NON-NLS-1$
        return item;
    }
    
    ConnectorWork getState(AtomicRequestID requestId) {
        return requestStates.get(requestId);
    }
    
    /**
     * Remove the state associated with
     * the given <code>RequestID</code>.
     */
    void removeState(AtomicRequestID id) {
    	LogManager.logDetail(LogConstants.CTX_CONNECTOR, new Object[] {id, "Remove State"}); //$NON-NLS-1$
        ConnectorWorkItem cwi = requestStates.remove(id);
        if (cwi != null && cwi.getPermitMode() == PermitMode.ACQUIRED) {
        	synchronized (this) {
	        	ConnectorWorkItem next = queuedRequests.pollFirst();
	        	if (next == null) {
	        		currentConnections--;
	        		return;
	        	}
	        	next.setPermitMode(PermitMode.ACQUIRED);
	        	next.getParent().moreWork();
        	}
        }
    }

    int size() {
        return requestStates.size();
    }
    
    public void setBufferService(BufferService service) {
    	this.bufferService = service;
    }
    
    /**
     * initialize this <code>ConnectorManager</code>.
     */
    public void start() {
        LogManager.logDetail(LogConstants.CTX_CONNECTOR, DQPPlugin.Util.getString("ConnectorManagerImpl.Initializing_connector", translatorName)); //$NON-NLS-1$
    }
    
    /**
     * Stop this connector.
     */
    public void stop() {    
    	stopped = true;
        //ensure that all requests receive a response
        for (ConnectorWork workItem : this.requestStates.values()) {
    		workItem.cancel();
		}
    }

    /**
     * Add begin point to transaction monitoring table.
     * @param qr Request that contains the MetaMatrix command information in the transaction.
     */
    void logSRCCommand(AtomicRequestMessage qr, ExecutionContext context, Event cmdStatus, Integer finalRowCnt) {
    	if (!LogManager.isMessageToBeRecorded(LogConstants.CTX_COMMANDLOGGING, MessageLevel.DETAIL)) {
    		return;
    	}
        String sqlStr = null;
        if(cmdStatus == Event.NEW){
        	Command cmd = qr.getCommand();
            sqlStr = cmd != null ? cmd.toString() : null;
        }
        String userName = qr.getWorkContext().getUserName();
        String transactionID = null;
        if ( qr.isTransactional() ) {
            transactionID = qr.getTransactionContext().getTransactionId();
        }
        
        String modelName = qr.getModelName();
        AtomicRequestID id = qr.getAtomicRequestID();
        
        String principal = userName == null ? "unknown" : userName; //$NON-NLS-1$
        
        CommandLogMessage message = null;
        if (cmdStatus == Event.NEW) {
            message = new CommandLogMessage(System.currentTimeMillis(), qr.getRequestID().toString(), id.getNodeID(), transactionID, modelName, translatorName, qr.getWorkContext().getSessionId(), principal, sqlStr, context);
        } 
        else {
            message = new CommandLogMessage(System.currentTimeMillis(), qr.getRequestID().toString(), id.getNodeID(), transactionID, modelName, translatorName, qr.getWorkContext().getSessionId(), principal, finalRowCnt, cmdStatus, context);
        }         
        LogManager.log(MessageLevel.DETAIL, LogConstants.CTX_COMMANDLOGGING, message);
    }
    
    /**
     * Get the <code>Translator</code> object managed by this  manager.
     * @return the <code>ExecutionFactory</code>.
     */
    protected ExecutionFactory getExecutionFactory() {
    	try {
			InitialContext ic = new InitialContext();
			return (ExecutionFactory)ic.lookup(this.translatorName);
		} catch (NamingException e) {
		}  
		return null;
    }
    
    /**
     * Get the ConnectionFactory object required by this manager
     * @return
     */
    protected Object getConnectionFactory() {
    	if (this.connectionName != null) {
	    	try {
				InitialContext ic = new InitialContext();
				return ic.lookup(this.connectionName);
			} catch (NamingException e) {
			}    		
    	}
    	return null;
    }
    
    
    DQPContextCache getContextCache() {
     	if (bufferService != null) {
    		return bufferService.getContextCache();
    	}
    	return null;
    }
    
    private void checkStatus() throws ConnectorException {
    	if (stopped) {
    		throw new ConnectorException(DQPPlugin.Util.getString("ConnectorManager.not_in_valid_state", this.translatorName)); //$NON-NLS-1$
    	}
    }
    
    public void setMaxConnections(int value) {
    	this.maxConnections = value;
    }
    
    public String getTranslatorName() {
    	return this.translatorName;
    }
    
    public String getConnectionName() {
    	return this.connectionName;
    }
}
