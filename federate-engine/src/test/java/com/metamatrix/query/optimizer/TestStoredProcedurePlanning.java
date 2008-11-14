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

package com.metamatrix.query.optimizer;

import junit.framework.TestCase;

import com.metamatrix.query.metadata.TempMetadataAdapter;
import com.metamatrix.query.metadata.TempMetadataStore;
import com.metamatrix.query.processor.ProcessorPlan;
import com.metamatrix.query.unittest.FakeMetadataFactory;

public class TestStoredProcedurePlanning extends TestCase {
    
    /**
     * Test planning stored queries. GeminiStoredQueryTestPlan - 1a
     */
    public void testStoredQuery1() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sq1()", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "SELECT e1, e2 FROM pm1.g1" }); //$NON-NLS-1$
        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN); 
        TestOptimizer.checkSubPlanCount(plan, 0);        
    }
    
    /**
     * Test planning stored queries
     */
    public void testStoredQuery2() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sq1()", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "SELECT e1, e2 FROM pm1.g1" }); //$NON-NLS-1$
        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN); 
    }
    
    /**
     * Test planning stored queries. GeminiStoredQueryTestPlan - 1b
     */
    public void testStoredQuery3() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sq2('1')", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "SELECT e1, e2 FROM pm1.g1 WHERE e1 = '1'" }); //$NON-NLS-1$
        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN); 
    }
    
    public void testStoredQuery4() {
        ProcessorPlan plan = TestOptimizer.helpPlan("select x.e1 from (EXEC pm1.sq1()) as x", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "SELECT e1 FROM pm1.g1" }); //$NON-NLS-1$
        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN); 
    }

    public void testStoredQuery5() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sp1()", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "EXEC pm1.sp1()" }); //$NON-NLS-1$
        TestOptimizer.checkNodeTypes(plan, new int[] {
            1,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            0,      // NestedLoopJoinStrategy
            0,      // MergeJoinStrategy
            0,      // Null
            0,      // PlanExecution
            1,      // Project
            0,      // Select
            0,      // Sort
            0       // UnionAll
        }); 
    }
    
    public void testStoredQuery6() {
        ProcessorPlan plan = TestOptimizer.helpPlan("select x.e1 from (EXEC pm1.sp1()) as x", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "EXEC pm1.sp1()" }); //$NON-NLS-1$
        TestOptimizer.checkNodeTypes(plan, new int[] {
            1,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            0,      // NestedLoopJoinStrategy
            0,      // MergeJoinStrategy
            0,      // Null
            0,      // PlanExecution
            2,      // Project
            0,      // Select
            0,      // Sort
            0       // UnionAll
        }); 
    }
    
    public void testStoredQuery7() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sqsp1()", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "EXEC pm1.sp1()" }); //$NON-NLS-1$
        TestOptimizer.checkNodeTypes(plan, new int[] {
            1,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            0,      // NestedLoopJoinStrategy
            0,      // MergeJoinStrategy
            0,      // Null
            0,      // PlanExecution
            2,      // Project
            0,      // Select
            0,      // Sort
            0       // UnionAll
        }); 
    }
    
    /**
     * Test planning stored queries. GeminiStoredQueryTestPlan - 1c
     */
    public void testStoredQuery8() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sq3('1', 1)", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "SELECT e1, e2 FROM pm1.g1 WHERE e1 = '1'", "SELECT e1, e2 FROM pm1.g1 WHERE e2 = 1" }); //$NON-NLS-1$ //$NON-NLS-2$
        TestOptimizer.checkNodeTypes(plan, new int[] {
            2,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            0,      // NestedLoopJoinStrategy
            0,      // MergeJoinStrategy
            0,      // Null
            0,      // PlanExecution
            0,      // Project
            0,      // Select
            0,      // Sort
            1       // UnionAll
        }); 
    }
    
    /**
     * Test planning stored queries. GeminiStoredQueryTestPlan - 5a
     */
    public void testStoredQuery9() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sq4()", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] {"SELECT e1, e2 FROM pm1.g1" }); //$NON-NLS-1$
        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN); 
    }
    
    /**
     * Test planning stored queries. GeminiStoredQueryTestPlan - 5b
     */
    public void testStoredQuery10() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sq5('1')", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "SELECT e1, e2 FROM pm1.g1 WHERE e1 = '1'"}); //$NON-NLS-1$
        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN); 
    }
    
     /**
     * Test planning stored queries. GeminiStoredQueryTestPlan - 5c
     */
    public void testStoredQuery11() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sq6()", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] {"SELECT e1, e2 FROM pm1.g1 WHERE e1 = '1'" }); //$NON-NLS-1$
        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN); 
    }
    
     /**
     * Test planning stored queries. GeminiStoredQueryTestPlan - 6a
     */
    public void testStoredQuery12() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sq7()", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "SELECT e1 FROM pm1.g1" }); //$NON-NLS-1$
        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN); 
    }
    
    /**
     * Test planning stored queries. GeminiStoredQueryTestPlan - 6c
     */
    public void testStoredQuery13() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sq8('1')", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "SELECT e1 FROM pm1.g1 WHERE e1 = '1'" }); //$NON-NLS-1$
        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN); 
    }
    
    /**
     * Test planning stored queries. GeminiStoredQueryTestPlan - 6b
     */
    public void testStoredQuery14() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sq9('1')", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "SELECT e1 FROM pm1.g1 WHERE e1 = '1'" }); //$NON-NLS-1$
        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN); 
    }
    
    /**
     * Test planning stored queries. GeminiStoredQueryTestPlan - 6d
     */
    public void testStoredQuery15() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sq10('1', 2)", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "SELECT e1 FROM pm1.g1 WHERE (e1 = '1') AND (e2 = 2)" }); //$NON-NLS-1$
        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN); 
    }
    
    /**
     * Test planning stored queries. 
     */
    public void testStoredQuery16() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sp2(1)", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "EXEC pm1.sp2(1)" }); //$NON-NLS-1$
        TestOptimizer.checkNodeTypes(plan, new int[] {
            1,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            0,      // NestedLoopJoinStrategy
            0,      // MergeJoinStrategy
            0,      // Null
            0,      // PlanExecution
            1,      // Project
            0,      // Select
            0,      // Sort
            0       // UnionAll
        }); 
    }
    
    /**
     * Test planning stored queries. GeminiStoredQueryTestPlan - 6d
     */
    public void testStoredQuery17() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sq11(1, 2)", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "EXEC pm1.sp2(?)" }); //$NON-NLS-1$

        TestOptimizer.checkNodeTypes(plan, new int[] {
            1,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            0,      // NestedLoopJoinStrategy
            0,      // MergeJoinStrategy
            0,      // Null
            0,      // PlanExecution
            2,      // Project
            1,      // Select
            0,      // Sort
            0       // UnionAll
        }); 
    }
    
    //GeminiStoredQueryTestPlan - 2a, 2b
    public void testStoredQuery18() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sq12('1', 1)", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "INSERT INTO pm1.g1 (e1, e2) VALUES ('1', 1)" }); //$NON-NLS-1$

        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN); 
    }
    
    //GeminiStoredQueryTestPlan - 2c
    public void testStoredQuery19() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sq13('1')", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "INSERT INTO pm1.g1 (e1, e2) VALUES ('1', 2)" }); //$NON-NLS-1$

        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN); 
    }
    
    //GeminiStoredQueryTestPlan - 3c
    public void testStoredQuery20() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sq14('1', 2)", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "UPDATE pm1.g1 SET e1 = '1' WHERE e2 = 2" }); //$NON-NLS-1$

        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN); 
    }
    
    //GeminiStoredQueryTestPlan - 4b
    public void testStoredQuery21() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sq15('1', 2)", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "DELETE FROM pm1.g1 WHERE (e1 = '1') AND (e2 = 2)" }); //$NON-NLS-1$

        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN);               
    }
    
    public void testStoredQuery22() {
        ProcessorPlan plan = TestOptimizer.helpPlan("select e1 from (EXEC pm1.sq1()) as x where e1='a' union (select e1 from vm1.g2 where e1='b')", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "SELECT pm1.g2.e1 FROM pm1.g2 WHERE pm1.g2.e1 = 'b'", "SELECT e1 FROM pm1.g1 WHERE e1 = 'a'", "SELECT pm1.g1.e1 FROM pm1.g1 WHERE pm1.g1.e1 = 'b'" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        TestOptimizer.checkNodeTypes(plan, new int[] {
            3,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            1,      // DupRemove
            0,      // Grouping
            1,      // NestedLoopJoinStrategy
            0,      // MergeJoinStrategy
            0,      // Null
            0,      // PlanExecution
            1,      // Project
            0,      // Select
            0,      // Sort
            1       // UnionAll
        });               
    }
    
    public void testStoredQuery23() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sq16()", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "INSERT INTO pm1.g1 (e1, e2) VALUES ('1', 2)" }); //$NON-NLS-1$
            
        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN);               
    }
    
    public void testStoredQuery24() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sp3()", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "EXEC pm1.sp3()" }); //$NON-NLS-1$

        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN);               
    }

    // test implicit type conversion of argument 
    public void testStoredQuery25() {
        ProcessorPlan plan = TestOptimizer.helpPlan("EXEC pm1.sq15(1, 2)", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "DELETE FROM pm1.g1 WHERE (e1 = '1') AND (e2 = 2)" }); //$NON-NLS-1$

        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN);
    }

    public void testStoredQueryXML1() {
        TestOptimizer.helpPlan("EXEC pm1.sq18()", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), new String[] { }); //$NON-NLS-1$
    }
    
    /**
     * union of two stored procs - case #1466
     */
    public void testStoredProc1() {
        ProcessorPlan plan = TestOptimizer.helpPlan("SELECT * FROM (EXEC pm1.sp2(1)) AS x UNION ALL SELECT * FROM (EXEC pm1.sp2(2)) AS y", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "EXEC pm1.sp2(1)", "EXEC pm1.sp2(2)" }); //$NON-NLS-1$ //$NON-NLS-2$
        TestOptimizer.checkNodeTypes(plan, new int[] {
            2,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            0,      // NestedLoopJoinStrategy
            0,      // MergeJoinStrategy
            0,      // Null
            0,      // PlanExecution
            4,      // Project
            0,      // Select
            0,      // Sort
            1       // UnionAll
        }); 
    }

    /**
     * union of stored proc and query - case #1466
     */
    public void testStoredProc2() {
        ProcessorPlan plan = TestOptimizer.helpPlan("SELECT * FROM (EXEC pm1.sp2(1)) AS x UNION ALL SELECT e1, e2 FROM pm1.g1", new TempMetadataAdapter(FakeMetadataFactory.example1Cached(), new TempMetadataStore()), //$NON-NLS-1$
            new String[] { "EXEC pm1.sp2(1)", "SELECT e1, e2 FROM pm1.g1" }); //$NON-NLS-1$ //$NON-NLS-2$
        TestOptimizer.checkNodeTypes(plan, new int[] {
            2,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            0,      // NestedLoopJoinStrategy
            0,      // MergeJoinStrategy
            0,      // Null
            0,      // PlanExecution
            2,      // Project
            0,      // Select
            0,      // Sort
            1       // UnionAll
        }); 
    }    

}
