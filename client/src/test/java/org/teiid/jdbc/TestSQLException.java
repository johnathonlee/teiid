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

package org.teiid.jdbc;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.SQLException;

import org.junit.Test;
import org.teiid.client.ProcedureErrorInstructionException;
import org.teiid.core.TeiidException;
import org.teiid.core.TeiidProcessingException;
import org.teiid.core.TeiidRuntimeException;
import org.teiid.net.CommunicationException;
import org.teiid.net.ConnectionException;


public class TestSQLException {
  
	/*
	 * Test method for 'com.metamatrix.jdbc.MMSQLException.MMSQLException()'
	 */
	@Test public void testMMSQLException() {
		TeiidSQLException e = new TeiidSQLException();
		String sqlState = e.getSQLState();
		Throwable cause = e.getCause();
		int errorCode = e.getErrorCode();
		Throwable nestedException = e.getCause();
		SQLException nextException = e.getNextException();

		assertTrue(
				"Expected MMSQLException.getSQLState() to return <null> but got \"" //$NON-NLS-1$
						+ sqlState + "\" instead.", sqlState == null); //$NON-NLS-1$
		assertTrue(
				"Expected MMSQLException.getCause() to return <null> but got [" //$NON-NLS-1$
						+ (cause != null ? cause.getClass().getName()
								: "<unknown>") + "] instead.", cause == null); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(
				"Expected MMSQLException.getErrorCode() to return [0] but got [" //$NON-NLS-1$
						+ errorCode + "] instead.", errorCode == 0); //$NON-NLS-1$
		assertTrue(
				"Expected MMSQLException.getNestedException() to return <null> but got [" //$NON-NLS-1$
						+ (nestedException != null ? nestedException.getClass()
								.getName() : "<unknown>") + "] instead.", //$NON-NLS-1$ //$NON-NLS-2$
				nestedException == null);
		assertTrue(
				"Expected MMSQLException.getNextException() to return <null> but got a SQLException with message \"" //$NON-NLS-1$
						+ (nextException != null ? nextException.getMessage()
								: "") + "\" instead.", nextException == null); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * Test method for 'com.metamatrix.jdbc.MMSQLException.create(Throwable)'
	 * 
	 * Tests various simple exceptions to see if the expected SQLState is
	 * returend.
	 */
	@Test public void testCreateThrowable_01() {
		testCreateThrowable(new CommunicationException(
				"A test MM Communication Exception"), //$NON-NLS-1$
				SQLStates.CONNECTION_EXCEPTION_STALE_CONNECTION);
		testCreateThrowable(
				new ConnectException("A test connection attempt exception"), //$NON-NLS-1$
				SQLStates.CONNECTION_EXCEPTION_SQLCLIENT_UNABLE_TO_ESTABLISH_SQLCONNECTION);
		testCreateThrowable(
				new ConnectionException("A test MM Connection Exception"), //$NON-NLS-1$
				SQLStates.CONNECTION_EXCEPTION_SQLCLIENT_UNABLE_TO_ESTABLISH_SQLCONNECTION);
		testCreateThrowable(new IOException(
				"A test Generic java.io.IOException"), //$NON-NLS-1$
				SQLStates.CONNECTION_EXCEPTION_STALE_CONNECTION);
		testCreateThrowable(
				new MalformedURLException(
						"A test java.net.MalformedURLException"), //$NON-NLS-1$
				SQLStates.CONNECTION_EXCEPTION_SQLCLIENT_UNABLE_TO_ESTABLISH_SQLCONNECTION);
		testCreateThrowable(new TeiidException(
				"A test Generic MM Core Exception"), SQLStates.DEFAULT); //$NON-NLS-1$
		testCreateThrowable(new TeiidException("A test MM Exception"), //$NON-NLS-1$
				SQLStates.DEFAULT);
		testCreateThrowable(new TeiidProcessingException(
				"A test Generic MM Query Processing Exception"), //$NON-NLS-1$
				SQLStates.USAGE_ERROR);
		testCreateThrowable(new TeiidRuntimeException(
				"A test MM Runtime Exception"), SQLStates.DEFAULT); //$NON-NLS-1$
		testCreateThrowable(new TeiidSQLException(
				"A test Generic MM SQL Exception"), SQLStates.DEFAULT); //$NON-NLS-1$
		testCreateThrowable(
				new NoRouteToHostException(
						"A test java.net.NoRouteToHostException"), //$NON-NLS-1$
				SQLStates.CONNECTION_EXCEPTION_SQLCLIENT_UNABLE_TO_ESTABLISH_SQLCONNECTION);
		testCreateThrowable(new NullPointerException("A test NPE"), //$NON-NLS-1$
				SQLStates.DEFAULT);
		testCreateThrowable(new ProcedureErrorInstructionException(
				"A test SQL Procedure Error exception"), //$NON-NLS-1$
				SQLStates.VIRTUAL_PROCEDURE_ERROR);
		testCreateThrowable(new SocketTimeoutException(
				"A test socket timeout exception"), //$NON-NLS-1$
				SQLStates.CONNECTION_EXCEPTION_STALE_CONNECTION);
		testCreateThrowable(
				new UnknownHostException("A test connection attempt exception"), //$NON-NLS-1$
				SQLStates.CONNECTION_EXCEPTION_SQLCLIENT_UNABLE_TO_ESTABLISH_SQLCONNECTION);
	}

	/*
	 * Test method for 'com.metamatrix.jdbc.MMSQLException.create(Throwable)'
	 * 
	 * Tests various nested exceptions to see if the expected SQLState is
	 * returend.
	 */
	@Test public void testCreateThrowable_02() {
		testCreateThrowable(
				new CommunicationException(new ConnectException(
						"A test java.net.ConnectException"), //$NON-NLS-1$
						"Test Communication Exception with a ConnectException in it"), //$NON-NLS-1$
				SQLStates.CONNECTION_EXCEPTION_SQLCLIENT_UNABLE_TO_ESTABLISH_SQLCONNECTION);
		testCreateThrowable(new CommunicationException(new SocketException(
				"A test java.net.SocketException"), //$NON-NLS-1$
				"Test Communication Exception with a SocketException in it"), //$NON-NLS-1$
				SQLStates.CONNECTION_EXCEPTION_STALE_CONNECTION);
		testCreateThrowable(
				new TeiidException(new SocketTimeoutException(
						"A test java.net.SocketTimeoutException"), //$NON-NLS-1$
						"Test MetaMatrixException with a SocketTimeoutException in it"), //$NON-NLS-1$
				SQLStates.CONNECTION_EXCEPTION_STALE_CONNECTION);
	}
    
    @Test public void testCreateThrowable3() {
        TeiidSQLException e = testCreateThrowable(
                            new TeiidException(
                                    new TeiidRuntimeException(
                                            new SocketTimeoutException(
                                                    "A test MM Invalid Session Exception"), //$NON-NLS-1$
                                            "Test MetaMatrixRuntimeException with a InvalidSessionException in it"), //$NON-NLS-1$
                                    "Test MM Core Exception with an MM Runtime Exception in it and an InvalidSessionException nested within"), //$NON-NLS-1$
                            SQLStates.CONNECTION_EXCEPTION_STALE_CONNECTION);
        
        //test to ensure that wrapping mmsqlexceptions works
        TeiidSQLException e1 = TeiidSQLException.create(e, "new message"); //$NON-NLS-1$
        assertEquals("new message", e1.getMessage()); //$NON-NLS-1$
        testCreateThrowable(((TeiidSQLException)e1.getCause()).getCause(), SQLStates.CONNECTION_EXCEPTION_STALE_CONNECTION);
    }

	/*
	 * Helper method to test SQLState and general MMSQLException validation
	 */
	private TeiidSQLException testCreateThrowable(Throwable ecause, String esqlState) {
		TeiidSQLException e = TeiidSQLException.create(ecause);
		if (ecause.getClass() == TeiidSQLException.class) {
            ecause = null;
		}
		String sqlState = e.getSQLState();
		Throwable cause = e.getCause();
		int errorCode = e.getErrorCode();
		Throwable nestedException = e.getCause();
		SQLException nextException = e.getNextException();

		assertEquals(esqlState, sqlState);
		assertEquals(ecause, cause);
		assertEquals(0, errorCode);
		assertEquals(nestedException, ecause);
		assertNull(nextException);
		return e;
    }
    
    @Test public void testCreate() {
        TeiidSQLException exception = TeiidSQLException.create(new Exception());
        
        assertEquals(exception.getMessage(), Exception.class.getName());
        assertNotNull(exception.getSQLState());
        assertEquals(exception.getSQLState(), "38000"); //$NON-NLS-1$
        
        assertEquals(exception, TeiidSQLException.create(exception));
    }
    
    @Test public void testCreateFromSQLException() {
        SQLException sqlexception = new SQLException("foo", "21"); //$NON-NLS-1$ //$NON-NLS-2$
        
        SQLException nested = new SQLException("bar"); //$NON-NLS-1$
        
        sqlexception.setNextException(nested);
        
        String message = "top level message"; //$NON-NLS-1$
        
        TeiidSQLException exception = TeiidSQLException.create(sqlexception, message);
        
        assertEquals(exception.getMessage(), message);
        assertEquals(exception.getSQLState(), sqlexception.getSQLState());        
        assertEquals(exception.getNextException().getMessage(), sqlexception.getMessage());
        assertEquals(exception.getNextException().getNextException().getMessage(), nested.getMessage());
    }
    
    @Test public void testCodeAsSQLState() {
        TeiidException sqlexception = new TeiidException("foo", "21"); //$NON-NLS-1$ //$NON-NLS-2$
        
        String message = "top level message"; //$NON-NLS-1$
        
        TeiidSQLException exception = TeiidSQLException.create(sqlexception, message);
        
        assertEquals(sqlexception.getCode(), exception.getSQLState());        
    }

}
