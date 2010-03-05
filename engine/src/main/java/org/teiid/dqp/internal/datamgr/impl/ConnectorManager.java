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

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;

import org.jboss.managed.api.annotation.ManagementComponent;
import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementProperties;
import org.jboss.managed.api.annotation.ManagementProperty;
import org.jboss.managed.api.annotation.ViewUse;
import org.teiid.SecurityHelper;
import org.teiid.adminapi.impl.WorkerPoolStatisticsMetadata;
import org.teiid.connector.api.Connection;
import org.teiid.connector.api.Connector;
import org.teiid.connector.api.ConnectorCapabilities;
import org.teiid.connector.api.ConnectorEnvironment;
import org.teiid.connector.api.ConnectorException;
import org.teiid.connector.api.ExecutionContext;
import org.teiid.connector.basic.WrappedConnection;
import org.teiid.connector.metadata.runtime.Datatype;
import org.teiid.connector.metadata.runtime.MetadataFactory;
import org.teiid.connector.metadata.runtime.MetadataStore;
import org.teiid.dqp.internal.cache.DQPContextCache;
import org.teiid.dqp.internal.datamgr.CapabilitiesConverter;

import com.metamatrix.common.comm.api.ResultsReceiver;
import com.metamatrix.common.log.LogManager;
import com.metamatrix.common.queue.StatsCapturingWorkManager;
import com.metamatrix.core.log.MessageLevel;
import com.metamatrix.core.util.Assertion;
import com.metamatrix.dqp.DQPPlugin;
import com.metamatrix.dqp.message.AtomicRequestID;
import com.metamatrix.dqp.message.AtomicRequestMessage;
import com.metamatrix.dqp.message.AtomicResultsMessage;
import com.metamatrix.dqp.service.BufferService;
import com.metamatrix.dqp.service.CommandLogMessage;
import com.metamatrix.dqp.service.ConnectorStatus;
import com.metamatrix.dqp.util.LogConstants;
import com.metamatrix.query.optimizer.capabilities.BasicSourceCapabilities;
import com.metamatrix.query.optimizer.capabilities.SourceCapabilities;
import com.metamatrix.query.optimizer.capabilities.SourceCapabilities.Scope;
import com.metamatrix.query.sql.lang.Command;

/**
 * The <code>ConnectorManager</code> manages a {@link org.teiid.connector.basic.BasicConnector Connector}
 * and its associated workers' state.
 */
@ManagementObject(isRuntime=true, componentType=@ManagementComponent(type="teiid",subtype="connectormanager"), properties=ManagementProperties.EXPLICIT)
public class ConnectorManager  {
	
	public static final int DEFAULT_MAX_THREADS = 20;
	private String connectorName;
	    
    private StatsCapturingWorkManager workManager;
    private SecurityHelper securityHelper;
    
    protected ConnectorWorkItemFactory workItemFactory;
    
    private volatile ConnectorStatus state = ConnectorStatus.NOT_INITIALIZED;

    //services acquired in start
    private BufferService bufferService;
    
    // known requests
    private ConcurrentHashMap<AtomicRequestID, ConnectorWorkItem> requestStates = new ConcurrentHashMap<AtomicRequestID, ConnectorWorkItem>();
	
	private SourceCapabilities cachedCapabilities;

    public ConnectorManager(String name) {
    	this(name, DEFAULT_MAX_THREADS, null);
    }
	
    public ConnectorManager(String name, int maxThreads, SecurityHelper securityHelper) {
    	if (name == null) {
    		throw new IllegalArgumentException("Connector name can not be null");
    	}
    	if (maxThreads <= 0) {
    		maxThreads = DEFAULT_MAX_THREADS;
    	}
    	this.connectorName = name;
    	this.workManager = new StatsCapturingWorkManager(this.connectorName, maxThreads);
    	this.securityHelper = securityHelper;
    }
    
    SecurityHelper getSecurityHelper() {
		return securityHelper;
	}
    
    public String getName() {
        return this.connectorName;
    }	
	    
    public MetadataStore getMetadata(String modelName, Map<String, Datatype> datatypes, Properties importProperties) throws ConnectorException {
    	
    	MetadataFactory factory = new MetadataFactory(modelName, datatypes, importProperties);
		
		WrappedConnection conn = null;
    	try {
    		checkStatus();
	    	conn = (WrappedConnection)getConnector().getConnection();
	    	conn.getConnectorMetadata(factory);
    	} finally {
    		if (conn != null) {
    			conn.close();
    		}
    	}		
    	return factory.getMetadataStore();
	}    
    
    
    public SourceCapabilities getCapabilities() throws ConnectorException {
    	if (cachedCapabilities != null) {
    		return cachedCapabilities;
    	}
        Connection conn = null;
        try {
        	checkStatus();
        	Connector connector = getConnector();
        	ConnectorCapabilities caps = connector.getCapabilities();
            boolean global = true;
            if (caps == null) {
            	conn = connector.getConnection();
            	caps = conn.getCapabilities();
            	global = false;
            }
            
            BasicSourceCapabilities resultCaps = CapabilitiesConverter.convertCapabilities(caps, getName(), connector.getConnectorEnvironment().isXaCapable());
            if (global) {
            	resultCaps.setScope(Scope.SCOPE_GLOBAL);
            	cachedCapabilities = resultCaps;
            } else {
            	resultCaps.setScope(Scope.SCOPE_PER_USER);
            }
            return resultCaps;
        } finally {
        	if ( conn != null ) {
                conn.close();
            }
        }
    }
    
    public void executeRequest(WorkManager workManager, ResultsReceiver<AtomicResultsMessage> receiver, AtomicRequestMessage message) throws ConnectorException {
        // Set the connector ID to be used; if not already set. 
    	checkStatus();
    	AtomicRequestID atomicRequestId = message.getAtomicRequestID();
    	LogManager.logDetail(LogConstants.CTX_CONNECTOR, new Object[] {atomicRequestId, "Create State"}); //$NON-NLS-1$

    	ConnectorWorkItem item = workItemFactory.createWorkItem(message, receiver, workManager);
    	
        Assertion.isNull(requestStates.put(atomicRequestId, item), "State already existed"); //$NON-NLS-1$
		enqueueRequest(workManager, item);
    }
    
    private void enqueueRequest(WorkManager workManager, ConnectorWorkItem work) throws ConnectorException {
        try {
        	// if connector is immutable, then we do not want pass-on the transaction context.
        	if (work.securityContext.isTransactional()) {
        		this.workManager.scheduleWork(workManager, work, work.requestMsg.getTransactionContext(), 0);
        	}
        	else {
        		this.workManager.scheduleWork(workManager, work);
        	}
		} catch (WorkException e) {
			throw new ConnectorException(e);
		}
    }
    
    void reenqueueRequest(WorkManager workManager, AsynchConnectorWorkItem work)  throws ConnectorException {
    	enqueueRequest(workManager, work);
    }
    
    ConnectorWorkItem getState(AtomicRequestID requestId) {
        return requestStates.get(requestId);
    }
    
    @SuppressWarnings("unused")
	public void requstMore(AtomicRequestID requestId) throws ConnectorException {
    	ConnectorWorkItem workItem = getState(requestId);
    	if (workItem == null) {
    		return; //already closed
    	}
	    workItem.requestMore();
    }
    
    public void cancelRequest(AtomicRequestID requestId) {
    	ConnectorWorkItem workItem = getState(requestId);
    	if (workItem == null) {
    		return; //already closed
    	}
	    workItem.requestCancel();
    }
    
    public void closeRequest(AtomicRequestID requestId) {
    	ConnectorWorkItem workItem = getState(requestId);
    	if (workItem == null) {
    		return; //already closed
    	}
	    workItem.requestClose();
    }
    
    /**
     * Schedule a task to be executed after the specified delay (in milliseconds) 
     * @param task The task to execute
     * @param delay The delay to wait (in ms) before executing the task
     * @since 4.3.3
     */
    public void scheduleTask(WorkManager workManager, final AsynchConnectorWorkItem state, long delay) throws ConnectorException {
    	try {
			this.workManager.scheduleWork(workManager, new Work() {
				@Override
				public void run() {
					state.requestMore();
				}
				@Override
				public void release() {
					
				}
			}, null, delay);
		} catch (WorkException e) {
			throw new ConnectorException(e);
		}        
    }
    
    /**
     * Remove the state associated with
     * the given <code>RequestID</code>.
     */
    void removeState(AtomicRequestID id) {
    	LogManager.logDetail(LogConstants.CTX_CONNECTOR, new Object[] {id, "Remove State"}); //$NON-NLS-1$
        requestStates.remove(id);    
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
    public synchronized void start() throws ConnectorException {
    	if (this.state != ConnectorStatus.NOT_INITIALIZED) {
    		return;
    	}
    	this.state = ConnectorStatus.INIT_FAILED;
        
        LogManager.logInfo(LogConstants.CTX_CONNECTOR, DQPPlugin.Util.getString("ConnectorManagerImpl.Initializing_connector", connectorName)); //$NON-NLS-1$

     	ConnectorEnvironment connectorEnv = null;
		
		connectorEnv = getConnector().getConnectorEnvironment();
    	
    	if (!connectorEnv.isSynchWorkers() && connectorEnv.isXaCapable()) {
    		throw new ConnectorException(DQPPlugin.Util.getString("ConnectorManager.xa_capbility_not_supported", this.connectorName)); //$NON-NLS-1$
    	}

		this.workItemFactory = new ConnectorWorkItemFactory(this, connectorEnv.isSynchWorkers());
    	this.state = ConnectorStatus.OPEN;
    }
    
    /**
     * Stop this connector.
     */
    public void stop() {        
        synchronized (this) {
        	if (this.state == ConnectorStatus.CLOSED) {
        		return;
        	}
            this.state= ConnectorStatus.CLOSED;
		}
        
        if (workManager != null) {
        	this.workManager.shutdownNow();
        }
        
        //ensure that all requests receive a response
        for (ConnectorWorkItem workItem : this.requestStates.values()) {
        	try {
        		workItem.resultsReceiver.exceptionOccurred(new ConnectorException(DQPPlugin.Util.getString("Connector_Shutting_down", new Object[] {workItem.id, this.connectorName}))); //$NON-NLS-1$
        	} catch (Exception e) {
        		//ignore
        	}
		}
        
    }

    /**
     * Returns a list of QueueStats objects that represent the queues in
     * this service.
     * If there are no queues, an empty Collection is returned.
     */
    @ManagementProperty(description="Get Runtime workmanager statistics", use={ViewUse.STATISTIC}, readOnly=true)
    public WorkerPoolStatisticsMetadata getWorkManagerStatistics() {
        return workManager.getStats();
    }

    /**
     * Add begin point to transaction monitoring table.
     * @param qr Request that contains the MetaMatrix command information in the transaction.
     */
    void logSRCCommand(AtomicRequestMessage qr, ExecutionContext context, short cmdStatus, int finalRowCnt) {
    	if (!LogManager.isMessageToBeRecorded(LogConstants.CTX_COMMANDLOGGING, MessageLevel.INFO)) {
    		return;
    	}
        String sqlStr = null;
        if(cmdStatus == CommandLogMessage.CMD_STATUS_NEW){
        	Command cmd = qr.getCommand();
            sqlStr = cmd != null ? cmd.toString() : null;
        }
        String userName = qr.getWorkContext().getUserName();
        String transactionID = null;
        if ( qr.isTransactional() ) {
            transactionID = qr.getTransactionContext().getXid().toString();
        }
        
        String modelName = qr.getModelName();
        AtomicRequestID id = qr.getAtomicRequestID();
        
        short cmdPoint = cmdStatus == CommandLogMessage.CMD_STATUS_NEW ? CommandLogMessage.CMD_POINT_BEGIN : CommandLogMessage.CMD_POINT_END;
        String principal = userName == null ? "unknown" : userName; //$NON-NLS-1$
        
        CommandLogMessage message = null;
        if (cmdPoint == CommandLogMessage.CMD_POINT_BEGIN) {
            message = new CommandLogMessage(System.currentTimeMillis(), qr.getRequestID().toString(), id.getNodeID(), transactionID, modelName, connectorName, qr.getWorkContext().getConnectionID(), principal, sqlStr, context);
        } 
        else {
            boolean isCancelled = false;
            boolean errorOccurred = false;

            if (cmdStatus == CommandLogMessage.CMD_STATUS_CANCEL) {
                isCancelled = true;
            } else if (cmdStatus == CommandLogMessage.CMD_STATUS_ERROR) {
                errorOccurred = true;
            }
            message = new CommandLogMessage(System.currentTimeMillis(), qr.getRequestID().toString(), id.getNodeID(), transactionID, modelName, connectorName, qr.getWorkContext().getConnectionID(), principal, finalRowCnt, isCancelled, errorOccurred, context);
        }         
        LogManager.log(MessageLevel.DETAIL, LogConstants.CTX_COMMANDLOGGING, message);
    }
    
    /**
     * Get the <code>Connector</code> object managed by this
     * manager.
     * @return the <code>Connector</code>.
     */
    Connector getConnector() throws ConnectorException {
		try {
			InitialContext ic  = new InitialContext();
			return (Connector)ic.lookup(this.connectorName);    			
		} catch (NamingException e) {
			throw new ConnectorException(e, DQPPlugin.Util.getString("ConnectorManager.failed_to_lookup_connector", this.connectorName)); //$NON-NLS-1$
		}
    }
    
    DQPContextCache getContextCache() {
     	if (bufferService != null) {
    		return bufferService.getContextCache();
    	}
    	return null;
    }
    
    public ConnectorStatus getStatus() {
    	return this.state;
    }
    
    private void checkStatus() throws ConnectorException {
    	if (this.state != ConnectorStatus.OPEN) {
    		throw new ConnectorException(DQPPlugin.Util.getString("ConnectorManager.not_in_valid_state", this.connectorName));
    	}
    }
}
