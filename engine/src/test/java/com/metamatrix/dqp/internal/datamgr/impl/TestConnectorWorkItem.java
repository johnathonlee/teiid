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

package com.metamatrix.dqp.internal.datamgr.impl;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import com.metamatrix.common.comm.api.ResultsReceiver;
import com.metamatrix.connector.api.ProcedureExecution;
import com.metamatrix.connector.exception.ConnectorException;
import com.metamatrix.connector.language.IProcedure;
import com.metamatrix.dqp.client.ResultsFuture;
import com.metamatrix.dqp.internal.datamgr.ConnectorID;
import com.metamatrix.dqp.internal.datamgr.language.LanguageBridgeFactory;
import com.metamatrix.dqp.internal.process.DQPWorkContext;
import com.metamatrix.dqp.message.AtomicRequestMessage;
import com.metamatrix.dqp.message.AtomicResultsMessage;
import com.metamatrix.dqp.message.RequestID;
import com.metamatrix.dqp.message.RequestMessage;
import com.metamatrix.dqp.service.FakeMetadataService;
import com.metamatrix.query.metadata.QueryMetadataInterface;
import com.metamatrix.query.parser.QueryParser;
import com.metamatrix.query.resolver.QueryResolver;
import com.metamatrix.query.sql.lang.Command;
import com.metamatrix.query.unittest.FakeMetadataFacade;
import com.metamatrix.query.unittest.FakeMetadataFactory;

public class TestConnectorWorkItem extends TestCase {

	private static final FakeMetadataFacade EXAMPLE_BQT = FakeMetadataFactory
			.exampleBQTCached();

	private static Command helpGetCommand(String sql,
			QueryMetadataInterface metadata) throws Exception {
		Command command = QueryParser.getQueryParser().parseCommand(sql);
		QueryResolver.resolveCommand(command, metadata);
		return command;
	}

	static ConnectorManager getConnectorManager() {
		ConnectorManager cm = new ConnectorManager();
		cm.setConnector(new ConnectorWrapper(new FakeConnector()));
		cm.setTransactionService(new FakeTransactionService());
		cm.setMetadataService(new FakeMetadataService());
		return cm;
	}

	static AtomicRequestMessage createNewAtomicRequestMessage(int requestid,
			int nodeid) throws Exception {
		RequestMessage rm = new RequestMessage();
		DQPWorkContext workContext = new DQPWorkContext();
		workContext.setUserName("foo"); //$NON-NLS-1$
		AtomicRequestMessage request = new AtomicRequestMessage(rm,
				workContext, nodeid);
		request.setCommand(helpGetCommand(
				"SELECT BQT1.SmallA.INTKEY FROM BQT1.SmallA", EXAMPLE_BQT)); //$NON-NLS-1$
		request.setRequestID(new RequestID(requestid));
		request.setConnectorID(new ConnectorID("testing")); //$NON-NLS-1$
		request.setFetchSize(5);
		return request;
	}

	public void testProcedureBatching() throws Exception {
		ProcedureExecution exec = new FakeProcedureExecution(2);

		// this has two result set columns and 1 out parameter
		int total_columns = 3;
		Command command = helpGetCommand("{call pm2.spTest8(?)}", EXAMPLE_BQT); //$NON-NLS-1$      
		IProcedure proc = (IProcedure) new LanguageBridgeFactory(EXAMPLE_BQT)
				.translate(command);

		ProcedureBatchHandler pbh = new ProcedureBatchHandler(proc, exec);

		assertEquals(total_columns, pbh.padRow(Arrays.asList(null, null)).size());

		List params = pbh.getOutputRow();
		
		// check the parameter value
		assertEquals(new Integer(3), params.get(2));

		try {
			pbh.padRow(Arrays.asList(1));
			fail("Expected exception from resultset mismatch"); //$NON-NLS-1$
		} catch (ConnectorException err) {
			assertEquals(
					"Could not process stored procedure results for EXEC spTest8(, ?).  Expected 2 result set columns, but was 1.  Please update your models to allow for stored procedure results batching.", err.getMessage()); //$NON-NLS-1$
		}
	}

	public void testCancelBeforeNew() throws Exception {
		AtomicRequestMessage request = createNewAtomicRequestMessage(1, 1);
		// only one response is expected
		ResultsFuture<AtomicResultsMessage> resultsFuture = new ResultsFuture<AtomicResultsMessage>();
		ConnectorWorkItem state = new SynchConnectorWorkItem(request,
				getConnectorManager(), resultsFuture
						.getResultsReceiver());

		state.asynchCancel(); // cancel does not cause close, but the next
								// processing will close
		assertFalse(state.isDoneProcessing());

		state.run();

		AtomicResultsMessage arm = resultsFuture.get(1000,
				TimeUnit.MILLISECONDS);

		assertTrue(arm.isRequestClosed());

		/*
		 * subsequent requests result in errors
		 */
		try {
			state.requestMore();
		} catch (IllegalStateException e) {

		}
	}

	private final class AsynchMoreResultsReceiver implements
			ResultsReceiver<AtomicResultsMessage> {
		private final ConnectorManager manager;
		int msgCount;
		ConnectorWorkItem workItem;
		Throwable exception;

		private AsynchMoreResultsReceiver(ConnectorManager manager) {
			this.manager = manager;
		}

		public void receiveResults(AtomicResultsMessage results) {
			switch (msgCount++) {
			case 0:
				// request more during delivery
				((FakeConnector) manager.getConnector().getActualConnector()).setReturnsFinalBatch(true);
				workItem.requestMore();
				break;
			case 1:
				if (results.isRequestClosed()) {
					exception = new AssertionError("request should not yet be closed"); //$NON-NLS-1$
				}
				break;
			case 2:
				if (!results.isRequestClosed()) {
					exception = new AssertionError("request be closed"); //$NON-NLS-1$
				}
				break;
			default:
				exception = new AssertionError("expected only 3 responses"); //$NON-NLS-1$
			}
		}

		public void exceptionOccurred(Throwable e) {
			exception = e;
		}
	}

	 final static class QueueResultsReceiver implements
			ResultsReceiver<AtomicResultsMessage> {
		LinkedBlockingQueue<AtomicResultsMessage> results = new LinkedBlockingQueue<AtomicResultsMessage>();
		Throwable exception;

		public QueueResultsReceiver() {
			
		}

		public void receiveResults(AtomicResultsMessage results) {
			this.results.add(results);
		}
		
		public LinkedBlockingQueue<AtomicResultsMessage> getResults() {
			return results;
		}

		public void exceptionOccurred(Throwable e) {
			exception = e;
		}
	}

	public void testMoreAsynch() throws Throwable {
		AtomicRequestMessage request = createNewAtomicRequestMessage(1, 1);
		final ConnectorManager manager = getConnectorManager();
		AsynchMoreResultsReceiver receiver = new AsynchMoreResultsReceiver(
				manager);
		ConnectorWorkItem state = new SynchConnectorWorkItem(request, manager,
				receiver);
		receiver.workItem = state;
		Thread t = runRequest(state);
		t.join(0);
		assertFalse(t.isAlive());
		if (receiver.exception != null) {
			throw receiver.exception;
		}
	}
	
	public void testSynchInterrupt() throws Exception {
		AtomicRequestMessage request = createNewAtomicRequestMessage(1, 1);
		final ConnectorManager manager = getConnectorManager();
		QueueResultsReceiver receiver = new QueueResultsReceiver();
		ConnectorWorkItem state = new SynchConnectorWorkItem(request, manager, receiver);
		Thread t = runRequest(state);
		t.interrupt();
		t.join();
		assertTrue(state.isCancelled());
	}

	public void testImplicitClose() throws Exception {
		AtomicRequestMessage request = createNewAtomicRequestMessage(1, 1);
		ConnectorManager manager = getConnectorManager();
		FakeConnector connector = (FakeConnector) manager.getConnector().getActualConnector();

		connector.setReturnsFinalBatch(true);

		ConnectorWorkItem state = new SynchConnectorWorkItem(request, manager,
				new QueueResultsReceiver());

		state.run();
		assertTrue(state.isDoneProcessing());
	}

	public void testCloseBeforeNew() throws Exception {
		AtomicRequestMessage request = createNewAtomicRequestMessage(1, 1);
		ResultsFuture<AtomicResultsMessage> resultsFuture = new ResultsFuture<AtomicResultsMessage>();
		ConnectorWorkItem state = new SynchConnectorWorkItem(request,
				getConnectorManager(), resultsFuture.getResultsReceiver());

		state.requestClose();
		assertFalse(resultsFuture.isDone());
		state.run();

		AtomicResultsMessage arm = resultsFuture.get(1000,
				TimeUnit.MILLISECONDS);
		assertTrue(arm.isRequestClosed());
		assertTrue(state.isDoneProcessing());
	}

	public void testAsynchBasicMore() throws Exception {
		AtomicRequestMessage request = createNewAtomicRequestMessage(1, 1);
		ConnectorManager manager = getConnectorManager();
		FakeConnector connector = (FakeConnector) manager.getConnector().getActualConnector();
		QueueResultsReceiver resultsReceiver = new QueueResultsReceiver(); 
		FakeQueuingAsynchConnectorWorkItem state = new FakeQueuingAsynchConnectorWorkItem(
				request, manager, resultsReceiver);

		state.run();

		assertFalse(state.isDoneProcessing());
		connector.setReturnsFinalBatch(true);

		state.requestMore();
		state.run();

		assertTrue(state.isDoneProcessing());

		assertEquals(3, resultsReceiver.results.size());
		assertEquals(1, state.resumeCount);
	}

	public void testAsynchKeepAlive() throws Exception {
		AtomicRequestMessage request = createNewAtomicRequestMessage(1, 1);
		ConnectorManager manager = getConnectorManager();
		FakeConnector connector = (FakeConnector) manager.getConnector().getActualConnector();
		QueueResultsReceiver resultsReceiver = new QueueResultsReceiver();
		FakeQueuingAsynchConnectorWorkItem state = new FakeQueuingAsynchConnectorWorkItem(
				request, manager, resultsReceiver);

		state.run();

		assertFalse(state.isDoneProcessing());

		connector.setReturnsFinalBatch(true);
		state.securityContext.keepExecutionAlive(true);

		state.requestMore();
		state.run();

		assertFalse(state.isDoneProcessing());

		assertEquals(2, resultsReceiver.results.size());
		assertEquals(1, state.resumeCount);
	}
	
	public void testUpdateExecution() throws Throwable {
		QueueResultsReceiver receiver = helpExecuteUpdate();
		AtomicResultsMessage results = receiver.getResults().remove();
		assertEquals(Integer.valueOf(1), results.getResults()[0].get(0));
	}

	private QueueResultsReceiver helpExecuteUpdate() throws Exception,
			Throwable {
		Command command = helpGetCommand("update bqt1.smalla set stringkey = 1 where stringkey = 2", EXAMPLE_BQT); //$NON-NLS-1$
		AtomicRequestMessage arm = createNewAtomicRequestMessage(1, 1);
		arm.setCommand(command);
		QueueResultsReceiver receiver = new QueueResultsReceiver();
		SynchConnectorWorkItem synchConnectorWorkItem = new SynchConnectorWorkItem(arm, getConnectorManager(), receiver);
		synchConnectorWorkItem.run();
		if (receiver.exception != null) {
			throw receiver.exception;
		}
		return receiver;
	}
	
	public void testExecutionWarning() throws Throwable {
		QueueResultsReceiver receiver = helpExecuteUpdate();
		AtomicResultsMessage results = receiver.getResults().remove();
		assertEquals(1, results.getWarnings().size());
	}

	private static class FakeQueuingAsynchConnectorWorkItem extends
			AsynchConnectorWorkItem {
		int resumeCount;

		FakeQueuingAsynchConnectorWorkItem(AtomicRequestMessage message,
				ConnectorManager manager, ResultsReceiver<AtomicResultsMessage> resultsReceiver) {
			super(message, manager, resultsReceiver);
		}

		@Override
		protected void resumeProcessing() {
			resumeCount++;
		}
	}

	private Thread runRequest(final ConnectorWorkItem state) {
		Thread t = new Thread() {
			@Override
			public void run() {
				state.run();
			}
		};
		t.start();
		return t;
	}

}
