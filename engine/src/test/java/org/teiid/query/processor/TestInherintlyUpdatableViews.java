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

package org.teiid.query.processor;

import static org.teiid.query.processor.TestProcessor.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.teiid.query.metadata.TransformationMetadata;
import org.teiid.query.optimizer.TestOptimizer;
import org.teiid.query.optimizer.capabilities.BasicSourceCapabilities;
import org.teiid.query.optimizer.capabilities.DefaultCapabilitiesFinder;
import org.teiid.query.rewriter.TestQueryRewriter;
import org.teiid.query.sql.lang.Command;
import org.teiid.query.util.CommandContext;
import org.teiid.query.validator.TestUpdateValidator;
import org.teiid.translator.SourceSystemFunctions;

@SuppressWarnings("nls")
public class TestInherintlyUpdatableViews {

	@Test public void testUpdatePassThrough() throws Exception {
		String userSql = "update vm1.gx set e1 = e2"; //$NON-NLS-1$
    	String viewSql = "select * from pm1.g1 where e3 < 5";
    	String expectedSql = "UPDATE pm1.g1 SET e1 = pm1.g1.e2 WHERE e3 < 5";
        helpTest(userSql, viewSql, expectedSql, null);	
	}

	private void helpTest(String userSql, String viewSql, String expectedSql, ProcessorDataManager dm)
			throws Exception {
		TransformationMetadata metadata = TestUpdateValidator.example1();
        TestUpdateValidator.createView(viewSql, metadata, "gx");
        Command command = TestQueryRewriter.helpTestRewriteCommand(userSql, expectedSql, metadata);

        if (dm != null) {
        	CommandContext context = createCommandContext();
	        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities();
	        caps.setFunctionSupport(SourceSystemFunctions.CONVERT, true);
	        ProcessorPlan plan = helpGetPlan(command, metadata, new DefaultCapabilitiesFinder(caps), context);
	        List[] expected = new List[] {Arrays.asList(1)};
        	helpProcess(plan, context, dm, expected);
        }
	}
	
	@Test public void testUpdatePassThroughWithAlias() throws Exception {
		String userSql = "update vm1.gx set e1 = e2"; //$NON-NLS-1$
    	String viewSql = "select * from pm1.g1 as x where e3 < 5";
    	String expectedSql = "UPDATE pm1.g1 SET e1 = pm1.g1.e2 WHERE e3 < 5";
        helpTest(userSql, viewSql, expectedSql, null);	
	}
	
	@Test public void testDeletePassThrough() throws Exception {
		String userSql = "delete from vm1.gx where e1 = e2"; //$NON-NLS-1$
    	String viewSql = "select * from pm1.g1 where e3 < 5";
        String expectedSql = "DELETE FROM pm1.g1 WHERE (pm1.g1.e1 = pm1.g1.e2) AND (e3 < 5)";
        helpTest(userSql, viewSql, expectedSql, null);
	}
	
	@Test public void testInsertPassThrough() throws Exception {
		String userSql = "insert into vm1.gx (e1) values (1)"; //$NON-NLS-1$
    	String viewSql = "select * from pm1.g1 where e3 < 5";
        String expectedSql = "INSERT INTO pm1.g1 (pm1.g1.e1) VALUES ('1')";
        helpTest(userSql, viewSql, expectedSql, null);
	}
	
	/**
	 * Here we should be able to figure out that we can pass through the join
	 * @throws Exception
	 */
	@Test public void testInsertPassThrough1() throws Exception {
		String userSql = "insert into vm1.gx (e1) values (1)"; //$NON-NLS-1$
    	String viewSql = "select g2.* from pm1.g1 inner join pm1.g2 on g1.e1 = g2.e1";
        String expectedSql = "INSERT INTO pm1.g2 (pm1.g2.e1) VALUES ('1')";
        helpTest(userSql, viewSql, expectedSql, null);	
	}
	
	@Test public void testUpdateComplex() throws Exception {
		String userSql = "update vm1.gx set e1 = e2 where e3 is null"; //$NON-NLS-1$
		String viewSql = "select g2.* from pm1.g1 inner join pm1.g2 on g1.e1 = g2.e1";
		
		HardcodedDataManager dm = new HardcodedDataManager();
        dm.addData("SELECT g_1.e2, g_1.e2 FROM pm1.g1 AS g_0, pm1.g2 AS g_1 WHERE (g_0.e1 = g_1.e1) AND (g_1.e3 IS NULL)", new List[] {Arrays.asList(1, 1)});
        dm.addData("UPDATE pm1.g2 SET e1 = pm1.g2.e2 WHERE pm1.g2.e2 = 1", new List[] {Arrays.asList(1)});
        
		helpTest(userSql, viewSql, "CREATE PROCEDURE\nBEGIN\nLOOP ON (SELECT pm1.g2.e2 AS s_0, pm1.g2.e2 AS s_1 FROM pm1.g1 INNER JOIN pm1.g2 ON g1.e1 = g2.e1 WHERE pm1.g2.e3 IS NULL) AS X\nBEGIN\nUPDATE pm1.g2 SET e1 = pm1.g2.e2 WHERE pm1.g2.e2 = X.s_1;\nVARIABLES.ROWS_UPDATED = (VARIABLES.ROWS_UPDATED + 1);\nEND\nEND",
				dm);
	}
	
	@Test public void testDeleteComplex() throws Exception {
		String userSql = "delete from vm1.gx where e2 < 10"; //$NON-NLS-1$
		String viewSql = "select g2.* from pm1.g1 inner join pm1.g2 on g1.e1 = g2.e1";
		
		HardcodedDataManager dm = new HardcodedDataManager();
        dm.addData("SELECT g_1.e2 FROM pm1.g1 AS g_0, pm1.g2 AS g_1 WHERE (g_0.e1 = g_1.e1) AND (g_1.e2 < 10)", new List[] {Arrays.asList(2)});
        dm.addData("DELETE FROM pm1.g2 WHERE pm1.g2.e2 = 2", new List[] {Arrays.asList(1)});
        
		helpTest(userSql, viewSql, "CREATE PROCEDURE\nBEGIN\nLOOP ON (SELECT pm1.g2.e2 AS s_0 FROM pm1.g1 INNER JOIN pm1.g2 ON g1.e1 = g2.e1 WHERE pm1.g2.e2 < 10) AS X\nBEGIN\nDELETE FROM pm1.g2 WHERE pm1.g2.e2 = X.s_0;\nVARIABLES.ROWS_UPDATED = (VARIABLES.ROWS_UPDATED + 1);\nEND\nEND",
				dm);
	}

}
