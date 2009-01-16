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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.metamatrix.api.exception.MetaMatrixComponentException;
import com.metamatrix.api.exception.query.QueryMetadataException;
import com.metamatrix.api.exception.query.QueryParserException;
import com.metamatrix.api.exception.query.QueryResolverException;
import com.metamatrix.query.optimizer.capabilities.BasicSourceCapabilities;
import com.metamatrix.query.optimizer.capabilities.FakeCapabilitiesFinder;
import com.metamatrix.query.optimizer.capabilities.SourceCapabilities.Capability;
import com.metamatrix.query.optimizer.relational.rules.JoinUtil;
import com.metamatrix.query.parser.QueryParser;
import com.metamatrix.query.processor.ProcessorPlan;
import com.metamatrix.query.resolver.QueryResolver;
import com.metamatrix.query.resolver.util.ResolverVisitor;
import com.metamatrix.query.sql.lang.Criteria;
import com.metamatrix.query.sql.symbol.Expression;
import com.metamatrix.query.sql.symbol.GroupSymbol;
import com.metamatrix.query.unittest.FakeMetadataFacade;
import com.metamatrix.query.unittest.FakeMetadataFactory;
import com.metamatrix.query.unittest.FakeMetadataObject;
import com.metamatrix.query.unittest.FakeMetadataStore;


public class TestJoinOptimization extends TestCase {
    
    public TestJoinOptimization(String name) {
        super(name);
    }

    /**
     * Single group criteria should get pushed and be eligible for copy criteria
     */
    public void testInnerJoinPushAndCopyNonJoinCriteria() {
        String sql = "select bqt1.smalla.intkey, bqt2.smalla.intkey from bqt1.smalla inner join bqt2.smalla on (bqt1.smalla.intkey = bqt2.smalla.intkey and bqt2.smalla.intkey = 1)"; //$NON-NLS-1$
        
        // Plan query
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, FakeMetadataFactory.exampleBQTCached(), new String[] {"SELECT bqt1.smalla.intkey FROM bqt1.smalla WHERE bqt1.smalla.intkey = 1", "SELECT bqt2.smalla.intkey FROM bqt2.smalla WHERE bqt2.smalla.intkey = 1"}); //$NON-NLS-1$ //$NON-NLS-2$

        TestOptimizer.checkNodeTypes(plan, new int[] {
            2,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            1,      // Join
            0,      // MergeJoin
            0,      // Null
            0,      // PlanExecution
            1,      // Project
            0,      // Select
            0,      // Sort
            0       // UnionAll
        });
    }
    
    /**
     * Single group criteria should get pushed when it is on the inner side
     */
    public void testOuterJoinPushNonJoinCriteria() {
        String sql = "select bqt1.smalla.intkey from bqt1.smalla left outer join bqt2.smalla on (bqt1.smalla.intkey = bqt2.smalla.intkey and bqt2.smalla.stringkey = 1)"; //$NON-NLS-1$
        
        // Plan query
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, FakeMetadataFactory.exampleBQTCached(), new String[] {"SELECT bqt1.smalla.intkey FROM bqt1.smalla", "SELECT bqt2.smalla.intkey FROM bqt2.smalla WHERE bqt2.smalla.stringkey = '1'"}); //$NON-NLS-1$ //$NON-NLS-2$

        TestOptimizer.checkNodeTypes(plan, new int[] {
            2,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            0,      // Join
            1,      // MergeJoin
            0,      // Null
            0,      // PlanExecution
            1,      // Project
            0,      // Select
            0,      // Sort
            0       // UnionAll
        });
    }
    
    /**
     * Single group criteria should not be pushed when it is on the outer side 
     */
    public void testOuterJoinPushNonJoinCriteriaA() {
        String sql = "select bqt1.smalla.intkey from bqt1.smalla left outer join bqt2.smalla on (bqt1.smalla.intkey = bqt2.smalla.intkey and bqt1.smalla.stringkey = 1)"; //$NON-NLS-1$
        
        // Plan query
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, FakeMetadataFactory.exampleBQTCached(), new String[] {"SELECT bqt1.smalla.intkey, bqt1.smalla.stringkey FROM bqt1.smalla", "SELECT bqt2.smalla.intkey FROM bqt2.smalla"}); //$NON-NLS-1$ //$NON-NLS-2$

        TestOptimizer.checkNodeTypes(plan, new int[] {
            2,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            0,      // Join
            1,      // MergeJoin
            0,      // Null
            0,      // PlanExecution
            1,      // Project
            0,      // Select
            0,      // Sort
            0       // UnionAll
        });
    }
    
    public void testOuterJoinPushNonJoinCriteria_Case5547() {
        String sql = "select bqt1.smalla.intkey from bqt1.smalla left outer join bqt2.smalla on (1=1) option debug"; //$NON-NLS-1$
        String BQT1 = "BQT1";   //$NON-NLS-1$
        String BQT2 = "BQT2";   //$NON-NLS-1$
        FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder();
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities();
        
        // ===  Must set the ORDER BY prop on the capabilities object to TRUE
        caps.setCapabilitySupport(Capability.QUERY_ORDERBY, true);
        capFinder.addCapabilities(BQT1, caps); 
        capFinder.addCapabilities(BQT2, caps); 

        // ===  Must also set the ORDER BY prop on the models to TRUE
        FakeMetadataFacade metadata = FakeMetadataFactory.exampleBQT();

        FakeMetadataObject bqt1  = metadata.getStore().findObject(BQT1, FakeMetadataObject.MODEL); 
        bqt1.putProperty(FakeMetadataObject.Props.ORDER_BY, Boolean.TRUE);

        FakeMetadataObject bqt2 = metadata.getStore().findObject(BQT2, FakeMetadataObject.MODEL); 
        bqt2.putProperty(FakeMetadataObject.Props.ORDER_BY, Boolean.TRUE);            
        
        // Plan query
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, 
                                                    metadata, 
                                                    null, 
                                                    capFinder, 
                                                    new String[] {"SELECT bqt1.smalla.intkey FROM bqt1.smalla", "SELECT BQT2.SmallA.IntKey FROM bqt2.smalla"}, true ); //$NON-NLS-1$  //$NON-NLS-2$
        
        TestOptimizer.checkNodeTypes(plan, new int[] {
            2,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            1,      // NestedLoopJoinStrategy
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
     * Single group criteria should not be pushed when it is used in a full outer join
     * Note that the join has also degraded into a cross join rather than an outer join
     */
    public void testFullOuterJoinPushNonJoinCriteria() {
        String sql = "select bqt1.smalla.intkey, bqt2.smalla.intkey from bqt1.smalla full outer join bqt2.smalla on (bqt1.smalla.intkey = bqt2.smalla.intkey and bqt1.smalla.stringkey = 1 and bqt2.smalla.stringkey = 1)"; //$NON-NLS-1$
        
        // Plan query
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, FakeMetadataFactory.exampleBQTCached(), new String[] {"SELECT bqt1.smalla.intkey, bqt1.smalla.stringkey FROM bqt1.smalla", "SELECT bqt2.smalla.intkey, bqt2.smalla.stringkey FROM bqt2.smalla"}); //$NON-NLS-1$ //$NON-NLS-2$

        TestOptimizer.checkNodeTypes(plan, new int[] {
            2,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            0,      // Join
            1,      // MergeJoin
            0,      // Null
            0,      // PlanExecution
            1,      // Project
            0,      // Select
            0,      // Sort
            0       // UnionAll
        });
    }
    
    /**
     * Copy criteria should still work here even though the join criteria has an implicit type conversion because
     * the equality operation on the select criteria can be used. 
     */
    public void testCopyCriteriaWithFunction1() {
        String sql = "select bqt1.smalla.intkey, bqt2.smalla.intkey from bqt1.smalla, bqt2.smalla where bqt1.smalla.stringkey = bqt2.smalla.intkey and bqt2.smalla.intkey = 1"; //$NON-NLS-1$
        
        // Plan query
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, FakeMetadataFactory.exampleBQTCached(), new String[] {"SELECT bqt1.smalla.intkey FROM bqt1.smalla WHERE bqt1.smalla.stringkey = '1'", "SELECT bqt2.smalla.intkey FROM bqt2.smalla WHERE bqt2.smalla.intkey = 1"}); //$NON-NLS-1$ //$NON-NLS-2$

        TestOptimizer.checkNodeTypes(plan, new int[] {
            2,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            1,      // Join
            0,      // MergeJoin
            0,      // Null
            0,      // PlanExecution
            1,      // Project
            0,      // Select
            0,      // Sort
            0       // UnionAll
        });
    }

    /**
     * Copy criteria should not work here as the join criteria has an implicit convert and the where criteria is a non-equality predicate
     */
    public void testCopyCriteriaWithFunction2() {
        String sql = "select bqt1.smalla.intkey, bqt2.smalla.intkey from bqt1.smalla, bqt2.smalla where bqt1.smalla.stringkey = bqt2.smalla.intkey and bqt2.smalla.intkey <> 1"; //$NON-NLS-1$
        
        // Plan query
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, FakeMetadataFactory.exampleBQTCached(), new String[] {"SELECT bqt1.smalla.stringkey, bqt1.smalla.intkey FROM bqt1.smalla", "SELECT bqt2.smalla.intkey FROM bqt2.smalla WHERE bqt2.smalla.intkey <> 1"}); //$NON-NLS-1$ //$NON-NLS-2$

        TestOptimizer.checkNodeTypes(plan, new int[] {
            2,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            0,      // Join
            1,      // MergeJoin
            0,      // Null
            0,      // PlanExecution
            2,      // Project
            0,      // Select
            0,      // Sort
            0       // UnionAll
        });
    }
    
    /**
     * The intkey criteria should not be copied above to bqt1.smalla since the criteria is comming from the inner side in the join below 
     */
    public void testInvalidCopyCriteria() {
        String sql = "select bqt1.smalla.intkey from bqt1.smalla inner join (select bqt3.smalla.intkey from bqt2.smalla left outer join bqt3.smalla on bqt2.smalla.intkey = bqt3.smalla.intkey and bqt3.smalla.intkey = 1) foo on bqt1.smalla.intkey = foo.intkey"; //$NON-NLS-1$

        // Plan query
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, FakeMetadataFactory.exampleBQTCached(), new String[] {"SELECT bqt1.smalla.intkey FROM bqt1.smalla", "SELECT bqt3.smalla.intkey FROM bqt3.smalla WHERE bqt3.smalla.intkey = 1", "SELECT bqt2.smalla.intkey FROM bqt2.smalla"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        TestOptimizer.checkNodeTypes(plan, new int[] {
            3,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            1,      // Join
            1,      // MergeJoin
            0,      // Null
            0,      // PlanExecution
            1,      // Project
            0,      // Select
            0,      // Sort
            0       // UnionAll
        });
    }
    
    /*
     * Note that the criteria does not get copied to the outer side.
     */
    public void testCopyCriteriaFromInnerSide() {
        String sql = "select bqt1.smalla.intkey from bqt1.smalla left outer join (select bqt3.smalla.intkey from bqt3.smalla where bqt3.smalla.intkey = 1) foo on bqt1.smalla.intkey = foo.intkey"; //$NON-NLS-1$

        // Plan query
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, FakeMetadataFactory.exampleBQTCached(), new String[] {"SELECT bqt1.smalla.intkey FROM bqt1.smalla", "SELECT BQT3.SmallA.IntKey FROM bqt3.smalla WHERE bqt3.smalla.intkey = 1"}); //$NON-NLS-1$ //$NON-NLS-2$ 

        TestOptimizer.checkNodeTypes(plan, new int[] {
            2,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            1,      // Join
            0,      // MergeJoin
            0,      // Null
            0,      // PlanExecution
            1,      // Project
            0,      // Select
            0,      // Sort
            0       // UnionAll
        });
    }
    
    /**
     * Check to ensure that the full outer join does not get merged since the where criteria cannot be moved
     */
    public void testFullOuterJoinPreservation() {
        String sql = "select bqt2.mediumb.intkey from bqt2.mediumb full outer join (select bqt2.smallb.intkey from bqt2.smalla left outer join bqt2.smallb on bqt2.smalla.intkey = bqt2.smallb.intkey where bqt2.smalla.stringkey = 1) foo on bqt2.mediumb.intkey = foo.intkey"; //$NON-NLS-1$

        // Plan query
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, FakeMetadataFactory.exampleBQTCached(), 
                                                    new String[] {"SELECT g_1.intkey FROM bqt2.smalla AS g_0 LEFT OUTER JOIN bqt2.smallb AS g_1 ON g_0.intkey = g_1.intkey WHERE g_0.stringkey = '1'", "SELECT g_0.intkey FROM bqt2.mediumb AS g_0"}); //$NON-NLS-1$ //$NON-NLS-2$

        TestOptimizer.checkNodeTypes(plan, new int[] {
            2,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            0,      // Join
            1,      // MergeJoin
            0,      // Null
            0,      // PlanExecution
            1,      // Project
            0,      // Select
            0,      // Sort
            0       // UnionAll
        });        
    }
    
    /** 
     * Same as above but with a 0 group criteria
     */
    public void testFullOuterJoinPreservation1() {
        String sql = "select bqt2.mediumb.intkey from bqt2.mediumb full outer join (select bqt2.smallb.intkey from bqt2.smalla inner join bqt2.smallb on bqt2.smalla.intkey = bqt2.smallb.intkey where ? = 1) foo on bqt2.mediumb.intkey = foo.intkey"; //$NON-NLS-1$

        // Plan query
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, FakeMetadataFactory.exampleBQTCached(), new String[] {"SELECT bqt2.smallb.intkey FROM bqt2.smalla, bqt2.smallb WHERE (? = 1) AND (bqt2.smalla.intkey = bqt2.smallb.intkey)", "SELECT bqt2.mediumb.intkey FROM bqt2.mediumb"}); //$NON-NLS-1$ //$NON-NLS-2$

        TestOptimizer.checkNodeTypes(plan, new int[] {
            2,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            0,      // Join
            1,      // MergeJoin
            0,      // Null
            0,      // PlanExecution
            1,      // Project
            0,      // Select
            0,      // Sort
            0       // UnionAll
        });        
    }
    
    /** 
     * Same as above but with a left outer join
     */
    public void testOuterJoinPreservation() {
        String sql = "select bqt2.mediumb.intkey from bqt2.mediumb left outer join (select bqt2.smallb.intkey from bqt2.smalla inner join bqt2.smallb on bqt2.smalla.intkey = bqt2.smallb.intkey where ? = 1) foo on bqt2.mediumb.intkey = foo.intkey"; //$NON-NLS-1$

        // Plan query
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, FakeMetadataFactory.exampleBQTCached(), new String[] {"SELECT bqt2.smallb.intkey FROM bqt2.smalla, bqt2.smallb WHERE (? = 1) AND (bqt2.smalla.intkey = bqt2.smallb.intkey)", "SELECT bqt2.mediumb.intkey FROM bqt2.mediumb"}); //$NON-NLS-1$ //$NON-NLS-2$

        TestOptimizer.checkNodeTypes(plan, new int[] {
            2,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            0,      // Join
            1,      // MergeJoin
            0,      // Null
            0,      // PlanExecution
            1,      // Project
            0,      // Select
            0,      // Sort
            0       // UnionAll
        });        
    }
    
    public void testCopyCriteriaCreatesFalseCriteria() {
        String sql = "select bqt1.smalla.intkey, bqt2.smalla.intkey from bqt1.smalla, bqt2.smalla where bqt1.smalla.stringkey = bqt2.smalla.intkey and bqt2.smalla.intkey = 1 and bqt1.smalla.stringkey = '2'"; //$NON-NLS-1$

        // Plan query
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, FakeMetadataFactory.exampleBQTCached(), new String[] {});

        TestOptimizer.checkNodeTypes(plan, TestRuleRaiseNull.FULLY_NULL);        
    }
    
    public void testPushNonJoinCriteriaWithFalse() {
        String sql = "select bqt1.smalla.intkey, bqt2.smalla.intkey from bqt1.smalla left outer join bqt2.smalla on (bqt1.smalla.stringkey = bqt2.smalla.intkey and bqt2.smalla.intkey = null)"; //$NON-NLS-1$

        // Plan query
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, FakeMetadataFactory.exampleBQTCached(), new String[] {"SELECT bqt1.smalla.intkey FROM bqt1.smalla"}); //$NON-NLS-1$

        TestOptimizer.checkNodeTypes(plan, new int[] {
            1,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            0,      // Join
            0,      // MergeJoin
            0,      // Null
            0,      // PlanExecution
            1,      // Project
            0,      // Select
            0,      // Sort
            0       // UnionAll
        });        
    }
    
    public void testPushMultiGroupJoinCriteria() {
        String sql = "select bqt1.smalla.intkey, bqt1.smallb.intkey from bqt1.smalla right outer join (bqt1.smallb cross join (bqt1.mediuma cross join bqt1.mediumb)) on bqt1.smalla.stringkey = bqt1.smallb.stringkey" //$NON-NLS-1$ 
            +" where bqt1.smallb.intkey + bqt1.mediuma.intkey + bqt1.mediumb.intkey = 1"; //$NON-NLS-1$

        FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder();
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities();
        caps.setCapabilitySupport(Capability.QUERY_WHERE, true);
        caps.setCapabilitySupport(Capability.FUNCTION, true);
        caps.setFunctionSupport("+", true); //$NON-NLS-1$
        capFinder.addCapabilities("BQT1", caps); //$NON-NLS-1$
        
        // Plan query
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, FakeMetadataFactory.exampleBQTCached(), null, capFinder, 
                                                    new String[] {"SELECT bqt1.smalla.stringkey, bqt1.smalla.intkey FROM bqt1.smalla", "SELECT bqt1.smallb.stringkey, bqt1.smallb.intkey FROM bqt1.smallb, bqt1.mediuma, bqt1.mediumb WHERE ((bqt1.smallb.intkey + bqt1.mediuma.intkey) + bqt1.mediumb.intkey) = 1"}, true); //$NON-NLS-1$ //$NON-NLS-2$

        TestOptimizer.checkNodeTypes(plan, new int[] {
            2,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            0,      // Join
            1,      // MergeJoin
            0,      // Null
            0,      // PlanExecution
            1,      // Project
            0,      // Select
            0,      // Sort
            0       // UnionAll
        });        
    }
    
    /**
     * Since the multigroup criteria spans the inner side, it should not be pushed. 
     */
    public void testPushMultiGroupJoinCriteria1() {
        String sql = "select bqt1.smalla.intkey, bqt1.smallb.intkey from bqt1.smalla right outer join (bqt1.smallb cross join (bqt1.mediuma cross join bqt1.mediumb)) on bqt1.smalla.stringkey = bqt1.smallb.stringkey" //$NON-NLS-1$ 
            +" where bqt1.smalla.intkey + bqt1.mediuma.intkey + bqt1.mediumb.intkey is null"; //$NON-NLS-1$

        FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder();
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities();
        caps.setCapabilitySupport(Capability.QUERY_WHERE, true);
        capFinder.addCapabilities("BQT1", caps); //$NON-NLS-1$
        
        // Plan query
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, FakeMetadataFactory.exampleBQTCached(), null, capFinder, 
                                                    new String[] {"SELECT bqt1.smalla.stringkey, bqt1.smalla.intkey FROM bqt1.smalla", "SELECT bqt1.smallb.stringkey, bqt1.mediuma.intkey, bqt1.mediumb.intkey, bqt1.smallb.intkey FROM bqt1.smallb, bqt1.mediuma, bqt1.mediumb"}, true); //$NON-NLS-1$ //$NON-NLS-2$

        TestOptimizer.checkNodeTypes(plan, new int[] {
            2,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            0,      // Grouping
            0,      // Join
            1,      // MergeJoin
            0,      // Null
            0,      // PlanExecution
            1,      // Project
            1,      // Select
            0,      // Sort
            0       // UnionAll
        });        
    }
    
    /**
     * Since the multigroup criteria is not null dependent, it should get pushed. 
     */
    public void testPushMultiGroupJoinCriteria2() {
        String sql = "select bqt1.smalla.intkey, bqt1.smallb.intkey from bqt1.smalla right outer join (bqt1.smallb cross join (bqt1.mediuma cross join bqt1.mediumb)) on bqt1.smalla.stringkey = bqt1.smallb.stringkey" //$NON-NLS-1$ 
            +" where bqt1.smalla.intkey + bqt1.mediuma.intkey + bqt1.mediumb.intkey = 1"; //$NON-NLS-1$

        FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder();
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities();
        caps.setCapabilitySupport(Capability.QUERY_WHERE, true);
        caps.setCapabilitySupport(Capability.FUNCTION, true);
        caps.setFunctionSupport("+", true); //$NON-NLS-1$
        capFinder.addCapabilities("BQT1", caps); //$NON-NLS-1$
        
        // Plan query
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, FakeMetadataFactory.exampleBQTCached(), null, capFinder, 
                                                    new String[] {"SELECT bqt1.smalla.intkey, bqt1.smallb.intkey FROM bqt1.mediuma, bqt1.smallb, bqt1.mediumb, bqt1.smalla WHERE (((bqt1.smalla.intkey + bqt1.mediuma.intkey) + bqt1.mediumb.intkey) = 1) AND (bqt1.smalla.stringkey = bqt1.smallb.stringkey)"}, true); //$NON-NLS-1$ 

        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN);        
    }

    
    /**
     * Having criteria should not be considered as regular criteria (unless it contains no aggregate expressions). 
     */
    public void testHavingCriteriaNotUsedAsJoinCriteria() {
        String sql = "select bqt1.smalla.intkey, max(bqt1.smallb.intkey) from bqt1.smalla, bqt1.smallb where bqt1.smalla.intkey = bqt1.smallb.intnum group by bqt1.smallb.intkey, bqt1.smalla.intkey having max(bqt1.smallb.intkey) = bqt1.smalla.intkey"; //$NON-NLS-1$

        FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder();
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities();
        caps.setCapabilitySupport(Capability.QUERY_WHERE, true);
        caps.setCapabilitySupport(Capability.FUNCTION, true);
        caps.setCapabilitySupport(Capability.QUERY_AGGREGATES, true);
        caps.setCapabilitySupport(Capability.QUERY_AGGREGATES_MAX, true);
        capFinder.addCapabilities("BQT1", caps); //$NON-NLS-1$
        
        // Plan query
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, FakeMetadataFactory.exampleBQTCached(), null, capFinder, 
                                                    new String[] {"SELECT bqt1.smalla.intkey, MAX(bqt1.smallb.intkey) FROM bqt1.smalla, bqt1.smallb WHERE bqt1.smalla.intkey = bqt1.smallb.intnum GROUP BY bqt1.smallb.intkey, bqt1.smalla.intkey HAVING MAX(bqt1.smallb.intkey) = bqt1.smalla.intkey"}, true); //$NON-NLS-1$ 
                
        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN);  
    }
    
    /**
     * Ensure that subqueries not initially pushable to the source still get replaced
     */
    public void testSubqueryReplacement() {
        FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder();
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities();
        caps.setCapabilitySupport(Capability.QUERY_WHERE_IN, true);
        caps.setCapabilitySupport(Capability.QUERY_WHERE_IN_SUBQUERY, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN_OUTER, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN_OUTER_FULL, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN_SELFJOIN, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_GROUP_ALIAS, true);
        capFinder.addCapabilities("pm1", caps); //$NON-NLS-1$
        
        FakeMetadataFacade metadata = FakeMetadataFactory.example1();
        FakeMetadataObject g1 = metadata.getStore().findObject("pm1", FakeMetadataObject.MODEL); //$NON-NLS-1$
        g1.putProperty(FakeMetadataObject.Props.JOIN, Boolean.TRUE);
        g1.putProperty(FakeMetadataObject.Props.OUTER_JOIN, Boolean.TRUE);
        
        ProcessorPlan plan = TestOptimizer.helpPlan("select Y.e1, Y.e2 FROM vm1.g1 X left outer join vm1.g1 Y on Y.e1 = X.e1 where Y.e3 in (select e3 FROM vm1.g1) or Y.e3 IS NULL", metadata, null, capFinder,  //$NON-NLS-1$
            new String[]{"SELECT pm1.g1.e1, pm1.g1.e2 FROM pm1.g1 AS g1__1 LEFT OUTER JOIN pm1.g1 ON pm1.g1.e1 = g1__1.e1 WHERE (pm1.g1.e3 IN (SELECT pm1.g1.e3 FROM pm1.g1)) OR (pm1.g1.e3 IS NULL)"}, true); //$NON-NLS-1$ 
        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN); 
    }
    
    public void testRulePushNonJoinCriteriaPreservesOuterJoin() throws Exception {
        FakeMetadataFacade metadata = FakeMetadataFactory.exampleBQT();
        FakeMetadataObject g1 = metadata.getStore().findObject("BQT1", FakeMetadataObject.MODEL); //$NON-NLS-1$
        g1.putProperty(FakeMetadataObject.Props.JOIN, Boolean.TRUE);
        g1.putProperty(FakeMetadataObject.Props.OUTER_JOIN, Boolean.TRUE);
        String sql = "select b.intkey from (select intkey from bqt1.smalla) a left outer join (select intkey from bqt1.smallb) b on (1 = 1)"; //$NON-NLS-1$
        
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, metadata, 
                                                    new String[]{"SELECT g_1.intkey FROM bqt1.smalla AS g_0 LEFT OUTER JOIN bqt1.smallb AS g_1 ON 1 = 1"}, TestOptimizer.ComparisonMode.EXACT_COMMAND_STRING); //$NON-NLS-1$
        
        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN);
    }
    
    public void testOuterToInnerJoinConversion() {
        FakeMetadataFacade metadata = FakeMetadataFactory.exampleBQT();
        FakeMetadataObject g1 = metadata.getStore().findObject("BQT1", FakeMetadataObject.MODEL); //$NON-NLS-1$
        g1.putProperty(FakeMetadataObject.Props.JOIN, Boolean.TRUE);
        g1.putProperty(FakeMetadataObject.Props.OUTER_JOIN, Boolean.TRUE);
        String sql = "select bqt1.smalla.intkey from bqt1.smalla left outer join bqt1.smallb on (bqt1.smalla.intkey = bqt1.smallb.intkey) where bqt1.smallb.intnum = 1"; //$NON-NLS-1$
        
        TestOptimizer.helpPlan(sql, metadata, new String[]{"SELECT bqt1.smalla.intkey FROM bqt1.smalla, bqt1.smallb WHERE (bqt1.smalla.intkey = bqt1.smallb.intkey) AND (bqt1.smallb.intnum = 1)"}); //$NON-NLS-1$
    }
    
    //same as above, but with a right outer join
    public void testOuterToInnerJoinConversion1() {
        FakeMetadataFacade metadata = FakeMetadataFactory.exampleBQT();
        FakeMetadataObject g1 = metadata.getStore().findObject("BQT1", FakeMetadataObject.MODEL); //$NON-NLS-1$
        g1.putProperty(FakeMetadataObject.Props.JOIN, Boolean.TRUE);
        g1.putProperty(FakeMetadataObject.Props.OUTER_JOIN, Boolean.TRUE);
        String sql = "select bqt1.smalla.intkey from bqt1.smalla right outer join bqt1.smallb on (bqt1.smalla.intkey = bqt1.smallb.intkey) where bqt1.smalla.intnum = 1"; //$NON-NLS-1$
        
        TestOptimizer.helpPlan(sql, metadata, new String[]{"SELECT bqt1.smalla.intkey FROM bqt1.smallb, bqt1.smalla WHERE (bqt1.smalla.intkey = bqt1.smallb.intkey) AND (bqt1.smalla.intnum = 1)"}); //$NON-NLS-1$
    }
    
    public void testOuterToInnerJoinConversion2() {
        FakeMetadataFacade metadata = FakeMetadataFactory.exampleBQT();
        FakeMetadataObject g1 = metadata.getStore().findObject("BQT1", FakeMetadataObject.MODEL); //$NON-NLS-1$
        g1.putProperty(FakeMetadataObject.Props.JOIN, Boolean.TRUE);
        g1.putProperty(FakeMetadataObject.Props.OUTER_JOIN, Boolean.TRUE);
        String sql = "select bqt1.smalla.intkey from bqt1.smalla full outer join bqt1.smallb on (bqt1.smalla.intkey = bqt1.smallb.intkey) where bqt1.smallb.intnum = 1"; //$NON-NLS-1$
        
        TestOptimizer.helpPlan(sql, metadata, new String[]{"SELECT bqt1.smalla.intkey FROM bqt1.smallb LEFT OUTER JOIN bqt1.smalla ON bqt1.smalla.intkey = bqt1.smallb.intkey WHERE bqt1.smallb.intnum = 1"}); //$NON-NLS-1$
    }    
    
    public void testOuterToInnerJoinConversion3() {
        FakeMetadataFacade metadata = FakeMetadataFactory.exampleBQT();
        FakeMetadataObject g1 = metadata.getStore().findObject("BQT1", FakeMetadataObject.MODEL); //$NON-NLS-1$
        g1.putProperty(FakeMetadataObject.Props.JOIN, Boolean.TRUE);
        g1.putProperty(FakeMetadataObject.Props.OUTER_JOIN, Boolean.TRUE);
        String sql = "select bqt1.smalla.intkey from bqt1.smalla full outer join bqt1.smallb on (bqt1.smalla.intkey = bqt1.smallb.intkey) where bqt1.smalla.intnum = 1"; //$NON-NLS-1$
        
        TestOptimizer.helpPlan(sql, metadata, new String[]{"SELECT bqt1.smalla.intkey FROM bqt1.smalla LEFT OUTER JOIN bqt1.smallb ON bqt1.smalla.intkey = bqt1.smallb.intkey WHERE bqt1.smalla.intnum = 1"}); //$NON-NLS-1$
    }
    
    /**
     * non-depenent criteria on each side of a full outer creates an inner join  
     */
    public void testOuterToInnerJoinConversion4() {
        FakeMetadataFacade metadata = FakeMetadataFactory.exampleBQT();
        FakeMetadataObject g1 = metadata.getStore().findObject("BQT1", FakeMetadataObject.MODEL); //$NON-NLS-1$
        g1.putProperty(FakeMetadataObject.Props.JOIN, Boolean.TRUE);
        g1.putProperty(FakeMetadataObject.Props.OUTER_JOIN, Boolean.TRUE);
        String sql = "select bqt1.smalla.intkey from bqt1.smalla full outer join bqt1.smallb on (bqt1.smalla.intkey = bqt1.smallb.intkey) where bqt1.smalla.intnum = bqt1.smallb.intnum"; //$NON-NLS-1$
        
        TestOptimizer.helpPlan(sql, metadata, new String[]{"SELECT bqt1.smalla.intkey FROM bqt1.smalla, bqt1.smallb WHERE (bqt1.smalla.intkey = bqt1.smallb.intkey) AND (bqt1.smalla.intnum = bqt1.smallb.intnum)"}); //$NON-NLS-1$
    }
        
    /**
     * Since concat2 is null dependent the join will not be changed 
     */
    public void testOuterToInnerJoinConversionNullDependent() {
        FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder();
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities();
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN_OUTER, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN_OUTER_FULL, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN_SELFJOIN, true);
        caps.setFunctionSupport("concat2", true); //$NON-NLS-1$
        capFinder.addCapabilities("BQT1", caps); //$NON-NLS-1$
        
        FakeMetadataFacade metadata = FakeMetadataFactory.exampleBQT();
        FakeMetadataObject g1 = metadata.getStore().findObject("BQT1", FakeMetadataObject.MODEL); //$NON-NLS-1$
        g1.putProperty(FakeMetadataObject.Props.JOIN, Boolean.TRUE);
        g1.putProperty(FakeMetadataObject.Props.OUTER_JOIN, Boolean.TRUE);
        String sql = "select bqt1.smalla.intkey from bqt1.smalla left outer join bqt1.smallb on (bqt1.smalla.intkey = bqt1.smallb.intkey) where concat2(bqt1.smallb.intnum, '1') = 1"; //$NON-NLS-1$
        
        TestOptimizer.helpPlan(sql, metadata, null, capFinder, new String[]{"SELECT bqt1.smallb.intnum, bqt1.smalla.intkey FROM bqt1.smalla LEFT OUTER JOIN bqt1.smallb ON bqt1.smalla.intkey = bqt1.smallb.intkey"}, TestOptimizer.SHOULD_SUCCEED); //$NON-NLS-1$
    }
    
    public void testInlineViewToHaving() {
        FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder();
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities();
        caps.setCapabilitySupport(Capability.QUERY_AGGREGATES, true);
        caps.setCapabilitySupport(Capability.QUERY_AGGREGATES_MAX, true);
        capFinder.addCapabilities("BQT1", caps); //$NON-NLS-1$
        
        String sql = "select x.y, x.intkey from (select max(intnum) y, intkey from bqt1.smalla group by intkey) x where x.y = 1"; //$NON-NLS-1$
        
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, FakeMetadataFactory.exampleBQTCached(), null, capFinder, 
                               new String[]{"SELECT MAX(intnum), intkey FROM bqt1.smalla GROUP BY intkey HAVING MAX(intnum) = 1"}, TestOptimizer.SHOULD_SUCCEED); //$NON-NLS-1$
        
        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN);
    }
    
    /**
     * <p>This test illustrates a flaw in RuleBreakMultiJoin - that rule can potentially
     * choose a binary join tree that makes it impossible to satisfy an access pattern
     * with a dependent join (when it WOULD be possible if a different join tree were
     * chosen).</p>
     * 
     * <p>In RuleBreakMultiJoin terminology: 
     * If any of the regions contains a group with any unsatisfied access patterns, one
     * of those access patterns could be satisfied by arranging for a dependent join,
     * provided that group has join criteria which covers the column(s) in an access
     * pattern, and which joins the group to group(s) in other region(s).  The task, then,
     * is to ensure that an ordering isn't picked which makes such a dependent join 
     * impossible.</p>
     * 
     * <p>A physical group can have zero or more access patterns; each access pattern can have one
     * or more columns.  So a group could implicitly be dependent on one or more other physical
     * groups in one or more other regions.  A table can be used to illustrate the potential
     * complexity of access patterns:
     * <pre>
     * Region with    | Target
     * Access Patterns| Regions
     * -------------------------
     * Reg3           | Reg1, Reg2
     * Reg3           | Reg4
     * Reg1           | Reg2
     * Reg4           | Reg3
     * </pre></p>
     * 
     * This tests now passes with RulePlanJoins
     */
    public void testPathologicalAccessPatternCaseCase2976Defect19018() throws Exception{
        FakeMetadataFacade metadata = FakeMetadataFactory.example2();
        
        // add single access pattern to pm1.g4 containing elements e1, e2, and e3
        FakeMetadataStore store = metadata.getStore();
        FakeMetadataObject pm4g1 = store.findObject("pm4.g1", FakeMetadataObject.GROUP);//$NON-NLS-1$
        List elementIDs = metadata.getElementIDsInGroupID(pm4g1);
        elementIDs.remove(2);
        FakeMetadataObject pm4g1ap1 = FakeMetadataFactory.createAccessPattern("pm4.g1.ap1", pm4g1, elementIDs); //e1,e2,e4 //$NON-NLS-1$
        store.addObject(pm4g1ap1);

        String sql = "SELECT pm1.g1.e1, pm2.g1.e1, pm4.g1.e1 " +//$NON-NLS-1$
        "FROM pm1.g1, pm2.g1, pm4.g1 WHERE " +//$NON-NLS-1$
        "pm1.g1.e1 = pm4.g1.e1 AND pm2.g1.e2 = pm4.g1.e2 AND pm1.g1.e4 = pm2.g1.e4 " +//$NON-NLS-1$
        "AND pm4.g1.e4 = 3.2";//$NON-NLS-1$

        String[] expected = new String[] {"SELECT g_0.e4, g_0.e1 FROM pm1.g1 AS g_0", //$NON-NLS-1$
                                           "SELECT g_0.e1, g_0.e2 FROM pm4.g1 AS g_0 WHERE (g_0.e4 = 3.2) AND (g_0.e1 IN (<dependent values>)) AND (g_0.e2 IN (<dependent values>))", //$NON-NLS-1$ 
                                           "SELECT g_0.e4, g_0.e2, g_0.e1 FROM pm2.g1 AS g_0",//$NON-NLS-1$
                                          };
        
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, metadata, expected, TestOptimizer.ComparisonMode.EXACT_COMMAND_STRING);
        TestOptimizer.checkNodeTypes(plan, new int[] {
                                        2,      // Access
                                        1,      // DependentAccess
                                        0,      // DependentSelect
                                        0,      // DependentProject
                                        0,      // DupRemove
                                        0,      // Grouping
                                        0,      // NestedLoopJoinStrategy
                                        2,      // MergeJoinStrategy
                                        0,      // Null
                                        0,      // PlanExecution
                                        1,      // Project
                                        0,      // Select
                                        0,      // Sort
                                        0       // UnionAll
                                    });          
    }       

    public void testHavingCriteriaPushDown() {
        FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder();
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities();
        caps.setCapabilitySupport(Capability.QUERY_AGGREGATES, false);
        capFinder.addCapabilities("pm1", caps); //$NON-NLS-1$
        
        ProcessorPlan plan = TestOptimizer.helpPlan("select X.e1 FROM vm1.g1 X group by X.e1 having X.e1 = 1 and sum(X.e2) = 2", FakeMetadataFactory.example1Cached(), null, capFinder,  //$NON-NLS-1$
            new String[]{"SELECT pm1.g1.e1, pm1.g1.e2 FROM pm1.g1 WHERE pm1.g1.e1 = '1'"}, true); //$NON-NLS-1$
        TestOptimizer.checkNodeTypes(plan, new int[] {
            1,      // Access
            0,      // DependentAccess
            0,      // DependentSelect
            0,      // DependentProject
            0,      // DupRemove
            1,      // Grouping
            0,      // NestedLoopJoinStrategy
            0,      // MergeJoinStrategy
            0,      // Null
            0,      // PlanExecution
            1,      // Project
            1,      // Select
            0,      // Sort
            0       // UnionAll
        }); 
    }
    
    /**
     * non-null dependent criteria should get pushed down
     */
    public void testPushMultiGroupCriteriaOuterJoin() { 
        String sql = "select m.intkey, m.intnum, s.intkey, s.intnum from BQT2.mediuma m left outer join BQT2.smalla s on m.intkey = s.intkey where not (m.intkey + s.intnum = 26)"; //$NON-NLS-1$

        FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder();
        BasicSourceCapabilities caps = new BasicSourceCapabilities();
        caps.setCapabilitySupport(Capability.QUERY_WHERE, true);
        caps.setCapabilitySupport(Capability.QUERY_WHERE_COMPARE, true);
        caps.setCapabilitySupport(Capability.QUERY_WHERE_COMPARE_EQ, true);
        caps.setCapabilitySupport(Capability.QUERY_WHERE_COMPARE_GT, true);        
        caps.setCapabilitySupport(Capability.QUERY_WHERE_NOT, true);        //TODO is this being enforced?!
        caps.setCapabilitySupport(Capability.QUERY_WHERE_IN, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN_OUTER, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN_OUTER_FULL, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN_SELFJOIN, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_GROUP_ALIAS, true);
        caps.setCapabilitySupport(Capability.QUERY_ORDERBY, true);  
        caps.setCapabilitySupport(Capability.FUNCTION, true);
        caps.setFunctionSupport("+", true); //$NON-NLS-1$
        capFinder.addCapabilities("BQT2", caps); //$NON-NLS-1$
        
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, FakeMetadataFactory.exampleBQTCached(),
                                      null, capFinder, 
                                      new String[] { 
                                          "SELECT m.intkey, m.intnum, s.intkey, s.intnum FROM BQT2.mediuma AS m, BQT2.smalla AS s WHERE (m.intkey = s.intkey) AND (NOT ((m.intkey + s.intnum) = 26))" }, //$NON-NLS-1$
                                          TestOptimizer.SHOULD_SUCCEED); 

        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN);                                    
        
    }

    /**
     * Assumes that pm1.g1 is the only inner group 
     */
    private void helpTestNullDependentVisitor(String critSQL,
                                              boolean dependent) throws QueryParserException,
                                                                QueryResolverException,
                                                                QueryMetadataException,
                                                                MetaMatrixComponentException {
        List<GroupSymbol> innerGroups = new ArrayList<GroupSymbol>();
        innerGroups.add(new GroupSymbol("pm1.g1")); //$NON-NLS-1$
        
        Criteria crit = QueryParser.getQueryParser().parseCriteria(critSQL);
        
        QueryResolver.resolveCriteria(crit, FakeMetadataFactory.example1Cached());
        
        assertEquals(dependent, JoinUtil.isNullDependent(FakeMetadataFactory.example1Cached(), innerGroups, crit));
    }
    
    private void helpTestNullDependent(String expressionSQL,
                                              boolean dependent) throws QueryParserException,
                                                                QueryResolverException,
                                                                QueryMetadataException,
                                                                MetaMatrixComponentException {
        List<GroupSymbol> innerGroups = new ArrayList<GroupSymbol>();
        innerGroups.add(new GroupSymbol("pm1.g1")); //$NON-NLS-1$
        
        Expression expr = QueryParser.getQueryParser().parseExpression(expressionSQL);
        
        ResolverVisitor.resolveLanguageObject(expr, FakeMetadataFactory.example1Cached());
        
        assertEquals(dependent, JoinUtil.isNullDependent(FakeMetadataFactory.example1Cached(), innerGroups, expr));
    }
    
    public void testNullDependentVisitor() throws Exception {
        helpTestNullDependentVisitor("nvl(pm1.g1.e1, 1) = 1", true); //$NON-NLS-1$
    }
    
    public void testNullDependentVisitor1() throws Exception {
        helpTestNullDependentVisitor("ifnull(pm1.g1.e1, 1) = 1", true); //$NON-NLS-1$
    }

    public void testNullDependentVisitor2() throws Exception {
        helpTestNullDependentVisitor("rand(pm1.g1.e2) = 1", true); //$NON-NLS-1$
    }

    public void testNullDependentVisitor3() throws Exception {
        helpTestNullDependentVisitor("concat2(pm1.g1.e1, pm1.g1.e2) = '1'", false); //$NON-NLS-1$
    }
    
    public void testNullDependentVisitor4() throws Exception {
        helpTestNullDependentVisitor("nvl(pm1.g2.e1, 1) = 1", true); //$NON-NLS-1$
    }
    
    public void testNullDependentVisitor5() throws Exception {
        helpTestNullDependentVisitor("pm1.g1.e1 is null", true); //$NON-NLS-1$
    }    
    
    public void testNullDependentVisitor6() throws Exception {
        helpTestNullDependentVisitor("pm1.g1.e1 is not null", false); //$NON-NLS-1$
    }    

    public void testNullDependentVisitor7() throws Exception {
        helpTestNullDependentVisitor("pm1.g2.e1 is not null", true); //$NON-NLS-1$
    }
    
    //this is an important test, the or causes this criteria to be null dependent
    public void testNullDependentVisitor8() throws Exception {
        helpTestNullDependentVisitor("pm1.g1.e1 = 1 or pm1.g2.e1 = 1", true); //$NON-NLS-1$
    }
    
    public void testNullDependentVisitor9() throws Exception {
        helpTestNullDependentVisitor("pm1.g1.e1 = 1 or pm1.g1.e2 = 2", false); //$NON-NLS-1$
    }
    
    public void testNullDependentVisitor10() throws Exception {
        helpTestNullDependentVisitor("pm1.g1.e1 in (1, pm1.g2.e1)", false); //$NON-NLS-1$
    }
    
    public void testNullDependentVisitor11() throws Exception {
        helpTestNullDependentVisitor("pm1.g2.e1 in (1, pm1.g1.e1)", true); //$NON-NLS-1$
    }
    
    public void testIsNullDependent() throws Exception {
        helpTestNullDependent("pm1.g1.e2 + 1", false); //$NON-NLS-1$
    }
    
    public void testIsNullDependent1() throws Exception {
        helpTestNullDependent("pm1.g2.e2 + 1", true); //$NON-NLS-1$
    }

    /**
     *  The criteria will still get pushed to appropriate location, and
     *  the other side of the join will be removed
     */
    public void testCriteriaPushedWithUnionJoin() throws Exception {
        String sql = "select * from pm1.g1 union join pm1.g2 where g1.e1 = 1"; //$NON-NLS-1$
        
        FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder();
        BasicSourceCapabilities caps = new BasicSourceCapabilities();
        caps.setCapabilitySupport(Capability.QUERY_WHERE, true);
        caps.setCapabilitySupport(Capability.QUERY_WHERE_COMPARE, true);
        caps.setCapabilitySupport(Capability.QUERY_WHERE_COMPARE_EQ, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_GROUP_ALIAS, true);
        caps.setCapabilitySupport(Capability.FUNCTION, true);
        capFinder.addCapabilities("pm1", caps); //$NON-NLS-1$
        
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, FakeMetadataFactory.example1Cached(),
                                      null, capFinder, 
                                      new String[] { 
                                          "SELECT pm1.g1.e1, pm1.g1.e2, pm1.g1.e3, pm1.g1.e4 FROM pm1.g1 WHERE pm1.g1.e1 = '1'" }, //$NON-NLS-1$ 
                                          TestOptimizer.SHOULD_SUCCEED); 

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
     * union joins allow RuleRemoveVirtual to still take effect 
     */
    public void testCriteriaPushedWithUnionJoin1() throws Exception {
        String sql = "select vm1.g1.e1 from vm1.g1 union join vm1.g2 where g2.e1 = 1"; //$NON-NLS-1$
        
        FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder();
        BasicSourceCapabilities caps = new BasicSourceCapabilities();
        caps.setCapabilitySupport(Capability.QUERY_WHERE, true);
        caps.setCapabilitySupport(Capability.QUERY_WHERE_COMPARE, true);
        caps.setCapabilitySupport(Capability.QUERY_WHERE_COMPARE_EQ, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN_OUTER, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN_OUTER_FULL, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN_SELFJOIN, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_GROUP_ALIAS, true);
        caps.setCapabilitySupport(Capability.FUNCTION, true);
        capFinder.addCapabilities("pm1", caps); //$NON-NLS-1$
        
        FakeMetadataFacade metadata = FakeMetadataFactory.example1();
        FakeMetadataObject g1 = metadata.getStore().findObject("pm1", FakeMetadataObject.MODEL); //$NON-NLS-1$
        g1.putProperty(FakeMetadataObject.Props.JOIN, Boolean.TRUE);
        g1.putProperty(FakeMetadataObject.Props.OUTER_JOIN, Boolean.TRUE);
        
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, metadata,
                                      null, capFinder, 
                                      new String[] { 
                                          "SELECT g1__1.e1 FROM pm1.g1 AS g1__1, pm1.g2 WHERE (g1__1.e1 = '1') AND (pm1.g2.e1 = '1')" }, //$NON-NLS-1$ 
                                          TestOptimizer.SHOULD_SUCCEED);  

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
     * null-dependent expressions should prevent merging of virtual layers
     */
    public void testNullDependentPreventsMerge() throws Exception { 
        String sql = "select x from pm1.g1 left outer join (select nvl(e2, 1) x from pm1.g2) y on e2 = x"; //$NON-NLS-1$

        FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder();
        BasicSourceCapabilities caps = new BasicSourceCapabilities();
        caps.setCapabilitySupport(Capability.QUERY_WHERE, true);
        caps.setCapabilitySupport(Capability.QUERY_WHERE_COMPARE, true);
        caps.setCapabilitySupport(Capability.QUERY_WHERE_COMPARE_EQ, true);
        caps.setCapabilitySupport(Capability.QUERY_WHERE_COMPARE_GT, true);        
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN_OUTER, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_GROUP_ALIAS, true);
        caps.setCapabilitySupport(Capability.QUERY_ORDERBY, true);  
        caps.setCapabilitySupport(Capability.FUNCTION, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_INLINE_VIEWS, true);
        caps.setFunctionSupport("nvl", true); //$NON-NLS-1$
        capFinder.addCapabilities("pm1", caps); //$NON-NLS-1$
        
        FakeMetadataFacade metadata = FakeMetadataFactory.example1();
        FakeMetadataObject g1 = metadata.getStore().findObject("pm1", FakeMetadataObject.MODEL); //$NON-NLS-1$
        g1.putProperty(FakeMetadataObject.Props.JOIN, Boolean.TRUE);
        g1.putProperty(FakeMetadataObject.Props.OUTER_JOIN, Boolean.TRUE);
        
        ProcessorPlan plan = TestOptimizer.helpPlan(sql, metadata,
                                      null, capFinder, 
                                      new String[] { 
                                          "SELECT v_0.c_0 FROM pm1.g1 AS g_0 LEFT OUTER JOIN (SELECT nvl(g_1.e2, 1) AS c_0 FROM pm1.g2 AS g_1) AS v_0 ON g_0.e2 = v_0.c_0" }, //$NON-NLS-1$
                                          TestOptimizer.ComparisonMode.EXACT_COMMAND_STRING); 

        TestOptimizer.checkNodeTypes(plan, TestOptimizer.FULL_PUSHDOWN);                                    
        
    }
    
    /**
     * RuleCopyCriteria will remove the first join criteria and the source doesn't support the * function.  However we still
     * want the join to be pushed since it originally contained proper criteria.
     */
    public void testCopyCriteriaJoinPushed() throws Exception {
    	String sql = "select pm1.g1.e1 from pm1.g1, pm1.g2 where pm1.g1.e1 = pm1.g2.e1 and pm1.g1.e1 = 5 and pm1.g1.e2 * 5 = pm1.g2.e2";
    	
    	FakeMetadataFacade metadata = FakeMetadataFactory.example1();
        FakeMetadataObject g1 = metadata.getStore().findObject("pm1", FakeMetadataObject.MODEL); //$NON-NLS-1$
        g1.putProperty(FakeMetadataObject.Props.JOIN, Boolean.TRUE);
    	
    	ProcessorPlan plan = TestOptimizer.helpPlan(sql,metadata, 
    			new String[] { "SELECT g_0.e2, g_1.e2, g_0.e1 FROM pm1.g1 AS g_0, pm1.g2 AS g_1 WHERE (g_0.e1 = '5') AND (g_1.e1 = '5')" }); //$NON-NLS-1$
    	
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
                1,      // Select
                0,      // Sort
                0       // UnionAll
    	});
    }
        
}
