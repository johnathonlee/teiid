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

package com.metamatrix.jdbc;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import com.metamatrix.api.exception.MetaMatrixProcessingException;
import com.metamatrix.common.types.MMJDBCSQLTypeInfo;
import com.metamatrix.common.util.TimestampWithTimezone;
import com.metamatrix.dqp.client.ClientSideDQP;
import com.metamatrix.dqp.client.ResultsFuture;
import com.metamatrix.dqp.message.RequestMessage;
import com.metamatrix.dqp.message.ResultsMessage;
import com.metamatrix.query.unittest.TimestampUtil;

public class TestAllResultsImpl {

	private static final long REQUEST_ID = 0;
	private static final int TYPE_FORWARD_ONLY = ResultSet.TYPE_FORWARD_ONLY;
	private static final int TYPE_SCROLL_SENSITIVE = ResultSet.TYPE_SCROLL_SENSITIVE;

	private MMStatement statement;

	@Before public void setUp() throws Exception {
		statement = TestMMResultSet.createMockStatement(TYPE_SCROLL_SENSITIVE);
	}
	
	/** test hasNext(), actual result set should return FALSE. */
	@Test public void testHasNext1() throws Exception {
		MMResultSet rs = new MMResultSet(exampleResultsMsg1(),
				statement);
		while (rs.next()) {
			// just walk through
		}

		boolean actual = rs.hasNext();
		boolean expected = false;
		assertEquals(expected, actual); 

		rs.close();
	}

	/** test hasNext(), actual result set should return TRUE. */
	@Test public void testHasNext2() throws Exception {
		List[] results = exampleResults1(5);
		MMResultSet rs = new MMResultSet(exampleResultsMsg1(),
				statement);

		for (int i = 1; i < results.length; i++) {
			rs.next();
		}

		boolean actual = rs.hasNext();
		boolean expected = true;
		assertEquals(expected, actual); 

		rs.close();
	}

	/**
	 * test next(), whether the result set's cursor is positioned on next row or
	 * not
	 */
	@Test public void testNext1() throws Exception {
		MMResultSet rs = new MMResultSet(exampleResultsMsg1(),
				statement);

		// point to first row
		boolean actual = rs.next();
		boolean expected = true;
		assertEquals(" Actual doesn't match with expected. ", expected, actual); //$NON-NLS-1$

		rs.close();
	}

	/** test next(), walk through all rows of a result set and compare each row. */
	@Test public void testNext2() throws Exception {
		List[] results = exampleResults1(5);
		MMResultSet rs = new MMResultSet(exampleResultsMsg1(),
				statement);

		int i = 0;
		while (rs.next()) {
			// walk through and compare
			List actual = rs.getCurrentRecord();
			List expected = results[i];
			assertEquals(expected, actual); 
			i++;
		}

		rs.close();
	}

	/** test next(), get result set and close without walking through */
	@Test public void testNext3() throws Exception {
		MMResultSet rs = new MMResultSet(exampleResultsMsg1(),
				statement);
		assertEquals(new Integer(0), new Integer(rs.getRow())); 

		rs.close();
	}

	/** test next(), walk through partial rows of a result set */
	@Test public void testNext4() throws Exception {
		List[] results = exampleResults1(5);
		MMResultSet rs = new MMResultSet(exampleResultsMsg1(),
				statement);

		for (int i = 0; i < results.length - 1; i++) {
			rs.next();
			List actual = rs.getCurrentRecord();
			List expected = results[i];
			assertEquals(expected, actual); 
		}

		rs.close();
	}

	/** test next(), when hasNext() == false */
	@Test public void testNext5() throws Exception {
		MMResultSet rs = new MMResultSet(exampleResultsMsg1(),
				statement);
		while (rs.next()) {
			// just walk through until hasNext() == false;
		}

		boolean actual = rs.hasNext();
		boolean expected = false;
		assertEquals(expected, actual); 

		rs.close();
	}

	/** test getObject() at columnIndex = 2 of 5th row */
	@Test public void testGetObject1() throws Exception {
		List[] results = exampleResults2();
		MMResultSet rs = new MMResultSet(exampleResultsMsg2a(),
				statement);

		String actual = null;
		String expected = "a3"; //$NON-NLS-1$

		// move cursor to the 4th row
		for (int i = 0; i < results.length - 2; i++) {
			rs.next();
		}

		// only compare the 4th row's 2nd column
		if (rs.next()) {
			actual = (String) rs.getObject(2);
		}

		assertEquals(expected, actual); 

		rs.close();
	}

	/** Should fail, test getObject() at wrong columnIndex */
	@Test public void testGetObject2() throws Exception {
		MMResultSet rs = new MMResultSet(exampleResultsMsg2a(),
				statement);

		if (rs.next()) {
			// ERROR -- there are totally only 2 columns inside result set, 6 is
			// an invalid one
			try {
				rs.getObject(6);
			} catch (Exception e) {
				if (e instanceof IllegalArgumentException) {
					// OK
				}
			}
		}

		rs.close();
	}

	@Test public void testGetRow() throws Exception {
		MMResultSet rs = new MMResultSet(exampleResultsMsg2(),
				statement);

		int expected = 0;
		assertEquals(expected, rs.getRow()); 

		if (rs.next()) {
			expected = 1;
			assertEquals(expected, rs.getRow()); 
		}
		rs.close();

	}

	@Test public void testPrevious() throws Exception {
		List[] results = exampleResults1(5);
		MMResultSet rs = new MMResultSet(exampleResultsMsg1(),
				statement);

		while (rs.next()) {
			// just walk to the end;
		}

		// walk reversely;
		int i = results.length - 1;
		while (rs.previous()) {
			List expected = new ArrayList();
			expected.add(new Integer(i + 1));
			assertEquals(expected, rs.getCurrentRecord()); 
			i--;
		}

		rs.close();
	}

	@Test public void testGetCurrentRecord() throws Exception {
		List[] results = exampleResults2();
		MMResultSet rs = new MMResultSet(exampleResultsMsg2(),
				statement);

		rs.next();
		List actual = rs.getCurrentRecord();
		assertEquals(results[0], actual); 
		rs.close();
	}

	@Test public void testGetMetaData() throws Exception {
		MMResultSet rs = new MMResultSet(exampleResultsMsg2a(),
				statement);
		ResultSetMetaData rmetadata = rs.getMetaData();
		assertEquals(2, rmetadata.getColumnCount()); 

		String[] columnNames = columnNames();
		String[] dataTypes = dataTypes();
		for (int i = 0; i < 2; i++) {
			assertEquals(columnNames[i], rmetadata.getColumnName(i + 1)); 
			assertEquals(dataTypes[i], rmetadata.getColumnTypeName(i + 1)); 
		}
		rs.close();
	}

	@Test public void testResultsWarnings() throws Exception {
		MMResultSet rs = new MMResultSet(exampleResultsMsg2(),
				statement);
		rs.close();
	}

	@Test public void testClose() throws Exception {
		MMResultSet rs = new MMResultSet(exampleResultsMsg2(),
				statement);
		rs.close();
		verify(statement, times(0)).close();
	}

	@Test public void testGetFetchSize() throws Exception {
		MMStatement s = mock(MMStatement.class);
		stub(s.getFetchSize()).toReturn(500);
		MMResultSet rs = new MMResultSet(exampleResultsMsg2(), s);
		assertEquals(500, rs.getFetchSize()); 
		rs.setFetchSize(100);
		assertEquals(100, rs.getFetchSize());
	}

	// //////////////////////Functions refer to ResultSet's TYPE_FORWARD_ONLY///
	// /////////////////
	@Test(expected=SQLException.class) public void testIsAfterLast1() throws Exception {
		MMResultSet rs = helpGetResultSetImpl(TYPE_FORWARD_ONLY);
		rs.last();
	}

	@Test(expected=SQLException.class) public void testAfterLast1() throws Exception {
		MMResultSet rs = helpGetResultSetImpl(TYPE_FORWARD_ONLY);
		rs.afterLast();
	}

	@Test public void testIsBeforeFirst1() throws Exception {
		MMResultSet rs = helpGetResultSetImpl(TYPE_FORWARD_ONLY);

		// right before the first row
		boolean actual = rs.isBeforeFirst();
		assertEquals(true, actual); 
		rs.close();
	}

	@Test public void testIsBeforeFirst2() throws Exception {
		MMResultSet rs = helpGetNoResults(TYPE_FORWARD_ONLY);

		// right before the first row
		boolean actual = rs.isBeforeFirst();
		assertEquals(false, actual); 
		rs.close();
	}

	@Test(expected=SQLException.class) public void testBeforeFirst1() throws Exception {
		MMResultSet rs = helpGetResultSetImpl(TYPE_FORWARD_ONLY);

		// move cursor to the first row
		rs.next();

		// move back to before first row
		rs.beforeFirst();
	}

	@Test public void testIsFirst1() throws Exception {
		MMResultSet rs = helpGetResultSetImpl(TYPE_FORWARD_ONLY);

		// move cursor to the first row
		rs.next();
		boolean actual = rs.isFirst();
		assertEquals(true, actual); 
		rs.close();
	}

	@Test public void testIsFirst2() throws Exception {
		MMResultSet rs = helpGetNoResults(TYPE_FORWARD_ONLY);

		// move cursor to the first row
		rs.next();
		boolean actual = rs.isFirst();
		assertEquals(false, actual); 
		rs.close();
	}

	@Test(expected=SQLException.class) public void testFirst1() throws Exception {
		MMResultSet rs = helpGetResultSetImpl(TYPE_FORWARD_ONLY);

		// move cursor to the first row
		rs.next();
		rs.first();
	}

	@Test(expected=SQLException.class) public void testFirst2() throws Exception {
		MMResultSet rs = helpGetNoResults(TYPE_FORWARD_ONLY);

		// move cursor to the first row
		rs.next();
		rs.first();
	}

	@Test public void testFindColumn() throws Exception {
		MMResultSet rs = new MMResultSet(exampleResultsMsg2a(),
				statement);

		assertEquals(1, rs.findColumn("IntNum")); //$NON-NLS-1$ 
		rs.close();
	}

	@Test public void testIsLast1() throws Exception {
		MMResultSet rs = helpGetResultSetImpl(TYPE_FORWARD_ONLY);

		// move cursor to the last row
		boolean actual = rs.isLast();
		assertEquals(false, actual); 
	}

	@Test public void testIsLast2() throws Exception {
		MMResultSet rs = helpGetNoResults(TYPE_FORWARD_ONLY);

		// move cursor to the last row
		boolean actual = rs.isLast();
		assertEquals(false, actual); 
	}

	@Test(expected=SQLException.class) public void testLast1() throws Exception {
		MMResultSet rs = helpGetResultSetImpl(TYPE_FORWARD_ONLY);

		rs.last();
	}

	@Test public void testRelative1() throws Exception {
		MMResultSet rs = new MMResultSet(exampleResultsMsg2(),
				statement);

		// move to 1st row
		rs.next();
		// move to 2nd row
		boolean actual = rs.relative(1);
		assertEquals(true, actual); 
		assertEquals(2, rs.getRow()); 

		actual = rs.relative(-1);
		assertEquals(true, actual); 
		assertEquals(1, rs.getRow()); 
		rs.close();
	}

	@Test(expected=SQLException.class) public void testAbsolute1() throws Exception {
		MMResultSet rs = helpGetResultSetImpl(TYPE_FORWARD_ONLY);

		rs.absolute(1);
	}

	// //////////Functions refer to other types other than ResultSet's
	// TYPE_FORWARD_ONLY//////

	@Test public void testAfterLast1a() throws Exception {
		MMResultSet rs = helpGetResultSetImpl(TYPE_SCROLL_SENSITIVE);

		// move cursor right past the last row
		rs.afterLast();

		// the expected row == 0 because it pasts the last row
		assertEquals(0, rs.getRow()); 
		rs.close();
	}

	@Test public void testIsAfterLast1a() throws Exception {
		MMResultSet rs = helpGetResultSetImpl(TYPE_SCROLL_SENSITIVE);

		// the last row
		rs.last();
		boolean actual = rs.isAfterLast();
		assertEquals(false, actual); 

		// move after the last row
		rs.next();
		actual = rs.isAfterLast();
		assertEquals(true, actual); 
		rs.close();
	}

	@Test public void testIsBeforeFirst1a() throws Exception {
		MMResultSet rs = helpGetResultSetImpl(TYPE_SCROLL_SENSITIVE);

		// right before the first row
		boolean actual = rs.isBeforeFirst();
		assertEquals(true, actual); 
		rs.close();
	}

	@Test public void testBeforeFirst1a() throws Exception {
		MMResultSet rs = helpGetResultSetImpl(TYPE_SCROLL_SENSITIVE);

		// move cursor to the first row
		rs.next();
		rs.next();

		// move back to before first row
		rs.beforeFirst();

		assertEquals(0, rs.getRow()); 
		rs.close();
	}

	@Test public void testIsFirst1a() throws Exception {
		MMResultSet rs = helpGetResultSetImpl(TYPE_SCROLL_SENSITIVE);

		// move cursor to the first row
		rs.next();
		boolean actual = rs.isFirst();
		assertEquals(true, actual); 

		// check row number
		assertEquals(1, rs.getRow()); 
		rs.close();
	}

	@Test public void testFirst1a() throws Exception {
		MMResultSet rs = helpGetResultSetImpl(TYPE_SCROLL_SENSITIVE);

		// move cursor to the first row
		boolean actual = rs.first();
		assertEquals(true, actual); 
		assertEquals(1, rs.getRow()); 

		// move cursor to the first row starting from the last row
		rs.afterLast();
		actual = rs.first();
		assertEquals(true, actual); 
		assertEquals(1, rs.getRow()); 

		// move cursor to the first row from random number;
		rs.absolute(3);
		actual = rs.first();
		assertEquals(true, actual); 
		assertEquals(1, rs.getRow()); 
		rs.close();
	}

	@Test public void testIsLast1a() throws Exception {
		MMResultSet rs = helpGetResultSetImpl(TYPE_SCROLL_SENSITIVE);

		// check whether the movement of cursor is successful
		rs.last();
		boolean actual = rs.isLast();
		assertEquals(true, actual); 

		// check row number
		assertEquals(5, rs.getRow()); 
		rs.close();
	}

	@Test public void testLast1a() throws Exception {
		MMResultSet rs = helpGetResultSetImpl(TYPE_SCROLL_SENSITIVE);

		// check whether the movement of cursor is successful
		boolean actual = rs.last();
		assertEquals(true, actual); 

		// check weather the current row is the last row
		assertEquals(5, rs.getRow()); 
		rs.close();
	}

	/** normal relative move, only including moving from valid row to valid one */
	@Test public void testRelative1a() throws Exception {
		MMResultSet rs = helpGetResultSetImpl(TYPE_SCROLL_SENSITIVE);

		// move to 1st row
		rs.next();
		// move to 2nd row
		boolean actual = rs.relative(1);

		assertEquals(true, actual); 
		assertEquals(2, rs.getRow()); 

		actual = rs.relative(-1);
		assertEquals(true, actual); 
		assertEquals(1, rs.getRow()); 
		rs.close();
	}

	/** normal relative move, including moving from valid row to invalid one */
	@Test public void testRelative1b() throws Exception {
		MMResultSet rs = helpGetResultSetImpl(TYPE_SCROLL_SENSITIVE);

		// move to 1st row
		rs.next();
		// move to 2nd row
		boolean actual = rs.relative(1);
		actual = rs.relative(-1);

		// test if move before first
		actual = rs.relative(-3);
		// relative should return false when not on a row
		assertEquals(false, actual); 
		assertEquals(0, rs.getRow()); 

		// test if move after last
		// this line is very important because it positions the cursor in a
		// valid row!!!
		rs.beforeFirst();
		rs.next();
		actual = rs.relative(7);
		// should return false because it's not on a valid row
		assertEquals(false, actual); 
		assertEquals(0, rs.getRow()); 
		rs.close();
	}

	/** check only moving from an invalid row */
	@Test public void testRelative1c() throws Exception {
		MMResultSet rs = helpGetResultSetImpl(TYPE_SCROLL_SENSITIVE);

		// test if move before first will work or not
		// default to before first
		try {
			rs.relative(-2);
			fail("relative move from an invalid row should fail"); //$NON-NLS-1$
		} catch (SQLException e) {

		}
		assertEquals(
				" Should still be before the first row ", true, rs.isBeforeFirst()); //$NON-NLS-1$
		assertEquals(0, rs.getRow()); 

		try {
			rs.relative(2);
			fail("relative move from an invalid row should fail"); //$NON-NLS-1$
		} catch (SQLException e) {

		}
		assertEquals(
				" Should still be before the first row ", true, rs.isBeforeFirst()); //$NON-NLS-1$
		assertEquals(0, rs.getRow()); 
		// test if move after last will work or not

		rs.afterLast();

		try {
			rs.relative(2);
			fail("relative move from an invalid row should fail"); //$NON-NLS-1$
		} catch (SQLException e) {

		}
		assertEquals(
				" Should still be after the last row. ", true, rs.isAfterLast()); //$NON-NLS-1$
		assertEquals(0, rs.getRow()); 
		try {
			rs.relative(-2);
			fail("relative move from an invalid row should fail"); //$NON-NLS-1$
		} catch (SQLException e) {

		}
		assertEquals(
				" Should still be after the last row. ", true, rs.isAfterLast()); //$NON-NLS-1$
		assertEquals(0, rs.getRow()); 
		rs.close();
	}

	/** test only valid row in result set */
	@Test public void testAbsolute1a() throws Exception {
		MMResultSet rs = helpGetResultSetImpl(TYPE_SCROLL_SENSITIVE);

		// start from beginning
		boolean actual = rs.absolute(1);
		assertEquals(true, actual); 
		assertEquals(1, rs.getRow()); 

		actual = rs.absolute(12);
		assertEquals(false, actual); 
		assertEquals(0, rs.getRow()); 

		// start from right after last
		rs.afterLast();
		actual = rs.absolute(-1);
		assertEquals(true, actual); 
		assertEquals(5, rs.getRow()); 

		actual = rs.absolute(-2);
		assertEquals(true, actual); 
		assertEquals(4, rs.getRow()); 
		rs.close();
	}

	/** test only valid row in result set */
	@Test public void testAbsolute2a() throws Exception {
		MMResultSet rs = helpGetNoResults(TYPE_SCROLL_SENSITIVE);

		// start from beginning
		assertEquals(false, rs.absolute(1)); 
		assertEquals(0, rs.getRow()); 

		// start from right after last
		rs.afterLast();
		assertEquals(false, rs.absolute(-1)); 
		assertEquals(0, rs.getRow()); 

		rs.close();
	}
	
	/**
	 * 3 batches
	 */
	@Test public void testMoreResults() throws Exception {
		int fetchSize = 5;
		int batchLength = 4;
		int totalLength = 10;
		
		MMResultSet rs = helpTestBatching(statement, fetchSize, batchLength,
				totalLength);
		
		assertTrue(rs.absolute(6));
		assertTrue(rs.absolute(-1));
		assertFalse(rs.next());
		
		for (int i = 0; i < totalLength; i++) {
			assertTrue(rs.previous());
		}
	}
	
	static MMResultSet helpTestBatching(MMStatement statement, int fetchSize, int batchLength,
			int totalLength) throws InterruptedException, ExecutionException,
			MetaMatrixProcessingException, SQLException, TimeoutException {
		ClientSideDQP dqp = mock(ClientSideDQP.class);
		stub(statement.getDQP()).toReturn(dqp);
		stub(statement.getFetchSize()).toReturn(fetchSize);
		for (int i = batchLength; i < totalLength; i += batchLength) {
			//forward requests
			ResultsFuture<ResultsMessage> nextBatch = mock(ResultsFuture.class);
			stub(nextBatch.get(Matchers.anyLong(), (TimeUnit)Matchers.anyObject())).toReturn(exampleResultsMsg4(i + 1, Math.min(batchLength, totalLength - i), fetchSize, i + batchLength >= totalLength));
			stub(dqp.processCursorRequest(REQUEST_ID, i + 1, fetchSize)).toReturn(nextBatch);
			
			if (i + batchLength < totalLength) {
				//backward requests
				ResultsFuture<ResultsMessage> previousBatch = mock(ResultsFuture.class);
				stub(previousBatch.get(Matchers.anyLong(), (TimeUnit)Matchers.anyObject())).toReturn(exampleResultsMsg4(i - batchLength + 1, i, fetchSize, false));
				stub(dqp.processCursorRequest(REQUEST_ID, i, fetchSize)).toReturn(previousBatch);
			}
		}
		
		ResultsMessage msg = exampleResultsMsg4(1, batchLength, fetchSize, batchLength == totalLength);
		return new MMResultSet(msg, statement);
	}

	// /////////////////////Helper Method///////////////////
	static List<Object>[] exampleResults1(int length) {
		return exampleResults1(length, 1);
	}
	
	static List<Object>[] exampleResults1(int length, int begin) {
		List<Object>[] results = new List[length];

		for (int i = 0; i < results.length; i++) {
			results[i] = new ArrayList<Object>();
			results[i].add(new Integer(begin + i));
		}

		return results;
	}

	private List[] exampleResults2() {
		List[] results = new List[5];

		for (int i = 0; i < results.length; i++) {
			results[i] = new ArrayList();
			results[i].add(new Integer(i));
			results[i].add(new String("a" + i)); //$NON-NLS-1$
		}

		return results;
	}

	private String[] columnNames() {
		String[] names = new String[2];
		names[0] = new String("IntNum"); //$NON-NLS-1$
		names[1] = new String("StringNum"); //$NON-NLS-1$
		return names;
	}

	private String[] dataTypes() {
		String[] types = new String[2];
		types[0] = MMJDBCSQLTypeInfo.INTEGER;
		types[1] = MMJDBCSQLTypeInfo.STRING;
		return types;
	}

	private MMResultSet helpGetResultSetImpl(int type)
			throws SQLException {
		ResultsMessage rsMsg = exampleResultsMsg2();
		statement = TestMMResultSet.createMockStatement(type);
		MMResultSet rs = new MMResultSet(rsMsg, statement);
		return rs;
	}

	private MMResultSet helpGetNoResults(int type) throws SQLException {
		ResultsMessage rsMsg = exampleResultsMsg3();
		statement = TestMMResultSet.createMockStatement(type);
		MMResultSet rs = new MMResultSet(rsMsg, statement);
		return rs;
	}

	/** without metadata info. */
	private ResultsMessage exampleResultsMsg1() {
		return exampleMessage(exampleResults1(5), new String[] { "IntNum" }, new String[] { MMJDBCSQLTypeInfo.INTEGER }); //$NON-NLS-1$
	}
	
	private ResultsMessage exampleMessage(List<Object>[] results, String[] columnNames, String[] datatypes) {
		RequestMessage request = new RequestMessage();
		request.setExecutionId(REQUEST_ID);
		ResultsMessage resultsMsg = new ResultsMessage(request);
		resultsMsg.setResults(results);
		resultsMsg.setColumnNames(columnNames);
		resultsMsg.setDataTypes(datatypes); 
		resultsMsg.setFinalRow(results.length);
		resultsMsg.setLastRow(results.length);
		resultsMsg.setFirstRow(1);
		return resultsMsg;
	}

	/** without metadata info. */
	private ResultsMessage exampleResultsMsg2() {
		return exampleMessage(exampleResults2(), new String[] { "IntNum", "StringNum" }, new String[] { MMJDBCSQLTypeInfo.INTEGER, MMJDBCSQLTypeInfo.STRING }); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/** with limited metadata info. */
	private ResultsMessage exampleResultsMsg2a() {
		ResultsMessage resultsMsg = exampleResultsMsg2();
		List[] results = exampleResults2();
		resultsMsg.setDataTypes(dataTypes());
		resultsMsg.setColumnNames(columnNames());

		resultsMsg.setResults(results);
		resultsMsg.setFinalRow(results.length);
		resultsMsg.setLastRow(results.length);
		resultsMsg.setFirstRow(1);
		return resultsMsg;
	}

	/** no rows. */
	private ResultsMessage exampleResultsMsg3() {
		return exampleMessage(new List[0], new String[] { "IntNum", "StringNum" }, new String[] { MMJDBCSQLTypeInfo.INTEGER, MMJDBCSQLTypeInfo.STRING }); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private static ResultsMessage exampleResultsMsg4(int begin, int length, int fetchSize, boolean lastBatch) {
		RequestMessage request = new RequestMessage();
		request.setExecutionId(REQUEST_ID);
		ResultsMessage resultsMsg = new ResultsMessage(request);
		List[] results = exampleResults1(length, begin);
		resultsMsg.setResults(results);
		resultsMsg.setColumnNames(new String[] { "IntKey" }); //$NON-NLS-1$
		resultsMsg.setDataTypes(new String[] { MMJDBCSQLTypeInfo.INTEGER }); 
		resultsMsg.setFirstRow(begin);
		if (lastBatch) {
			resultsMsg.setFinalRow(begin + results.length - 1);
		}
		resultsMsg.setLastRow(begin + results.length - 1);
		return resultsMsg;
	}

	@Test public void testNotCallingNext() throws SQLException {
		MMResultSet cs = new MMResultSet(exampleResultsMsg2a(),
				statement);

		try {
			cs.getObject(1);
			fail("Exception expected"); //$NON-NLS-1$
		} catch (SQLException e) {
			assertEquals("The cursor is not on a valid row.", e.getMessage()); //$NON-NLS-1$
		}
	}
	
	@Test public void testDateType() throws SQLException {
		RequestMessage request = new RequestMessage();
		request.setExecutionId(REQUEST_ID);
		ResultsMessage resultsMsg = new ResultsMessage(request);
		resultsMsg.setResults(new List[] {Arrays.asList(new Timestamp(0))});
		resultsMsg.setColumnNames(new String[] { "TS" }); //$NON-NLS-1$
		resultsMsg.setDataTypes(new String[] { MMJDBCSQLTypeInfo.TIMESTAMP }); 
		resultsMsg.setFirstRow(1);
		resultsMsg.setFinalRow(1);
		resultsMsg.setLastRow(1);
		MMResultSet rs = new MMResultSet(resultsMsg, statement);
		assertTrue(rs.next());
		//assumes the mock statement is setup with GMT-5 server and GMT-6 client
		
		//will adjust ahead one hour
		assertEquals(new Timestamp(3600000), rs.getObject(1));
		
		//will be the same as the original
		assertEquals(new Timestamp(0), rs.getTimestamp(1, Calendar.getInstance(TimeZone.getTimeZone("GMT-05:00")))); //$NON-NLS-1$
	}
	
	@Test public void testWasNull() throws SQLException{
		ResultsMessage message = exampleMessage(new List[] { Arrays.asList((String)null), Arrays.asList("1") }, new String[] { "string" }, //$NON-NLS-1$
				new String[] { MMJDBCSQLTypeInfo.STRING });
		MMResultSet rs = new MMResultSet(message, statement);
		assertTrue(rs.next());
		assertEquals(Boolean.FALSE.booleanValue(), rs.getBoolean(1));
		assertTrue(rs.wasNull());
		assertEquals(0, rs.getShort(1));
		assertTrue(rs.wasNull());
		assertEquals(0, rs.getInt(1));
		assertTrue(rs.wasNull());
		assertEquals(0l, rs.getLong(1));
		assertTrue(rs.wasNull());
		assertEquals(0f, rs.getFloat(1), 0);
		assertTrue(rs.wasNull());		
		assertEquals(0d, rs.getDouble(1), 0);
		assertTrue(rs.wasNull());
		assertNull(rs.getString(1));
		assertTrue(rs.wasNull());
		assertTrue(rs.next());
		assertEquals(1, rs.getShort(1));
		assertFalse(rs.wasNull());
		assertFalse(rs.next());
	}
	
	@Test public void testGetters() throws SQLException{
		TimeZone.setDefault(TimeZone.getTimeZone("GMT-05:00")); //$NON-NLS-1$
		ResultsMessage message = exampleMessage(new List[] { Arrays.asList(1, TimestampUtil.createTime(0, 0, 0), TimestampUtil.createDate(1, 1, 1), TimestampUtil.createTimestamp(1, 1, 1, 1, 1, 1, 1), "<root/>") }, //$NON-NLS-1$ 
				new String[] { "int", "time", "date", "timestamp", "sqlxml" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ 
				new String[] { MMJDBCSQLTypeInfo.INTEGER, MMJDBCSQLTypeInfo.TIME, MMJDBCSQLTypeInfo.DATE, MMJDBCSQLTypeInfo.TIMESTAMP, MMJDBCSQLTypeInfo.STRING });
		TimestampWithTimezone.resetCalendar(TimeZone.getTimeZone("GMT-06:00")); //$NON-NLS-1$
		MMResultSet rs = new MMResultSet(message, statement);
		assertTrue(rs.next());
		assertEquals(Boolean.TRUE.booleanValue(), rs.getBoolean(1));
		assertEquals(1, rs.getShort(1));
		assertEquals(1, rs.getInt(1));
		assertEquals(1l, rs.getLong(1));
		assertEquals(1f, rs.getFloat(1), 0);
		assertEquals(1d, rs.getDouble(1), 0);
		assertEquals("1", rs.getString(1)); //$NON-NLS-1$
		assertEquals(Integer.valueOf(1), rs.getObject(1)); 
		//the mock statement is in GMT-6 the server results are from GMT-5, so we expect them to display the same
		assertEquals(TimestampUtil.createTime(0, 0, 0), rs.getTime(2)); 
		assertEquals(TimestampUtil.createDate(1, 1, 1), rs.getDate(3));
		assertEquals(TimestampUtil.createTimestamp(1, 1, 1, 1, 1, 1, 1), rs.getTimestamp(4));
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><root/>", rs.getSQLXML(5).getString()); //$NON-NLS-1$
		try {
			rs.getSQLXML(1);
		} catch (SQLException e) {
			assertEquals("Unable to transform the column value 1 to a SQLXML.", e.getMessage()); //$NON-NLS-1$
		}
		assertFalse(rs.next());
		TimestampWithTimezone.resetCalendar(null);
	}

}
