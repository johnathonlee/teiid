/*
 * JBoss, Home of Professional Open Source.
 * Copyright (C) 2008 Red Hat, Inc.
 * Copyright (C) 2000-2007 MetaMatrix, Inc.
 * Licensed to Red Hat, Inc. under one or more contributor 
 * license agreements.  See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package com.metamatrix.dqp.internal.process;

import java.sql.ResultSet;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import com.metamatrix.api.exception.query.QueryResolverException;
import com.metamatrix.common.application.basic.BasicEnvironment;
import com.metamatrix.common.application.exception.ApplicationLifecycleException;
import com.metamatrix.dqp.internal.datamgr.impl.FakeTransactionService;
import com.metamatrix.dqp.internal.process.DQPCore.ConnectorCapabilitiesCache;
import com.metamatrix.dqp.message.RequestMessage;
import com.metamatrix.dqp.message.ResultsMessage;
import com.metamatrix.dqp.service.AutoGenDataService;
import com.metamatrix.dqp.service.DQPServiceNames;
import com.metamatrix.dqp.service.FakeBufferService;
import com.metamatrix.dqp.service.FakeMetadataService;
import com.metamatrix.dqp.service.FakeVDBService;
import com.metamatrix.jdbc.api.ExecutionProperties;
import com.metamatrix.platform.security.api.MetaMatrixSessionID;
import com.metamatrix.query.optimizer.capabilities.BasicSourceCapabilities;
import com.metamatrix.query.optimizer.capabilities.SourceCapabilities;
import com.metamatrix.query.unittest.FakeMetadataFactory;


public class TestDQPCore extends TestCase {

    public TestDQPCore(String name) {
        super(name);
    }

    private DQPCore exampleDQPCore() throws ApplicationLifecycleException {
        BasicEnvironment env = new BasicEnvironment();
        Properties props = new Properties();
        env.setApplicationProperties(props);

        env.bindService(DQPServiceNames.BUFFER_SERVICE, new FakeBufferService());
        FakeMetadataService mdSvc = new FakeMetadataService();
        mdSvc.addVdb("bqt", "1", FakeMetadataFactory.exampleBQTCached()); //$NON-NLS-1$ //$NON-NLS-2$
        env.bindService(DQPServiceNames.METADATA_SERVICE, mdSvc);
        env.bindService(DQPServiceNames.DATA_SERVICE, new AutoGenDataService());
        env.bindService(DQPServiceNames.TRANSACTION_SERVICE, new FakeTransactionService());
        FakeVDBService vdbService = new FakeVDBService();
        vdbService.addBinding("bqt", "1", "BQT1", "mmuuid:blah", "BQT"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        vdbService.addBinding("bqt", "1", "BQT2", "mmuuid:blah", "BQT"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        vdbService.addBinding("bqt", "1", "BQT3", "mmuuid:blah", "BQT"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        env.bindService(DQPServiceNames.VDB_SERVICE, vdbService);

        DQPCore core = new DQPCore(env);
        core.start();
        return core;
    }
    
    @Override
    protected void setUp() throws Exception {
        DQPWorkContext workContext = new DQPWorkContext();
        workContext.setVdbName("bqt"); //$NON-NLS-1$
        workContext.setVdbVersion("1"); //$NON-NLS-1$
        workContext.setSessionId(new MetaMatrixSessionID(1));
        DQPWorkContext.setWorkContext(workContext);
    }
    
    @Override
    protected void tearDown() throws Exception {
    	DQPWorkContext.setWorkContext(new DQPWorkContext());
    }

    public RequestMessage exampleRequestMessage(String sql) {
        RequestMessage msg = new RequestMessage();
        msg.setCommand(sql);
        msg.setCallableStatement(false);
        msg.setCursorType(ResultSet.TYPE_SCROLL_INSENSITIVE);
        msg.setFetchSize(10);
        msg.setPartialResults(false);
        msg.setExecutionId(100);
        return msg;
    }

    public void testRequest1() throws Exception {
    	helpExecute("SELECT IntKey FROM BQT1.SmallA", "a"); //$NON-NLS-1$
    }
    
    /**
     * the execute method is not really Synchronous, but a result can be expected in a short amount of time
     * after initial planning has completed successfully
     * @throws Exception
     */
    public void testSynchronousRequest() throws Exception {
        DQPCore core = exampleDQPCore();
        RequestMessage reqMsg = exampleRequestMessage("SELECT IntKey FROM BQT1.SmallA"); //$NON-NLS-1$
        reqMsg.setSynchronousRequest(true);
        Future<ResultsMessage> message = core.executeRequest(reqMsg.getExecutionId(), reqMsg);
        ResultsMessage results = message.get(5000, TimeUnit.MILLISECONDS);
        assertEquals(0, results.getResults().length);
        assertTrue(results.getFinalRow() < 0);
    }

    public void testUser1() throws Exception {
        String sql = "SELECT IntKey FROM BQT1.SmallA WHERE user() = 'logon'"; //$NON-NLS-1$
        String userName = "logon"; //$NON-NLS-1$
        helpExecute(sql, userName);
    }

    public void testUser2() throws Exception {
        String sql = "SELECT IntKey FROM BQT1.SmallA WHERE user() LIKE 'logon'"; //$NON-NLS-1$
        String userName = "logon"; //$NON-NLS-1$
        helpExecute(sql, userName);
    }

    public void testUser3() throws Exception {
        String sql = "SELECT IntKey FROM BQT1.SmallA WHERE user() IN ('logon3') AND StringKey LIKE '1'"; //$NON-NLS-1$
        String userName = "logon3"; //$NON-NLS-1$
        helpExecute(sql, userName);
    }

    public void testUser4() throws Exception {
        String sql = "SELECT IntKey FROM BQT1.SmallA WHERE 'logon4' = user() AND StringKey = '1'"; //$NON-NLS-1$
        String userName = "logon4"; //$NON-NLS-1$
        helpExecute(sql, userName);
    }

    public void testUser5() throws Exception {
        String sql = "SELECT IntKey FROM BQT1.SmallA WHERE user() IS NULL "; //$NON-NLS-1$
        String userName = "logon"; //$NON-NLS-1$
        helpExecute(sql, userName);
    }

    public void testUser6() throws Exception {
        String sql = "SELECT IntKey FROM BQT1.SmallA WHERE user() = 'logon33' "; //$NON-NLS-1$
        String userName = "logon"; //$NON-NLS-1$
        helpExecute(sql, userName);
    }

    public void testUser7() throws Exception {
        String sql = "UPDATE BQT1.SmallA SET IntKey = 2 WHERE user() = 'logon' AND StringKey = '1' "; //$NON-NLS-1$
        String userName = "logon"; //$NON-NLS-1$
        helpExecute(sql, userName);
    }

    public void testUser8() throws Exception {
        String sql = "SELECT user(), StringKey FROM BQT1.SmallA WHERE IntKey = 1 "; //$NON-NLS-1$
        String userName = "logon"; //$NON-NLS-1$
        helpExecute(sql, userName);
    }

    public void testUser9() throws Exception {
        String sql = "SELECT IntKey FROM BQT1.SmallA WHERE user() = StringKey AND StringKey = '1' "; //$NON-NLS-1$
        String userName = "1"; //$NON-NLS-1$
        helpExecute(sql, userName);
    }
    
    public void testTxnAutoWrap() throws Exception {
    	String sql = "SELECT * FROM BQT1.SmallA"; //$NON-NLS-1$
    	helpExecute(sql, "a", true); //$NON-NLS-1$
    }
    
    public void testPlanOnly() throws Exception {
    	String sql = "SELECT * FROM BQT1.SmallA option planonly"; //$NON-NLS-1$
    	helpExecute(sql,"a"); //$NON-NLS-1$
    }
    
    /**
     * Tests whether an exception result is sent when an exception occurs
     * @since 4.3
     */
    public void testPlanningException() throws Exception {
        String sql = "SELECT IntKey FROM BQT1.BadIdea "; //$NON-NLS-1$
        
        DQPCore core = exampleDQPCore();
        RequestMessage reqMsg = exampleRequestMessage(sql);

        Future<ResultsMessage> message = core.executeRequest(reqMsg.getExecutionId(), reqMsg);
        try {
        	message.get(5000, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
        	assertTrue(e.getCause() instanceof QueryResolverException);
        }
    }
    
    public void testCapabilitesCache() {
    	ConnectorCapabilitiesCache cache = new ConnectorCapabilitiesCache();
    	DQPWorkContext workContext = new DQPWorkContext();
    	workContext.setVdbName("foo");
    	workContext.setVdbVersion("1");
    	Map<String, SourceCapabilities> vdbCapabilites = cache.getVDBConnectorCapabilities(workContext);
    	assertNull(vdbCapabilites.get("model1"));
    	vdbCapabilites.put("model1", new BasicSourceCapabilities());
    	vdbCapabilites = cache.getVDBConnectorCapabilities(workContext);
    	assertNotNull(vdbCapabilites.get("model1"));
    	workContext.setVdbName("bar");
    	vdbCapabilites = cache.getVDBConnectorCapabilities(workContext);
    	assertNull(vdbCapabilites.get("model1"));
    }

    ///////////////////////////Helper method///////////////////////////////////
    private void helpExecute(String sql, String userName) throws Exception {
    	helpExecute(sql, userName, false);
    }

    private void helpExecute(String sql, String userName, boolean txnAutoWrap) throws Exception {
        DQPCore core = exampleDQPCore();
        RequestMessage reqMsg = exampleRequestMessage(sql);
        DQPWorkContext.getWorkContext().setUserName(userName);
        if (txnAutoWrap) {
        	reqMsg.setTxnAutoWrapMode(ExecutionProperties.AUTO_WRAP_ON);
        }

        Future<ResultsMessage> message = core.executeRequest(reqMsg.getExecutionId(), reqMsg);
        ResultsMessage results = message.get(50000, TimeUnit.MILLISECONDS);
        assertNull(results.getException());
    }
}
