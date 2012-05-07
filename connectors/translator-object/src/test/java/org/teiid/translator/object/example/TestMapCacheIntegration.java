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
package org.teiid.translator.object.example;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.teiid.language.Select;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.object.BaseObjectTest;
import org.teiid.translator.object.ObjectCacheConnection;
import org.teiid.translator.object.ObjectExecution;
import org.teiid.translator.object.example.MapCacheExecutionFactory;
import org.teiid.translator.object.util.VDBUtility;


@SuppressWarnings("nls")
public class TestMapCacheIntegration extends BaseObjectTest {
	
	private static boolean print = false;
	
	@Test public void testQueryIncludeLegs() throws Exception {		
		Select command = (Select)VDBUtility.TRANSLATION_UTILITY.parseCommand("select T.TradeId, T.Name as TradeName, L.Name as LegName From Trade_Object.Trade as T, Trade_Object.Leg as L Where T.TradeId = L.TradeId"); //$NON-NLS-1$

		ExecutionContext context = Mockito.mock(ExecutionContext.class);
		
		ObjectCacheConnection connection = Mockito.mock(ObjectCacheConnection.class);

		MapCacheExecutionFactory factory = new MapCacheExecutionFactory();	
		factory.setCacheLoaderClassName("org.teiid.translator.object.util.TradesCacheSource");
		factory.start();
				
		ObjectExecution exec = (ObjectExecution) factory.createExecution(command, context, VDBUtility.RUNTIME_METADATA, connection);
		
		exec.execute();
		
		int cnt = 0;
		List<Object> row = exec.next();

//		BaseObjectTest.compareResultSet("testQueryIncludeLegs", row);
		
		while (row != null) {
			++cnt;
			row = exec.next();
			printRow(cnt, row);
		}
		

		assertEquals("Did not get expected number of rows", 30, cnt); //$NON-NLS-1$
		     
		exec.close();
		
	}	
	
	@Ignore
	@Test public void testQueryGetTrades() throws Exception {		
		WRITE_ACTUAL_RESULTS_TO_FILE = true;
		Select command = (Select)VDBUtility.TRANSLATION_UTILITY.parseCommand("select * From Trade_Object.Trade"); //$NON-NLS-1$

		ExecutionContext context = Mockito.mock(ExecutionContext.class);
		ObjectCacheConnection connection = Mockito.mock(ObjectCacheConnection.class);
		
		MapCacheExecutionFactory factory = new MapCacheExecutionFactory();	
		factory.setCacheLoaderClassName("org.teiid.translator.object.testdata.TradesCacheSource");
		factory.start();
				
		ObjectExecution exec = (ObjectExecution) factory.createExecution(command, context, VDBUtility.RUNTIME_METADATA, connection);
		
		exec.execute();
		
		int cnt = 0;
		List<Object> row = exec.next();

//		BaseObjectTest.compareResultSet("testQueryIncludeLegs", row);
		
		while (row != null) {
			++cnt;
			row = exec.next();
			printRow(cnt, row);
		}
		

		assertEquals("Did not get expected number of rows", 30, cnt); //$NON-NLS-1$
		     
		exec.close();
		
	}		
	
	@Ignore
	@Test public void testQueryGetTransaction() throws Exception {		
		WRITE_ACTUAL_RESULTS_TO_FILE = true;
		Select command = (Select)VDBUtility.TRANSLATION_UTILITY.parseCommand("select * From Trade_Object.Transaction"); //$NON-NLS-1$

		ExecutionContext context = Mockito.mock(ExecutionContext.class);
		ObjectCacheConnection connection = Mockito.mock(ObjectCacheConnection.class);

		MapCacheExecutionFactory factory = new MapCacheExecutionFactory();	
		factory.setCacheLoaderClassName("org.teiid.translator.object.testdata.TradesCacheSource");
		factory.start();
				
		ObjectExecution exec = (ObjectExecution) factory.createExecution(command, context, VDBUtility.RUNTIME_METADATA, connection);
		
		exec.execute();
		
		int cnt = 0;
		List<Object> row = exec.next();

//		BaseObjectTest.compareResultSet("testQueryIncludeLegs", row);
		
		while (row != null) {
			++cnt;
			row = exec.next();
			printRow(cnt, row);
		}
		

		assertEquals("Did not get expected number of rows", 50, cnt); //$NON-NLS-1$
		     
		exec.close();
		
	}		
	
	private void printRow(int rownum, List<?> row) {
		if (!print) return;
		if (row == null) {
			System.out.println("Row " + rownum + " is null");
			return;
		}
		int i = 0;
		for(Object o:row) {
			System.out.println("Row " + rownum + " Col " + i + " - " + o.toString());
			++i;
		}
		
	}
  
}
