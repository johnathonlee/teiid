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

package org.teiid.dqp.internal.process;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;
import javax.transaction.xa.Xid;

import org.teiid.adminapi.Admin;
import org.teiid.adminapi.AdminException;
import org.teiid.adminapi.Request.ProcessingState;
import org.teiid.adminapi.Request.ThreadState;
import org.teiid.adminapi.impl.RequestMetadata;
import org.teiid.adminapi.impl.WorkerPoolStatisticsMetadata;
import org.teiid.cache.Cache;
import org.teiid.cache.CacheFactory;
import org.teiid.client.DQP;
import org.teiid.client.RequestMessage;
import org.teiid.client.ResultsMessage;
import org.teiid.client.lob.LobChunk;
import org.teiid.client.metadata.MetadataResult;
import org.teiid.client.util.ResultsFuture;
import org.teiid.client.util.ResultsReceiver;
import org.teiid.client.xa.XATransactionException;
import org.teiid.client.xa.XidImpl;
import org.teiid.common.buffer.BufferManager;
import org.teiid.core.TeiidComponentException;
import org.teiid.core.TeiidProcessingException;
import org.teiid.core.types.Streamable;
import org.teiid.dqp.DQPPlugin;
import org.teiid.dqp.internal.datamgr.ConnectorManagerRepository;
import org.teiid.dqp.internal.process.ThreadReuseExecutor.PrioritizedRunnable;
import org.teiid.dqp.message.AtomicRequestMessage;
import org.teiid.dqp.message.RequestID;
import org.teiid.dqp.service.BufferService;
import org.teiid.dqp.service.TransactionContext;
import org.teiid.dqp.service.TransactionService;
import org.teiid.dqp.service.TransactionContext.Scope;
import org.teiid.logging.CommandLogMessage;
import org.teiid.logging.LogConstants;
import org.teiid.logging.LogManager;
import org.teiid.logging.MessageLevel;
import org.teiid.logging.CommandLogMessage.Event;
import org.teiid.query.processor.ProcessorDataManager;
import org.teiid.query.tempdata.TempTableDataManager;
import org.teiid.query.tempdata.TempTableStore;


/**
 * Implements the core DQP processing.
 */
public class DQPCore implements DQP {
	
	//TODO: replace with FutureTask
	public final static class FutureWork<T> implements Work, WorkListener, PrioritizedRunnable {
		private final Callable<T> toCall;
		private ResultsFuture<T> result = new ResultsFuture<T>();
		private ResultsReceiver<T> receiver = result.getResultsReceiver();
		private int priority;
		private long creationTime = System.currentTimeMillis();

		public FutureWork(Callable<T> processor, int priority) {
			this.toCall = processor;
			this.priority = priority;
		}
		
		public ResultsFuture<T> getResult() {
			return result;
		}
		
		@Override
		public void run() {
			try {
				receiver.receiveResults(toCall.call());
			} catch (Throwable t) {
				receiver.exceptionOccurred(t);
			}
		}

		@Override
		public void release() {
			
		}
		
		@Override
		public void workAccepted(WorkEvent arg0) {
			
		}
		
		@Override
		public void workCompleted(WorkEvent arg0) {
			
		}
		
		@Override
		public void workRejected(WorkEvent arg0) {
			receiver.exceptionOccurred(arg0.getException());
		}
		
		@Override
		public void workStarted(WorkEvent arg0) {
			
		}

		@Override
		public int getPriority() {
			return priority;
		}
		
		@Override
		public long getCreationTime() {
			return creationTime;
		}
	}	
	
	static class ClientState {
		List<RequestID> requests;
		TempTableStore sessionTables;
		
		public ClientState(TempTableStore tableStoreImpl) {
			this.sessionTables = tableStoreImpl;
		}
		
		public synchronized void addRequest(RequestID requestID) {
			if (requests == null) {
				requests = new LinkedList<RequestID>();
			}
			requests.add(requestID);
		}
		
		public synchronized List<RequestID> getRequests() {
			if (requests == null) {
				return Collections.emptyList();
			}
			return new ArrayList<RequestID>(requests);
		}

		public synchronized void removeRequest(RequestID requestID) {
			if (requests != null) {
				requests.remove(requestID);
			}
		}
		
	}
	
	private ThreadReuseExecutor processWorkerPool;
    
    private int maxFetchSize = DQPConfiguration.DEFAULT_FETCH_SIZE;
    private int queryThreshold = DQPConfiguration.DEFAULT_QUERY_THRESHOLD;
    
    // Resources
    private BufferManager bufferManager;
    private ProcessorDataManager dataTierMgr;
    private SessionAwareCache<PreparedPlan> prepPlanCache;
    private SessionAwareCache<CachedResults> rsCache;
    private TransactionService transactionService;
    private BufferService bufferService;
    private ConnectorManagerRepository connectorManagerRepository;
    
    // Query worker pool for processing plans
    private int processorTimeslice = DQPConfiguration.DEFAULT_PROCESSOR_TIMESLICE;
    
    private int maxSourceRows = DQPConfiguration.DEFAULT_MAX_SOURCE_ROWS;
    private boolean exceptionOnMaxSourceRows = true;
    
    private int chunkSize = Streamable.STREAMING_BATCH_SIZE_IN_BYTES;
    
	private Map<RequestID, RequestWorkItem> requests = new ConcurrentHashMap<RequestID, RequestWorkItem>();			
	private Map<String, ClientState> clientState = new ConcurrentHashMap<String, ClientState>();
    private boolean useEntitlements = false;
    
    private int maxActivePlans = DQPConfiguration.DEFAULT_MAX_ACTIVE_PLANS;
    private int currentlyActivePlans;
    private LinkedList<RequestWorkItem> waitingPlans = new LinkedList<RequestWorkItem>();
    private CacheFactory cacheFactory;
    
    /**
     * perform a full shutdown and wait for 10 seconds for all threads to finish
     */
    public void stop() {
    	processWorkerPool.shutdownNow();
    	try {
			processWorkerPool.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
    	// TODO: Should we be doing more cleanup here??
		LogManager.logDetail(LogConstants.CTX_DQP, "Stopping the DQP"); //$NON-NLS-1$
    }
    
    /**
     * Return a list of {@link RequestMetadata} for the given session
     */
    public List<RequestMetadata> getRequestsForSession(String sessionId) {
    	ClientState state = getClientState(sessionId, false);
    	if (state == null) {
    		return Collections.emptyList();
    	}
        return buildRequestInfos(state.getRequests(), -1);
    }
    
    public ClientState getClientState(String key, boolean create) {
    	if (key == null) {
    		return null;
    	}
		ClientState state = clientState.get(key);
		if (state == null && create) {
			state = new ClientState(new TempTableStore(key));
    		clientState.put(key, state);
		}
		return state;
    }

    /**
     * Return a list of all {@link RequestMetadata} 
     */
    public List<RequestMetadata> getRequests() {
		return buildRequestInfos(requests.keySet(), -1);
    } 
    
    public List<RequestMetadata> getLongRunningRequests(){
    	return buildRequestInfos(requests.keySet(), this.queryThreshold);
    }

    private List<RequestMetadata> buildRequestInfos(Collection<RequestID> ids, int longRunningQueryThreshold) {
		List<RequestMetadata> results = new ArrayList<RequestMetadata>();
    	
		for (RequestID requestID : ids) {
            RequestWorkItem holder = requests.get(requestID);
            
            if(holder != null && !holder.isCanceled()) {
            	RequestMetadata req = new RequestMetadata();
            	
            	req.setExecutionId(holder.requestID.getExecutionID());
            	req.setSessionId(holder.requestID.getConnectionID());
            	req.setCommand(holder.requestMsg.getCommandString());
            	req.setStartTime(holder.getProcessingTimestamp());
            	req.setState(holder.isCanceled()?ProcessingState.CANCELED:holder.isDoneProcessing()?ProcessingState.DONE:ProcessingState.PROCESSING);
            	switch (holder.getThreadState()) {
            	case DONE:
            	case IDLE:
            		req.setThreadState(ThreadState.IDLE);
            		break;
            	default:
            		if (holder.isProcessing()) {
            			req.setThreadState(ThreadState.RUNNING);
            		} else {
            			req.setThreadState(ThreadState.QUEUED);
            		}
            	}
            	if (holder.getTransactionContext() != null && holder.getTransactionContext().getTransactionType() != Scope.NONE) {
            		req.setTransactionId(holder.getTransactionContext().getTransactionId());
            	}

                for (DataTierTupleSource conInfo : holder.getConnectorRequests()) {
                    String connectorName = conInfo.getConnectorName();

                    if (connectorName == null) {
                    	continue;
                    }
                    // If the request has not yet completed processing, then
                    // add all the subrequest messages
                	AtomicRequestMessage arm = conInfo.getAtomicRequestMessage();
                	RequestMetadata info = new RequestMetadata();
                	if (conInfo.isQueued()) {
                		info.setThreadState(ThreadState.QUEUED);
                	} else if (conInfo.isRunning()) {
                		info.setThreadState(ThreadState.RUNNING);
                	} else {
                		info.setThreadState(ThreadState.IDLE);
                	}
                	info.setExecutionId(arm.getRequestID().getExecutionID());
                	info.setSessionId(holder.requestID.getConnectionID());
                	info.setCommand(arm.getCommand().toString());
                	info.setStartTime(arm.getProcessingTimestamp());
                	info.setSourceRequest(true);
                	info.setNodeId(arm.getAtomicRequestID().getNodeID());
                	info.setState(conInfo.isCanceled()?ProcessingState.CANCELED:conInfo.isDone()?ProcessingState.DONE:ProcessingState.PROCESSING);
        			results.add(info);
                }
                
                // check if only need long running queries.
                long elapsedTime = System.currentTimeMillis() - req.getStartTime();
                if (longRunningQueryThreshold == -1 || elapsedTime > longRunningQueryThreshold) {
                	results.add(req);
                }
            }
        }
    	return results;
	}    

	public ResultsFuture<ResultsMessage> executeRequest(long reqID,RequestMessage requestMsg) {
    	DQPWorkContext workContext = DQPWorkContext.getWorkContext();
		RequestID requestID = workContext.getRequestID(reqID);
		requestMsg.setFetchSize(Math.min(requestMsg.getFetchSize(), maxFetchSize));
		Request request = null;
	    if ( requestMsg.isPreparedStatement() || requestMsg.isCallableStatement()) {
	    	request = new PreparedStatementRequest(prepPlanCache);
	    } else {
	    	request = new Request();
	    }
	    ClientState state = this.getClientState(workContext.getSessionId(), true);
	    request.initialize(requestMsg, bufferManager,
				dataTierMgr, transactionService, state.sessionTables,
				workContext, connectorManagerRepository,
				this.useEntitlements);
		
        ResultsFuture<ResultsMessage> resultsFuture = new ResultsFuture<ResultsMessage>();
        RequestWorkItem workItem = new RequestWorkItem(this, requestMsg, request, resultsFuture.getResultsReceiver(), requestID, workContext);
    	logMMCommand(workItem, Event.NEW, null); 
        addRequest(requestID, workItem, state);
        synchronized (waitingPlans) {
			if (currentlyActivePlans < maxActivePlans) {
				startActivePlan(workItem);
			} else {
				waitingPlans.add(workItem);
			}
		}
        return resultsFuture;
    }
	
	public ResultsFuture<ResultsMessage> processCursorRequest(long reqID,
			int batchFirst, int fetchSize) throws TeiidProcessingException {
        if (LogManager.isMessageToBeRecorded(LogConstants.CTX_DQP, MessageLevel.DETAIL)) {
            LogManager.logDetail(LogConstants.CTX_DQP, "DQP process cursor request from " + batchFirst);  //$NON-NLS-1$
        }
		DQPWorkContext workContext = DQPWorkContext.getWorkContext();
        ResultsFuture<ResultsMessage> resultsFuture = new ResultsFuture<ResultsMessage>();
		RequestWorkItem workItem = getRequestWorkItem(workContext.getRequestID(reqID));
		workItem.requestMore(batchFirst, batchFirst + Math.min(fetchSize, maxFetchSize) - 1, resultsFuture.getResultsReceiver());
		return resultsFuture;
	}

	void addRequest(RequestID requestID, RequestWorkItem workItem, ClientState state) {
		this.requests.put(requestID, workItem);
		state.addRequest(requestID);
	}

	private void startActivePlan(RequestWorkItem workItem) {
		workItem.active = true;
		this.addWork(workItem);
		this.currentlyActivePlans++;
	}
	
    void finishProcessing(final RequestWorkItem workItem) {
    	synchronized (waitingPlans) {
    		if (!workItem.active) {
        		return;
        	}
        	workItem.active = false;
    		currentlyActivePlans--;
			if (!waitingPlans.isEmpty()) {
				startActivePlan(waitingPlans.remove());
			}
		}
    }
    
    void removeRequest(final RequestWorkItem workItem) {
    	finishProcessing(workItem);
    	this.requests.remove(workItem.requestID);
    	ClientState state = getClientState(workItem.getDqpWorkContext().getSessionId(), false);
    	if (state != null) {
    		state.removeRequest(workItem.requestID);
    	}
    }
    
    void addWork(Runnable work) {
		this.processWorkerPool.execute(work);
    }
    
    void scheduleWork(final Runnable r, int priority, long delay) {
		this.processWorkerPool.schedule(new FutureWork<Void>(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				r.run();
				return null;
			}
		}, priority), delay, TimeUnit.MILLISECONDS);
    }
    
	public ResultsFuture<?> closeLobChunkStream(int lobRequestId,
			long requestId, String streamId)
			throws TeiidProcessingException {
        if (LogManager.isMessageToBeRecorded(LogConstants.CTX_DQP, MessageLevel.DETAIL)) {
            LogManager.logDetail(LogConstants.CTX_DQP, "Request to close the Lob stream with Stream id="+streamId+" instance id="+lobRequestId);  //$NON-NLS-1$//$NON-NLS-2$
        }   
        DQPWorkContext workContext = DQPWorkContext.getWorkContext();
        RequestWorkItem workItem = safeGetWorkItem(workContext.getRequestID(requestId));
        if (workItem != null) {
	        workItem.removeLobStream(lobRequestId);
        }
        return ResultsFuture.NULL_FUTURE;
    }
	    
	public ResultsFuture<LobChunk> requestNextLobChunk(int lobRequestId,
			long requestId, String streamId)
			throws TeiidProcessingException {
        if (LogManager.isMessageToBeRecorded(LogConstants.CTX_DQP, MessageLevel.DETAIL)) {
            LogManager.logDetail(LogConstants.CTX_DQP, "Request for next Lob chunk with Stream id="+streamId+" instance id="+lobRequestId);  //$NON-NLS-1$//$NON-NLS-2$
        }  
        RequestWorkItem workItem = getRequestWorkItem(DQPWorkContext.getWorkContext().getRequestID(requestId));
        ResultsFuture<LobChunk> resultsFuture = new ResultsFuture<LobChunk>();
        workItem.processLobChunkRequest(streamId, lobRequestId, resultsFuture.getResultsReceiver());
        return resultsFuture;
    }
    
//    /**
//     * Cancels a node in the request. (This request is called by the 
//     * client directly using the admin API), so if this does not support
//     * partial results then remove the original request.
//     * @throws MetaMatrixComponentException 
//     */
//    public void cancelAtomicRequest(AtomicRequestID requestID) throws MetaMatrixComponentException {                    
//        RequestWorkItem workItem = safeGetWorkItem(requestID.getRequestID());
//        if (workItem == null) {
//    		LogManager.logDetail(LogConstants.CTX_DQP, "Could not cancel", requestID, "parent request does not exist"); //$NON-NLS-1$ //$NON-NLS-2$
//        	return;
//        }
//        workItem.requestAtomicRequestCancel(requestID);
//    }
    
    RequestWorkItem getRequestWorkItem(RequestID reqID) throws TeiidProcessingException {
    	RequestWorkItem result = this.requests.get(reqID);
    	if (result == null) {
    		throw new TeiidProcessingException(DQPPlugin.Util.getString("DQPCore.The_request_has_been_closed.", reqID));//$NON-NLS-1$
    	}
    	return result;
    }
    
	RequestWorkItem safeGetWorkItem(Object processorID) {
    	return this.requests.get(processorID);
	}
	
    public WorkerPoolStatisticsMetadata getWorkManagerStatistics() {
    	return this.processWorkerPool.getStats();
    }
           
    public void terminateSession(String sessionId) {
        // sometimes there will not be any atomic requests pending, in that
        // situation we still need to clear the master request from our map
        ClientState state = getClientState(sessionId, false);
        if (state != null) {
	        for (RequestID reqId : state.getRequests()) {
	            try {
	                cancelRequest(reqId);
	            } catch (TeiidComponentException err) {
	                LogManager.logWarning(LogConstants.CTX_DQP, err, "Failed to cancel " + reqId); //$NON-NLS-1$
				}
	        }
        }
        
        try {
            transactionService.cancelTransactions(sessionId, false);
        } catch (XATransactionException err) {
            LogManager.logWarning(LogConstants.CTX_DQP, "rollback failed for requestID=" + sessionId); //$NON-NLS-1$
        } 
    }

    public boolean cancelRequest(String sessionId, long requestId) throws TeiidComponentException {
    	RequestID requestID = new RequestID(sessionId, requestId);
    	return cancelRequest(requestID);
    }
    
    private boolean cancelRequest(RequestID requestID) throws TeiidComponentException {
        if (LogManager.isMessageToBeRecorded(LogConstants.CTX_DQP, MessageLevel.DETAIL)) {
            LogManager.logDetail(LogConstants.CTX_DQP, "cancelQuery for requestID=" + requestID); //$NON-NLS-1$
        }
        
        boolean markCancelled = false;
        
        RequestWorkItem workItem = safeGetWorkItem(requestID);
        if (workItem != null) {
        	markCancelled = workItem.requestCancel();
        }
    	if (markCancelled) {
            logMMCommand(workItem, Event.CANCEL, null);
    	} else {
    		LogManager.logDetail(LogConstants.CTX_DQP, DQPPlugin.Util.getString("DQPCore.failed_to_cancel")); //$NON-NLS-1$
    	}
        return markCancelled;
    }
    
	public ResultsFuture<?> closeRequest(long requestId) throws TeiidProcessingException, TeiidComponentException {
        DQPWorkContext workContext = DQPWorkContext.getWorkContext();
        closeRequest(workContext.getRequestID(requestId));
        return ResultsFuture.NULL_FUTURE;
	}
    
    /**
     * Close the request with given ID 
     * @param requestID
     * @throws TeiidComponentException 
     */
    void closeRequest(RequestID requestID) throws TeiidComponentException {
        if (LogManager.isMessageToBeRecorded(LogConstants.CTX_DQP, MessageLevel.DETAIL)) {
            LogManager.logDetail(LogConstants.CTX_DQP, "closeQuery for requestID=" + requestID); //$NON-NLS-1$
        }
        
        RequestWorkItem workItem = safeGetWorkItem(requestID);
        if (workItem != null) {
        	workItem.requestClose();
        } else {
        	LogManager.logDetail(LogConstants.CTX_DQP, requestID + " close call ignored as the request has already been removed."); //$NON-NLS-1$
        }
    }
    
    private void clearPlanCache(){
        LogManager.logInfo(LogConstants.CTX_DQP, DQPPlugin.Util.getString("DQPCore.Clearing_prepared_plan_cache")); //$NON-NLS-1$
        this.prepPlanCache.clearAll();
    }

	private void clearResultSetCache() {
		//clear cache in server
		if(rsCache != null){
			rsCache.clearAll();
		}
	}
	
	
    public Collection<String> getCacheTypes(){
    	ArrayList<String> caches = new ArrayList<String>();
    	caches.add(Admin.Cache.PREPARED_PLAN_CACHE.toString());
    	caches.add(Admin.Cache.QUERY_SERVICE_RESULT_SET_CACHE.toString());
    	return caches;
    }	
	
	public void clearCache(String cacheType) {
		Admin.Cache cache = Admin.Cache.valueOf(cacheType);
		switch (cache) {
		case PREPARED_PLAN_CACHE:
			clearPlanCache();
			break;
		case QUERY_SERVICE_RESULT_SET_CACHE:
			clearResultSetCache();
			break;
		}
	}
    
	public Collection<org.teiid.adminapi.Transaction> getTransactions() {
		return this.transactionService.getTransactions();
	}
	
	public void terminateTransaction(String xid) throws AdminException {
		this.transactionService.terminateTransaction(xid);
	}	
	
    void logMMCommand(RequestWorkItem workItem, Event status, Integer rowCount) {
    	if (!LogManager.isMessageToBeRecorded(LogConstants.CTX_COMMANDLOGGING, MessageLevel.DETAIL)) {
    		return;
    	}
    	
        RequestMessage msg = workItem.requestMsg;
        DQPWorkContext workContext = DQPWorkContext.getWorkContext();
        RequestID rID = new RequestID(workContext.getSessionId(), msg.getExecutionId());
    	String txnID = null;
		TransactionContext tc = workItem.getTransactionContext();
		if (tc != null && tc.getTransactionType() != Scope.NONE) {
			txnID = tc.getTransactionId();
		}
    	String appName = workContext.getAppName();
        // Log to request log
        CommandLogMessage message = null;
        if (status == Event.NEW) {
            message = new CommandLogMessage(System.currentTimeMillis(), rID.toString(), txnID, workContext.getSessionId(), appName, workContext.getUserName(), workContext.getVdbName(), workContext.getVdbVersion(), msg.getCommandString());
        } else {
            message = new CommandLogMessage(System.currentTimeMillis(), rID.toString(), txnID, workContext.getSessionId(), workContext.getUserName(), workContext.getVdbName(), workContext.getVdbVersion(), rowCount, status);
        }
        LogManager.log(MessageLevel.DETAIL, LogConstants.CTX_COMMANDLOGGING, message);
    }
    
    ProcessorDataManager getDataTierManager() {
    	return this.dataTierMgr;
    }
    
	public BufferManager getBufferManager() {
		return bufferManager;
	}

	public TransactionService getTransactionService() {
		return transactionService;
	}

	SessionAwareCache<CachedResults> getRsCache() {
		return rsCache;
	}
	
	int getProcessorTimeSlice() {
		return this.processorTimeslice;
	}	
	
	int getChunkSize() {
		return chunkSize;
	}
	
	public void start(DQPConfiguration config) {
		this.processorTimeslice = config.getTimeSliceInMilli();
        this.maxFetchSize = config.getMaxRowsFetchSize();
        this.useEntitlements = config.useEntitlements();
        this.queryThreshold = config.getQueryThresholdInSecs();
        this.maxSourceRows = config.getMaxSourceRows();
        this.exceptionOnMaxSourceRows = config.isExceptionOnMaxSourceRows();
        
        this.chunkSize = config.getLobChunkSizeInKB() * 1024;
        
        //result set cache
        if (config.isResultSetCacheEnabled()) {
			this.rsCache = new SessionAwareCache<CachedResults>(config.getResultSetCacheMaxEntries(), this.cacheFactory, Cache.Type.RESULTSET);
			this.rsCache.setBufferManager(this.bufferManager);
        }

        //prepared plan cache
        prepPlanCache = new SessionAwareCache<PreparedPlan>(config.getPreparedPlanCacheMaxCount(), this.cacheFactory, Cache.Type.PREPAREDPLAN);
        prepPlanCache.setBufferManager(this.bufferManager);
		
        //get buffer manager
        this.bufferManager = bufferService.getBufferManager();

        this.processWorkerPool = new ThreadReuseExecutor(DQPConfiguration.PROCESS_PLAN_QUEUE_NAME, config.getMaxThreads());
        
        dataTierMgr = new TempTableDataManager(new DataTierManagerImpl(this,
                                            this.connectorManagerRepository,
                                            this.bufferService), this.bufferManager); 
	}
	
	public void setBufferService(BufferService service) {
		this.bufferService = service;
	}
	
	public void setTransactionService(TransactionService service) {
		this.transactionService = service;
	}
	
	@Override
	public boolean cancelRequest(long requestID)
			throws TeiidProcessingException, TeiidComponentException {
		DQPWorkContext workContext = DQPWorkContext.getWorkContext();
		return this.cancelRequest(workContext.getRequestID(requestID));
	}
	
	// local txn
	public ResultsFuture<?> begin() throws XATransactionException {
    	String threadId = DQPWorkContext.getWorkContext().getSessionId();
    	this.getTransactionService().begin(threadId);
    	return ResultsFuture.NULL_FUTURE;
	}
	
	// local txn
	public ResultsFuture<?> commit() throws XATransactionException {
		final String threadId = DQPWorkContext.getWorkContext().getSessionId();
		Callable<Void> processor = new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				getTransactionService().commit(threadId);
				return null;
			}
		};
		return addWork(processor, 0);
	}
	
	// local txn
	public ResultsFuture<?> rollback() throws XATransactionException {
		final String threadId = DQPWorkContext.getWorkContext().getSessionId();
		Callable<Void> processor = new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				getTransactionService().rollback(threadId);
				return null;
			}
		};
		return addWork(processor, 0);
	}

	// global txn
	public ResultsFuture<?> commit(final XidImpl xid, final boolean onePhase) throws XATransactionException {
		Callable<Void> processor = new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				DQPWorkContext workContext = DQPWorkContext.getWorkContext();
				getTransactionService().commit(workContext.getSessionId(), xid, onePhase, workContext.getSession().isEmbedded());
				return null;
			}
		};
		return addWork(processor, 0);
	}
	// global txn
	public ResultsFuture<?> end(XidImpl xid, int flags) throws XATransactionException {
		DQPWorkContext workContext = DQPWorkContext.getWorkContext();
		this.getTransactionService().end(workContext.getSessionId(), xid, flags, workContext.getSession().isEmbedded());
		return ResultsFuture.NULL_FUTURE;
	}
	// global txn
	public ResultsFuture<?> forget(XidImpl xid) throws XATransactionException {
		DQPWorkContext workContext = DQPWorkContext.getWorkContext();
		this.getTransactionService().forget(workContext.getSessionId(), xid, workContext.getSession().isEmbedded());
		return ResultsFuture.NULL_FUTURE;
	}
		
	// global txn
	public ResultsFuture<Integer> prepare(final XidImpl xid) throws XATransactionException {
		Callable<Integer> processor = new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				DQPWorkContext workContext = DQPWorkContext.getWorkContext();
				return getTransactionService().prepare(workContext.getSessionId(),xid, workContext.getSession().isEmbedded());
			}
		};
		return addWork(processor, 10);
	}

	<T> ResultsFuture<T> addWork(Callable<T> processor, int priority) {
		FutureWork<T> work = new FutureWork<T>(processor, priority);
		this.addWork(work);
		return work.getResult();
	}
	
	// global txn
	public ResultsFuture<Xid[]> recover(int flag) throws XATransactionException {
		ResultsFuture<Xid[]> result = new ResultsFuture<Xid[]>();
		DQPWorkContext workContext = DQPWorkContext.getWorkContext();
		result.getResultsReceiver().receiveResults(this.getTransactionService().recover(flag, workContext.getSession().isEmbedded()));
		return result;
	}
	// global txn
	public ResultsFuture<?> rollback(final XidImpl xid) throws XATransactionException {
		Callable<Void> processor = new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				DQPWorkContext workContext = DQPWorkContext.getWorkContext();
				getTransactionService().rollback(workContext.getSessionId(),xid, workContext.getSession().isEmbedded());
				return null;
			}
		};
		return addWork(processor, 0);
	}
	// global txn
	public ResultsFuture<?> start(final XidImpl xid, final int flags, final int timeout)
			throws XATransactionException {
		Callable<Void> processor = new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				DQPWorkContext workContext = DQPWorkContext.getWorkContext();
				getTransactionService().start(workContext.getSessionId(), xid, flags, timeout, workContext.getSession().isEmbedded());
				return null;
			}
		};
		return addWork(processor, 100);
	}

	public MetadataResult getMetadata(long requestID)
			throws TeiidComponentException, TeiidProcessingException {
		DQPWorkContext workContext = DQPWorkContext.getWorkContext();
		MetaDataProcessor processor = new MetaDataProcessor(this, this.prepPlanCache,  workContext.getVdbName(), workContext.getVdbVersion());
		return processor.processMessage(workContext.getRequestID(requestID), workContext, null, true);
	}

	public MetadataResult getMetadata(long requestID, String preparedSql,
			boolean allowDoubleQuotedVariable)
			throws TeiidComponentException, TeiidProcessingException {
		DQPWorkContext workContext = DQPWorkContext.getWorkContext();
		MetaDataProcessor processor = new MetaDataProcessor(this, this.prepPlanCache, workContext.getVdbName(), workContext.getVdbVersion());
		return processor.processMessage(workContext.getRequestID(requestID), workContext, preparedSql, allowDoubleQuotedVariable);
	}
	
	public void setConnectorManagerRepository(ConnectorManagerRepository repo) {
		this.connectorManagerRepository = repo;
	}
	
	public ConnectorManagerRepository getConnectorManagerRepository() {
		return this.connectorManagerRepository;
	}
	
	public boolean isExceptionOnMaxSourceRows() {
		return exceptionOnMaxSourceRows;
	}
	
	public int getMaxSourceRows() {
		return maxSourceRows;
	}
	
	public void setCacheFactory(CacheFactory factory) {
		this.cacheFactory = factory;
	}	
	
}