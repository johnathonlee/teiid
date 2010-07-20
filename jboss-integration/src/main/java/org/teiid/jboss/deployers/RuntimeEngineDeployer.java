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
package org.teiid.jboss.deployers;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.WorkManager;
import javax.transaction.TransactionManager;

import org.jboss.managed.api.ManagedOperation.Impact;
import org.jboss.managed.api.annotation.ManagementComponent;
import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementOperation;
import org.jboss.managed.api.annotation.ManagementParameter;
import org.jboss.managed.api.annotation.ManagementProperties;
import org.jboss.managed.api.annotation.ManagementProperty;
import org.jboss.managed.api.annotation.ViewUse;
import org.jboss.profileservice.spi.ProfileService;
import org.jboss.util.naming.Util;
import org.teiid.adminapi.Admin;
import org.teiid.adminapi.AdminComponentException;
import org.teiid.adminapi.AdminException;
import org.teiid.adminapi.impl.DQPManagement;
import org.teiid.adminapi.impl.RequestMetadata;
import org.teiid.adminapi.impl.SessionMetadata;
import org.teiid.adminapi.impl.WorkerPoolStatisticsMetadata;
import org.teiid.adminapi.jboss.AdminProvider;
import org.teiid.client.DQP;
import org.teiid.client.security.ILogon;
import org.teiid.client.util.ExceptionUtil;
import org.teiid.core.ComponentNotFoundException;
import org.teiid.core.TeiidComponentException;
import org.teiid.core.TeiidRuntimeException;
import org.teiid.deployers.VDBRepository;
import org.teiid.dqp.internal.datamgr.ConnectorManagerRepository;
import org.teiid.dqp.internal.process.DQPConfiguration;
import org.teiid.dqp.internal.process.DQPCore;
import org.teiid.dqp.internal.process.DQPWorkContext;
import org.teiid.dqp.internal.transaction.TransactionServerImpl;
import org.teiid.dqp.service.BufferService;
import org.teiid.dqp.service.SessionService;
import org.teiid.dqp.service.SessionServiceException;
import org.teiid.dqp.service.TransactionService;
import org.teiid.jboss.IntegrationPlugin;
import org.teiid.logging.Log4jListener;
import org.teiid.logging.LogConstants;
import org.teiid.logging.LogManager;
import org.teiid.logging.MessageLevel;
import org.teiid.security.SecurityHelper;
import org.teiid.transport.ClientServiceRegistry;
import org.teiid.transport.ClientServiceRegistryImpl;
import org.teiid.transport.LogonImpl;
import org.teiid.transport.ODBCSocketListener;
import org.teiid.transport.SocketConfiguration;
import org.teiid.transport.SocketListener;


@ManagementObject(isRuntime=true, componentType=@ManagementComponent(type="teiid",subtype="dqp"), properties=ManagementProperties.EXPLICIT)
public class RuntimeEngineDeployer extends DQPConfiguration implements DQPManagement, Serializable , ClientServiceRegistry  {
	private static final long serialVersionUID = -4676205340262775388L;

	private transient SocketConfiguration jdbcSocketConfiguration;
	private transient SocketConfiguration adminSocketConfiguration;
	private transient SocketConfiguration odbcSocketConfiguration;
	private transient SocketListener jdbcSocket;	
	private transient SocketListener adminSocket;
	private transient SocketListener odbcSocket;
	private transient TransactionServerImpl transactionServerImpl = new TransactionServerImpl();
		
	private transient DQPCore dqpCore = new DQPCore();
	private transient SessionService sessionService;
	private transient ILogon logon;
	private transient Admin admin;
	private transient ClientServiceRegistryImpl csr = new ClientServiceRegistryImpl();	
	private transient VDBRepository vdbRepository;

	private transient ProfileService profileService;
	private transient String jndiName;
	
    public RuntimeEngineDeployer() {
		// TODO: this does not belong here
		LogManager.setLogListener(new Log4jListener());
    }
	
	@Override
	public <T> T getClientService(Class<T> iface)
			throws ComponentNotFoundException {
		return this.csr.getClientService(iface);
	}
	
	@Override
	public SecurityHelper getSecurityHelper() {
		return this.csr.getSecurityHelper();
	}
	
    public void start() {
		dqpCore.setTransactionService((TransactionService)LogManager.createLoggingProxy(LogConstants.CTX_TXN_LOG, transactionServerImpl, new Class[] {TransactionService.class}, MessageLevel.DETAIL));

    	// create the necessary services
    	createClientServices();
    	
    	this.csr.registerClientService(ILogon.class, logon, LogConstants.CTX_SECURITY);
    	this.csr.registerClientService(DQP.class, proxyService(DQP.class, this.dqpCore, LogConstants.CTX_DQP), LogConstants.CTX_DQP);
    	this.csr.registerClientService(Admin.class, proxyService(Admin.class, admin, LogConstants.CTX_ADMIN_API), LogConstants.CTX_ADMIN_API);
    	
    	if (this.jdbcSocketConfiguration.isEnabled()) {
	    	this.jdbcSocket = new SocketListener(this.jdbcSocketConfiguration, csr, this.dqpCore.getBufferManager());
	    	LogManager.logInfo(LogConstants.CTX_RUNTIME, IntegrationPlugin.Util.getString("socket_enabled","Teiid JDBC = ",(this.jdbcSocketConfiguration.getSSLConfiguration().isSslEnabled()?"mms://":"mm://")+this.jdbcSocketConfiguration.getHostAddress().getHostName()+":"+this.jdbcSocketConfiguration.getPortNumber())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    	} else {
    		LogManager.logInfo(LogConstants.CTX_RUNTIME, IntegrationPlugin.Util.getString("socket_not_enabled", "jdbc connections")); //$NON-NLS-1$ //$NON-NLS-2$
    	}
    	
    	if (this.adminSocketConfiguration.isEnabled()) {
	    	this.adminSocket = new SocketListener(this.adminSocketConfiguration, csr, this.dqpCore.getBufferManager());
	    	LogManager.logInfo(LogConstants.CTX_RUNTIME, IntegrationPlugin.Util.getString("socket_enabled","Teiid Admin", (this.adminSocketConfiguration.getSSLConfiguration().isSslEnabled()?"mms://":"mm://")+this.adminSocketConfiguration.getHostAddress().getHostName()+":"+this.adminSocketConfiguration.getPortNumber())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    	} else {
    		LogManager.logInfo(LogConstants.CTX_RUNTIME, IntegrationPlugin.Util.getString("socket_not_enabled", "admin connections")); //$NON-NLS-1$ //$NON-NLS-2$
    	}
    	
    	if (this.odbcSocketConfiguration.isEnabled()) {
    		this.vdbRepository.odbcEnabled();
	    	this.odbcSocket = new ODBCSocketListener(this.odbcSocketConfiguration, csr, this.dqpCore.getBufferManager());
	    	LogManager.logInfo(LogConstants.CTX_RUNTIME, IntegrationPlugin.Util.getString("odbc_enabled","Teiid ODBC - SSL=", (this.odbcSocketConfiguration.getSSLConfiguration().isSslEnabled()?"ON":"OFF")+" Host = "+this.odbcSocketConfiguration.getHostAddress().getHostName()+" Port = "+this.odbcSocketConfiguration.getPortNumber())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
    	} else {
    		LogManager.logInfo(LogConstants.CTX_RUNTIME, IntegrationPlugin.Util.getString("odbc_not_enabled")); //$NON-NLS-1$
    	}    	
    	
    	LogManager.logInfo(LogConstants.CTX_RUNTIME, IntegrationPlugin.Util.getString("engine_started", new Date(System.currentTimeMillis()).toString())); //$NON-NLS-1$
    	if (jndiName != null) {
	    	final InitialContext ic ;
	    	try {
	    		ic = new InitialContext() ;
	    		Util.bind(ic, jndiName, this) ;
	    	} catch (final NamingException ne) {
	    		// Add jndi_failed to bundle
	        	LogManager.logError(LogConstants.CTX_RUNTIME, ne, IntegrationPlugin.Util.getString("jndi_failed", new Date(System.currentTimeMillis()).toString())); //$NON-NLS-1$
	    	}
    	}
	}	
    
    public void stop() {
    	if (jndiName != null) {
	    	final InitialContext ic ;
	    	try {
	    		ic = new InitialContext() ;
	    		Util.unbind(ic, jndiName) ;
	    	} catch (final NamingException ne) {
	    	}
    	}
    	
    	try {
	    	this.dqpCore.stop();
    	} catch(TeiidRuntimeException e) {
    		// this bean is already shutdown
    	}
    	
    	// Stop socket transport(s)
    	if (this.jdbcSocket != null) {
    		this.jdbcSocket.stop();
    		this.jdbcSocket = null;
    	}
    	
    	if (this.adminSocket != null) {
    		this.adminSocket.stop();
    		this.adminSocket = null;
    	}    
    	
    	if (this.odbcSocket != null) {
    		this.odbcSocket.stop();
    		this.odbcSocket = null;
    	}      	
    	LogManager.logInfo(LogConstants.CTX_RUNTIME, IntegrationPlugin.Util.getString("engine_stopped", new Date(System.currentTimeMillis()).toString())); //$NON-NLS-1$
    }
    
	private void createClientServices() {
		
		this.dqpCore.start(this);
		
		this.logon = new LogonImpl(this.sessionService, "teiid-cluster"); //$NON-NLS-1$
		if (profileService != null) {
			this.admin = AdminProvider.getLocal(profileService);
		} else {
			try {
				this.admin = AdminProvider.getLocal();
			} catch (AdminComponentException e) {
				throw new TeiidRuntimeException(e.getCause());
			}
		}
	}    
	
	/**
	 * Creates an proxy to validate the incoming session
	 */
	private <T> T proxyService(final Class<T> iface, final T instance, String context) {

		return iface.cast(Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {iface}, new LogManager.LoggingProxy(instance, context, MessageLevel.TRACE) {

			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				Throwable exception = null;
				try {
					sessionService.validateSession(DQPWorkContext.getWorkContext().getSessionId());
					return super.invoke(proxy, method, args);
				} catch (InvocationTargetException e) {
					exception = e.getTargetException();
				} catch(Throwable t){
					exception = t;
				}
				throw ExceptionUtil.convertException(method, exception);
			}
		}));
	}
	
	public void setJdbcSocketConfiguration(SocketConfiguration socketConfig) {
		this.jdbcSocketConfiguration = socketConfig;
	}
	
	public void setAdminSocketConfiguration(SocketConfiguration socketConfig) {
		this.adminSocketConfiguration = socketConfig;
	}
	
	public void setOdbcSocketConfiguration(SocketConfiguration socketConfig) {
		this.odbcSocketConfiguration = socketConfig;
	}
    
    public void setXATerminator(XATerminator xaTerminator){
    	this.transactionServerImpl.setXaTerminator(xaTerminator);
    }   
    
    public void setTransactionManager(TransactionManager transactionManager) {
    	this.transactionServerImpl.setTransactionManager(transactionManager);
    }
    
    public void setWorkManager(WorkManager mgr) {
    	this.transactionServerImpl.setWorkManager(mgr);
    }
	
	public void setSessionService(SessionService service) {
		this.sessionService = service;
		service.setDqp(this.dqpCore);
	}
	
	public void setBufferService(BufferService service) {
		this.dqpCore.setBufferService(service);
	}
	
	public void setConnectorManagerRepository(ConnectorManagerRepository repo) {
		this.dqpCore.setConnectorManagerRepository(repo);
	}
	
	public void setSecurityHelper(SecurityHelper helper) {
		this.csr.setSecurityHelper(helper);
	}
	
	public void setVDBRepository(VDBRepository repo) {
		this.vdbRepository = repo;
	}
	
	public void setProfileService(final ProfileService profileService) {
		this.profileService = profileService ;
	}
	
	public void setJndiName(final String jndiName) {
		this.jndiName = jndiName ;
	}
	
	@Override
    @ManagementOperation(description="Requests for perticular session", impact=Impact.ReadOnly,params={@ManagementParameter(name="sessionId",description="The session Identifier")})
    public List<RequestMetadata> getRequestsForSession(String sessionId) {
		return this.dqpCore.getRequestsForSession(sessionId);
	}
	
	@Override
    @ManagementOperation(description="Requests using a certain VDB", impact=Impact.ReadOnly,params={@ManagementParameter(name="vdbName",description="VDB Name"), @ManagementParameter(name="vdbVersion",description="VDB Version")})
    public List<RequestMetadata> getRequestsUsingVDB(String vdbName, int vdbVersion) throws AdminException {
		List<RequestMetadata> requests = new ArrayList<RequestMetadata>();
		try {
			Collection<SessionMetadata> sessions = this.sessionService.getActiveSessions();
			for (SessionMetadata session:sessions) {
				if (session.getVDBName().equals(vdbName) && session.getVDBVersion() == vdbVersion) {
					requests.addAll(this.dqpCore.getRequestsForSession(session.getSessionId()));
				}
			}
		} catch (SessionServiceException e) {
			throw new AdminComponentException(e);
		}
		return requests;
	}
	
    
	@Override
    @ManagementOperation(description="Active requests", impact=Impact.ReadOnly)
    public List<RequestMetadata> getRequests() {
		return this.dqpCore.getRequests();
	}
	
	@Override
    @ManagementOperation(description="Long running requests", impact=Impact.ReadOnly)
    public List<RequestMetadata> getLongRunningRequests() {
		return this.dqpCore.getLongRunningRequests();
	}
	
	
	@Override
	@ManagementOperation(description="Get Runtime workmanager statistics", impact=Impact.ReadOnly,params={@ManagementParameter(name="identifier",description="Use \"runtime\" for engine, or connector name for connector")})
    public WorkerPoolStatisticsMetadata getWorkManagerStatistics(String identifier) {
		if ("runtime".equalsIgnoreCase(identifier)) { //$NON-NLS-1$
			return this.dqpCore.getWorkManagerStatistics();
		}
		/*ConnectorManager cm = this.dqpCore.getConnectorManagerRepository().getConnectorManager(identifier);
		if (cm != null) {
			return cm.getWorkManagerStatistics();
		}*/
		return null;
	}
	
	@Override
    @ManagementOperation(description="Terminate a Session",params={@ManagementParameter(name="terminateeId",description="The session to be terminated")})
    public void terminateSession(String terminateeId) {
		this.sessionService.terminateSession(terminateeId, DQPWorkContext.getWorkContext().getSessionId());
    }
    
	@Override
    @ManagementOperation(description="Cancel a Request",params={@ManagementParameter(name="sessionId",description="The session Identifier"), @ManagementParameter(name="requestId",description="The request Identifier")})    
    public boolean cancelRequest(String sessionId, long requestId) throws AdminException {
    	try {
			return this.dqpCore.cancelRequest(sessionId, requestId);
		} catch (TeiidComponentException e) {
			throw new AdminComponentException(e);
		}
    }
    
	@Override
    @ManagementOperation(description="Get Cache types in the system", impact=Impact.ReadOnly)
    public Collection<String> getCacheTypes(){
		return this.dqpCore.getCacheTypes();
	}
	
	@Override
	@ManagementOperation(description="Clear the caches in the system", impact=Impact.ReadOnly)
	public void clearCache(String cacheType) {
		this.dqpCore.clearCache(cacheType);
	}
	
	@Override
	@ManagementOperation(description="Active sessions", impact=Impact.ReadOnly)
	public Collection<SessionMetadata> getActiveSessions() throws AdminException {
		try {
			return this.sessionService.getActiveSessions();
		} catch (SessionServiceException e) {
			throw new AdminComponentException(e);
		}
	}
	
	@Override
	@ManagementProperty(description="Active session count", use={ViewUse.STATISTIC}, readOnly=true)
	public int getActiveSessionsCount() throws AdminException{
		try {
			return this.sessionService.getActiveSessionsCount();
		} catch (SessionServiceException e) {
			throw new AdminComponentException(e);
		}
	}
	
	@Override
	@ManagementOperation(description="Active Transactions", impact=Impact.ReadOnly)
	public Collection<org.teiid.adminapi.Transaction> getTransactions() {
		return this.dqpCore.getTransactions();
	}
	
	@Override
	@ManagementOperation(description="Terminate the transaction", impact=Impact.ReadOnly)
	public void terminateTransaction(String xid) throws AdminException {
		this.dqpCore.terminateTransaction(xid);
	}

	@Override
    @ManagementOperation(description="Merge Two VDBs",params={@ManagementParameter(name="sourceVDBName"),@ManagementParameter(name="sourceVDBName"), @ManagementParameter(name="targetVDBName"), @ManagementParameter(name="targetVDBVersion")})
	public void mergeVDBs(String sourceVDBName, int sourceVDBVersion,
			String targetVDBName, int targetVDBVersion) throws AdminException {
		this.vdbRepository.mergeVDBs(sourceVDBName, sourceVDBVersion, targetVDBName, targetVDBVersion);
	}	
}
