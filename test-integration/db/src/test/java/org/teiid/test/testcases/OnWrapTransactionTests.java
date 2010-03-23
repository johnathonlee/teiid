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
package org.teiid.test.testcases;

import java.util.ArrayList;

import org.teiid.jdbc.AbstractQueryTest;
import org.teiid.test.framework.TransactionContainer;
import org.teiid.test.framework.ConfigPropertyNames.TXN_AUTO_WRAP_OPTIONS;
import org.teiid.test.framework.query.AbstractQueryTransactionTest;
import org.teiid.test.framework.query.QueryExecution;
import org.teiid.test.framework.transaction.TxnAutoTransaction;

/**
 * @author vanhalbert
 * 
 */
public class OnWrapTransactionTests extends CommonTransactionTests {

    public OnWrapTransactionTests(String testName) {
	super(testName);
    }

    @Override
    protected TransactionContainer getTransactionContainter() {
	return new TxnAutoTransaction(TXN_AUTO_WRAP_OPTIONS.AUTO_WRAP_ON);
    }

    /**
     * Sources = 1 Commands = multiple Success Batching = Full Processing,
     * Single Connector Batch result = rollback
     */
    public void testSingleSourceMultipleCommandsReferentialIntegrityRollback()
	    throws Exception {
	AbstractQueryTransactionTest userTxn = new AbstractQueryTransactionTest(
		"testSingleSourceMultipleCommandsReferentialIntegrityRollback") {
	    public void testCase() throws Exception {
		for (int i = 200; i < 210; i++) {
		    Integer val = new Integer(i);
		    execute("insert into pm1.g1 (e1, e2) values(?,?)",
			    new Object[] { val, val.toString() });
		    execute("insert into pm1.g2 (e1, e2) values(?,?)",
			    new Object[] { val, val.toString() });
		}

		// try to rollback, however since this autocommit=on above two
		// are already commited
		execute("insert into pm1.g2 (e1, e2) values(?,?)",
			new Object[] { new Integer(9999), "9999" });
	    }

	    public boolean exceptionExpected() {
		return true;
	    }
	};

	// run test
	getTransactionContainter().runTransaction(userTxn);

	// now verify the results
	AbstractQueryTest test = new QueryExecution(userTxn.getSource("pm1"));
	test.execute("select * from g1 where e1 >= 200 and e1 < 210");
	test.assertRowCount(10);
	test.execute("select * from g2 where e1 = 9999");
	test.assertRowCount(0);
	test.closeConnection();
    }

    /**
     * Sources = 1 Commands = multiple Success Batching = Full Processing,
     * Single Connector Batch result = rollback
     */
    public void testSingleSourceBatchCommandReferentialIntegrityRollback()
	    throws Exception {
	AbstractQueryTransactionTest userTxn = new AbstractQueryTransactionTest(
		"testSingleSourceBatchCommandReferentialIntegrityRollback") {
	    public void testCase() throws Exception {
		ArrayList list = new ArrayList();
		for (int i = 200; i < 210; i++) {
		    list.add("insert into pm1.g1 (e1, e2) values(" + i + ",'"
			    + i + "')");
		}

		// try to rollback, since we are in single batch it must
		// rollback
		list.add("insert into pm1.g2 (e1, e2) values(9999,'9999')");
		executeBatch((String[]) list.toArray(new String[list.size()]));
	    }

	    public boolean exceptionExpected() {
		return true;
	    }
	};

	// run test
	getTransactionContainter().runTransaction(userTxn);

	// now verify the results
	AbstractQueryTest test = new QueryExecution(userTxn.getSource("pm1"));
	test.execute("select * from g1 where e1 >= 200 and e1 < 210");
	test.assertRowCount(0);
	test.execute("select * from g2 where e1 = 9999");
	test.assertRowCount(0);
	test.closeConnection();
    }

    /**
     * Sources = 2 Commands = 1, Update Batching = Full Processing, Single
     * Connector Batch result = commit
     */
    public void testMultipleSourceBulkRowInsertRollback() throws Exception {
	AbstractQueryTransactionTest userTxn = new AbstractQueryTransactionTest(
		"testMultipleSourceBulkRowInsertRollback") {
	    ArrayList list = new ArrayList();

	    public void testCase() throws Exception {
		for (int i = 100; i < 110; i++) {
		    list
			    .add("insert into vm.g1 (pm1e1, pm1e2, pm2e1, pm2e2) values("
				    + i + ",'" + i + "'," + i + ",'" + i + "')");
		}
		list
			.add("select pm1.g1.e1, pm1.g1.e2 into pm2.g2 from pm1.g1 where pm1.g1.e1 >= 100");

		// force the rollback by trying to insert an invalid row.
		list.add("insert into pm1.g2 (e1, e2) values(9999,'9999')");

		executeBatch((String[]) list.toArray(new String[list.size()]));
	    }

	    public boolean exceptionExpected() {
		return true;
	    }
	};

	// run test
	getTransactionContainter().runTransaction(userTxn);

	// now verify the results
	AbstractQueryTest test = new QueryExecution(userTxn.getSource("pm1"));
	test.execute("select * from g1 where e1 >= 100 and e1 < 110");
	test.assertRowCount(0);
	test.closeConnection();

	test = new QueryExecution(userTxn.getSource("pm2"));
	test.execute("select * from g1 where e1 >= 100 and e1 < 110");
	test.assertRowCount(0);
	test.execute("select * from g2 where e1 >= 100 and e1 < 110");
	test.assertRowCount(0);
	test.closeConnection();
    }
}
