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

package com.metamatrix.dqp.internal.datamgr.language;

import java.util.Iterator;

import com.metamatrix.connector.language.ICriteria;
import com.metamatrix.query.sql.lang.CompareCriteria;
import com.metamatrix.query.sql.lang.CompoundCriteria;

import junit.framework.TestCase;

public class TestCompoundCriteriaImpl extends TestCase {

    /**
     * Constructor for TestCompoundCriteriaImpl.
     * @param name
     */
    public TestCompoundCriteriaImpl(String name) {
        super(name);
    }

    public static CompoundCriteria helpExample(int operator) {
        CompareCriteria c1 = TestCompareCriteriaImpl.helpExample(CompareCriteria.GE, 100, 200);
        CompareCriteria c2 = TestCompareCriteriaImpl.helpExample(CompareCriteria.LT, 500, 600);
        return new CompoundCriteria(operator, c1, c2);
    }
    
    public static CompoundCriteriaImpl example(int operator) throws Exception {
        return (CompoundCriteriaImpl)TstLanguageBridgeFactory.factory.translate(helpExample(operator));
    }

    public void testGetOperator() throws Exception {
        assertEquals(CompoundCriteriaImpl.AND, example(CompoundCriteria.AND).getOperator());
        assertEquals(CompoundCriteriaImpl.OR, example(CompoundCriteria.OR).getOperator());
    }

    public void testGetCriteria() throws Exception {
        CompoundCriteriaImpl cc = example(CompoundCriteria.AND);
        assertNotNull(cc.getCriteria());
        assertEquals(2, cc.getCriteria().size());
        for (Iterator i = cc.getCriteria().iterator(); i.hasNext();) {
            assertTrue(i.next() instanceof ICriteria);
        }
    }

}
