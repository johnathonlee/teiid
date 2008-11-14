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
package com.metamatrix.server.integration;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.Arrays;
import java.util.List;

import com.metamatrix.connector.jdbc.oracle.OracleCapabilities;
import com.metamatrix.connector.jdbc.sqlserver.SqlServerCapabilities;
import com.metamatrix.core.util.UnitTestUtil;
import com.metamatrix.dqp.internal.datamgr.CapabilitiesConverter;
import com.metamatrix.query.metadata.QueryMetadataInterface;
import com.metamatrix.query.optimizer.TestOptimizer;
import com.metamatrix.query.optimizer.capabilities.FakeCapabilitiesFinder;
import com.metamatrix.query.processor.HardcodedDataManager;
import com.metamatrix.query.processor.ProcessorPlan;


/** 
 * @since 4.2
 */
public class TestTPCR extends BaseQueryTest {

    private static final boolean DEBUG = false;

    private static final QueryMetadataInterface METADATA = createMetadata(UnitTestUtil.getTestDataPath()+"/TPC_R.vdb");  //$NON-NLS-1$
    
    public TestTPCR(String name) {
        super(name);
    }

    /**
     * Will create a full push down query
     */
    public void testQuery3() throws Exception{
        FakeCapabilitiesFinder finder = new FakeCapabilitiesFinder();
        finder.addCapabilities("TPCR_Oracle_9i", CapabilitiesConverter.convertCapabilities(new OracleCapabilities())); //$NON-NLS-1$
        
        ProcessorPlan plan = createPlan(METADATA,  
                 "select l_orderkey, sum(l_extendedprice*(1-l_discount)) as revenue, o_orderdate, o_shippriority " + //$NON-NLS-1$
                 "from customer, orders, lineitem " +  //$NON-NLS-1$
                 "where c_mktsegment = 'BUILDING' and c_custkey = o_custkey and l_orderkey = o_orderkey " +  //$NON-NLS-1$
                 "and o_orderdate < {ts'1995-03-15 00:00:00'} " +  //$NON-NLS-1$
                 "and l_shipdate > {ts'1995-03-15 00:00:00'} " +  //$NON-NLS-1$
                 "group by l_orderkey, o_orderdate, o_shippriority " + //$NON-NLS-1$
                 "order by revenue desc, o_orderdate", //$NON-NLS-1$
                 finder, DEBUG);

        HardcodedDataManager dataMgr = new HardcodedDataManager();
    
        List[] expected =
            new List[] { Arrays.asList(new Object[] { new Double(2456423.0), new BigDecimal("406181.0111"), new Date(95, 2, 5), new Double(0.0) }), //$NON-NLS-1$
                         Arrays.asList(new Object[] { new Double(3459808.0), new BigDecimal("405838.6989"), new Date(95, 2, 4), new Double(0.0) }), //$NON-NLS-1$
                         Arrays.asList(new Object[] { new Double(492164.0), new BigDecimal("390324.0610"), new Date(95, 1, 19), new Double(0.0) }) }; //$NON-NLS-1$

        dataMgr.addData("SELECT g_2.l_orderkey AS c_0, SUM((g_2.l_extendedprice * (1 - g_2.l_discount))) AS c_1, g_1.o_orderdate AS c_2, g_1.o_shippriority AS c_3 FROM TPCR_Oracle_9i.CUSTOMER AS g_0, TPCR_Oracle_9i.ORDERS AS g_1, TPCR_Oracle_9i.LINEITEM AS g_2 WHERE (g_2.l_orderkey = g_1.o_orderkey) AND (g_2.l_shipdate > {ts'1995-03-15 00:00:00.0'}) AND (g_0.c_custkey = g_1.o_custkey) AND (g_0.c_mktsegment = 'BUILDING') AND (g_1.o_orderdate < {d'1995-03-15'}) GROUP BY g_2.l_orderkey, g_1.o_orderdate, g_1.o_shippriority ORDER BY c_1 DESC, c_2", //$NON-NLS-1$
                        expected);

        doProcess(plan, dataMgr, expected, DEBUG);
        
    }
    
    public void testQueryCase3042() throws Exception{
        FakeCapabilitiesFinder finder = new FakeCapabilitiesFinder();
        finder.addCapabilities("TPCR_Ora", CapabilitiesConverter.convertCapabilities(new OracleCapabilities())); //$NON-NLS-1$
        
        ProcessorPlan plan = createPlan(BaseQueryTest.createMetadata(UnitTestUtil.getTestDataPath()+"/TPCR_3.vdb"),  //$NON-NLS-1$
                 "SELECT count (*)  " + //$NON-NLS-1$
                 "FROM TPCR_Ora.CUSTOMER LEFT OUTER JOIN TPCR_Ora.ORDERS ON C_CUSTKEY = O_CUSTKEY " +  //$NON-NLS-1$
                 "WHERE (O_ORDERKEY IS NULL) OR O_ORDERDATE < '1992-01-02 00:00:00' " +  //$NON-NLS-1$
                 "AND C_ACCTBAL > 0", //$NON-NLS-1$
                 finder, DEBUG);

        HardcodedDataManager dataMgr = new HardcodedDataManager();
        List[] expected =
            new List[] { Arrays.asList(new Object[] { new Integer(5) } ) };
                
        dataMgr.addData("SELECT COUNT(*) FROM TPCR_Ora.CUSTOMER AS g_0 LEFT OUTER JOIN TPCR_Ora.ORDERS AS g_1 ON g_0.C_CUSTKEY = g_1.O_CUSTKEY WHERE (g_1.O_ORDERKEY IS NULL) OR ((g_1.O_ORDERDATE < {ts'1992-01-02 00:00:00.0'}) AND (g_0.C_ACCTBAL > 0))", //$NON-NLS-1$
                       expected);
        
        doProcess(plan, dataMgr, expected, DEBUG);
        
    }
    
    /**
     * Test of case 3047 - need a query planner optimization to recognize when join clause criteria
     * could be migrated to WHERE clause of an atomic query, as long as the join is not being pushed
     * down.  In this case, there is a left outer join.  The join criteria includes 
     * O_ORDERDATE < {ts'1992-01-02 00:00:00.0'} which is on the inner side of the outer join and
     * thus cannot normally be moved to the WHERE clause.  However, since the join is cross-data
     * source, the join will be performed in MetaMatrix, and the above criteria could be moved to
     * the WHERE clause of the atomic query, since that WHERE clause will effectively still be 
     * applied before the join is processed, and the results will be the same.  This is what the 
     * user wants to happen.
     * @throws Exception
     */
    public void testQueryCase3047() throws Exception{
        FakeCapabilitiesFinder finder = new FakeCapabilitiesFinder();
        finder.addCapabilities("TPCR_Ora", CapabilitiesConverter.convertCapabilities(new OracleCapabilities())); //$NON-NLS-1$
        finder.addCapabilities("TPCR_SQLS", CapabilitiesConverter.convertCapabilities(new SqlServerCapabilities())); //$NON-NLS-1$
        
        ProcessorPlan plan = createPlan(BaseQueryTest.createMetadata(UnitTestUtil.getTestDataPath()+"/TPCR_3.vdb"),  //$NON-NLS-1$
                 "SELECT C_CUSTKEY, C_NAME, C_ADDRESS, C_PHONE, C_ACCTBAL, O_ORDERKEY FROM TPCR_Ora.CUSTOMER " + //$NON-NLS-1$
                 "LEFT OUTER JOIN TPCR_SQLS.ORDERS ON C_CUSTKEY = O_CUSTKEY " + //$NON-NLS-1$
                 "AND O_ORDERDATE < {ts'1992-01-02 00:00:00.0'} " + //$NON-NLS-1$
                 "WHERE (C_ACCTBAL > 50)", //$NON-NLS-1$
                 finder, DEBUG);

        HardcodedDataManager dataMgr = new HardcodedDataManager();
                
        List[] oracleExpected =
            new List[] { Arrays.asList(new Object[] { new Long(5), "Bill", "101 Fake St.", "392839283", "21.12" } ), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                         Arrays.asList(new Object[] { new Long(6), "Stu", "102 Fake St.", "385729385", "51.50" } )}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        dataMgr.addData("SELECT g_0.C_CUSTKEY AS c_0, g_0.C_NAME AS c_1, g_0.C_ADDRESS AS c_2, g_0.C_PHONE AS c_3, g_0.C_ACCTBAL AS c_4 FROM TPCR_Ora.CUSTOMER AS g_0 WHERE g_0.C_ACCTBAL > 50 ORDER BY c_0", //$NON-NLS-1$
                        oracleExpected);

        List[] sqlServerExpected =
            new List[] { Arrays.asList(new Object[] { new Integer(5), new Integer(12), new Long(5) } ),
                         Arrays.asList(new Object[] { new Integer(5), new Integer(13), new Long(5) } )};
        dataMgr.addData("SELECT g_0.O_CUSTKEY AS c_0, g_0.O_ORDERKEY AS c_1, g_0.O_CUSTKEY AS c_2 FROM TPCR_SQLS.ORDERS AS g_0 WHERE g_0.O_ORDERDATE < {ts'1992-01-02 00:00:00.0'} ORDER BY c_2", //$NON-NLS-1$
                        sqlServerExpected);
        
        List[] expected =
            new List[] { Arrays.asList(new Object[] { new Long(5), "Bill", "101 Fake St.", "392839283", "21.12", new Integer(12) } ), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                         Arrays.asList(new Object[] { new Long(5), "Bill", "101 Fake St.", "392839283", "21.12", new Integer(13) } ), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                         Arrays.asList(new Object[] { new Long(6), "Stu", "102 Fake St.", "385729385", "51.50", null } )}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        doProcess(plan, dataMgr, expected, DEBUG);
        
    }     

    /**
     * Confirm the workaround for case 3047 (using an inline view to get the desired piece
     * of criteria pushed down) 
     * @throws Exception
     * @since 4.3
     */
    public void testQueryCase3047workaround() throws Exception{
        FakeCapabilitiesFinder finder = new FakeCapabilitiesFinder();
        finder.addCapabilities("TPCR_Ora", CapabilitiesConverter.convertCapabilities(new OracleCapabilities())); //$NON-NLS-1$
        finder.addCapabilities("TPCR_SQLS", CapabilitiesConverter.convertCapabilities(new SqlServerCapabilities())); //$NON-NLS-1$
        
        ProcessorPlan plan = createPlan(BaseQueryTest.createMetadata(UnitTestUtil.getTestDataPath()+"/TPCR_3.vdb"),  //$NON-NLS-1$
                 "SELECT C_CUSTKEY, C_NAME, C_ADDRESS, C_PHONE, C_ACCTBAL, O_ORDERKEY FROM TPCR_Ora.CUSTOMER " + //$NON-NLS-1$
                 "LEFT OUTER JOIN " + //$NON-NLS-1$
                 "(SELECT O_CUSTKEY, O_ORDERKEY FROM TPCR_SQLS.ORDERS WHERE O_ORDERDATE < {ts'1992-01-02 00:00:00.0'}) AS X " + //$NON-NLS-1$
                 "ON C_CUSTKEY = O_CUSTKEY " + //$NON-NLS-1$
                 "WHERE (C_ACCTBAL > 50)", //$NON-NLS-1$
                 finder, DEBUG);

        HardcodedDataManager dataMgr = new HardcodedDataManager();
                
        List[] oracleExpected =
            new List[] { Arrays.asList(new Object[] { new Long(5), "Bill", "101 Fake St.", "392839283", "51.12" } ), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                         Arrays.asList(new Object[] { new Long(6), "Stu", "102 Fake St.", "385729385", "51.50" } )}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        dataMgr.addData("SELECT g_0.C_CUSTKEY AS c_0, g_0.C_NAME AS c_1, g_0.C_ADDRESS AS c_2, g_0.C_PHONE AS c_3, g_0.C_ACCTBAL AS c_4 FROM TPCR_Ora.CUSTOMER AS g_0 WHERE g_0.C_ACCTBAL > 50 ORDER BY c_0", //$NON-NLS-1$
                        oracleExpected);

        List[] sqlServerExpected =
            new List[] { Arrays.asList(new Object[] { new Integer(5), new Integer(12), new Long(5) } ),
                         Arrays.asList(new Object[] { new Integer(5), new Integer(13), new Long(5) } )};
        dataMgr.addData("SELECT g_0.O_CUSTKEY AS c_0, g_0.O_ORDERKEY AS c_1, g_0.O_CUSTKEY AS c_2 FROM TPCR_SQLS.ORDERS AS g_0 WHERE g_0.O_ORDERDATE < {ts'1992-01-02 00:00:00.0'} ORDER BY c_2", //$NON-NLS-1$
                        sqlServerExpected);
        
        List[] expected =
            new List[] { Arrays.asList(new Object[] { new Long(5), "Bill", "101 Fake St.", "392839283", "51.12", new Integer(12) } ), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                         Arrays.asList(new Object[] { new Long(5), "Bill", "101 Fake St.", "392839283", "51.12", new Integer(13) } ), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                         Arrays.asList(new Object[] { new Long(6), "Stu", "102 Fake St.", "385729385", "51.50", null } )}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        doProcess(plan, dataMgr, expected, DEBUG);
        
    }    
    
    public void testQuery22() throws Exception{
        FakeCapabilitiesFinder finder = new FakeCapabilitiesFinder();
        finder.addCapabilities("TPCR_Oracle_9i", CapabilitiesConverter.convertCapabilities(new OracleCapabilities())); //$NON-NLS-1$
        
        ProcessorPlan plan = TestOptimizer.helpPlan("SELECT custsale.cntrycode, COUNT(*) AS numcust, SUM(c_acctbal) AS totacctbal FROM (SELECT left(C_PHONE, 2) AS cntrycode, CUSTOMER.C_ACCTBAL FROM CUSTOMER WHERE (left(C_PHONE, 2) IN ('13','31','23','29','30','18','17')) AND (CUSTOMER.C_ACCTBAL > (SELECT AVG(CUSTOMER.C_ACCTBAL) FROM CUSTOMER WHERE (CUSTOMER.C_ACCTBAL > 0.0) AND (left(C_PHONE, 2) IN ('13','31','23','29','30','18','17')))) AND (NOT (EXISTS (SELECT * FROM ORDERS WHERE O_CUSTKEY = C_CUSTKEY)))) AS custsale GROUP BY custsale.cntrycode ORDER BY custsale.cntrycode", //$NON-NLS-1$
        		METADATA, null, finder,
        		new String[] {"SELECT v_0.c_0, COUNT(*) AS c_1, SUM(v_0.c_1) AS c_2 FROM (SELECT left(g_0.C_PHONE, 2) AS c_0, g_0.C_ACCTBAL AS c_1 FROM TPCR_Oracle_9i.CUSTOMER AS g_0 WHERE (left(g_0.C_PHONE, 2) IN ('13', '31', '23', '29', '30', '18', '17')) AND (g_0.C_ACCTBAL > (SELECT AVG(g_1.C_ACCTBAL) FROM TPCR_Oracle_9i.CUSTOMER AS g_1 WHERE (g_1.C_ACCTBAL > 0.0) AND (left(g_1.C_PHONE, 2) IN ('13', '31', '23', '29', '30', '18', '17')))) AND (NOT (EXISTS (SELECT g_2.O_ORDERKEY, g_2.O_CUSTKEY, g_2.O_ORDERSTATUS, g_2.O_TOTALPRICE, g_2.O_ORDERDATE, g_2.O_ORDERPRIORITY, g_2.O_CLERK, g_2.O_SHIPPRIORITY, g_2.O_COMMENT FROM TPCR_Oracle_9i.ORDERS AS g_2 WHERE g_2.O_CUSTKEY = g_0.C_CUSTKEY)))) AS v_0 GROUP BY v_0.c_0 ORDER BY c_0"}, true); //$NON-NLS-1$
        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN);
        TestOptimizer.checkSubPlanCount(plan, 0);
    }
    
    public void testDefect22475() throws Exception {
        FakeCapabilitiesFinder finder = new FakeCapabilitiesFinder();
        finder.addCapabilities("TPCR_Oracle_9i", CapabilitiesConverter.convertCapabilities(new SqlServerCapabilities())); //$NON-NLS-1$

        ProcessorPlan plan = TestOptimizer.helpPlan("select S_ACCTBAL, S_NAME, N_NAME, P_PARTKEY, P_MFGR, S_ADDRESS, S_PHONE, S_COMMENT from (SELECT SUPPLIER.S_ACCTBAL, SUPPLIER.S_NAME, NATION.N_NAME, PART.P_PARTKEY, PART.P_MFGR, SUPPLIER.S_ADDRESS, SUPPLIER.S_PHONE, SUPPLIER.S_COMMENT FROM PART, SUPPLIER, PARTSUPP, NATION, REGION WHERE (PART.P_PARTKEY = PS_PARTKEY) AND (S_SUPPKEY = PS_SUPPKEY) AND (P_SIZE = 15) AND (P_TYPE LIKE '%BRASS') AND (S_NATIONKEY = N_NATIONKEY) AND (N_REGIONKEY = R_REGIONKEY) AND (R_NAME = 'EUROPE') AND (PS_SUPPLYCOST = (SELECT MIN(PS_SUPPLYCOST) FROM PARTSUPP, SUPPLIER, NATION, REGION WHERE (PART.P_PARTKEY = PS_PARTKEY) AND (S_SUPPKEY = PS_SUPPKEY) AND (S_NATIONKEY = N_NATIONKEY) AND (N_REGIONKEY = R_REGIONKEY) AND (R_NAME = 'EUROPE'))) ORDER BY SUPPLIER.S_ACCTBAL DESC, NATION.N_NAME, SUPPLIER.S_NAME, PART.P_PARTKEY) as x", //$NON-NLS-1$
        		METADATA, null, finder,
        		new String[] {"SELECT g_1.S_ACCTBAL, g_1.S_NAME, g_3.N_NAME, g_0.P_PARTKEY, g_0.P_MFGR, g_1.S_ADDRESS, g_1.S_PHONE, g_1.S_COMMENT FROM TPCR_Oracle_9i.PART AS g_0, TPCR_Oracle_9i.SUPPLIER AS g_1, TPCR_Oracle_9i.PARTSUPP AS g_2, TPCR_Oracle_9i.NATION AS g_3, TPCR_Oracle_9i.REGION AS g_4 WHERE (g_2.PS_SUPPLYCOST = (SELECT MIN(g_5.PS_SUPPLYCOST) FROM TPCR_Oracle_9i.PARTSUPP AS g_5, TPCR_Oracle_9i.SUPPLIER AS g_6, TPCR_Oracle_9i.NATION AS g_7, TPCR_Oracle_9i.REGION AS g_8 WHERE (g_7.N_REGIONKEY = g_8.R_REGIONKEY) AND (g_8.R_NAME = 'EUROPE') AND (g_6.S_NATIONKEY = g_7.N_NATIONKEY) AND (g_6.S_SUPPKEY = g_5.PS_SUPPKEY) AND (g_5.PS_PARTKEY = g_0.P_PARTKEY))) AND (g_0.P_PARTKEY = g_2.PS_PARTKEY) AND (g_0.P_SIZE = 15.0) AND (g_0.P_TYPE LIKE '%BRASS') AND (g_1.S_NATIONKEY = g_3.N_NATIONKEY) AND (g_1.S_SUPPKEY = g_2.PS_SUPPKEY) AND (g_3.N_REGIONKEY = g_4.R_REGIONKEY) AND (g_4.R_NAME = 'EUROPE')"}, true); //$NON-NLS-1$
        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN);
    }
 
}
