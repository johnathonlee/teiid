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

package com.metamatrix.query.sql.lang;

import junit.framework.*;

import com.metamatrix.core.util.UnitTestUtil;
import com.metamatrix.query.sql.symbol.Constant;
import com.metamatrix.query.sql.symbol.ElementSymbol;

/**
 * @author amiller
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class TestCompoundCriteria extends TestCase {

    /**
     * Constructor for TestCompoundCriteria.
     * @param name
     */
    public TestCompoundCriteria(String name) {
        super(name);
    }

    public void testClone1() {
        ElementSymbol e1 = new ElementSymbol("e1"); //$NON-NLS-1$
        CompareCriteria ccrit1 = new CompareCriteria(e1, CompareCriteria.EQ, new Constant("abc")); //$NON-NLS-1$
        ElementSymbol e2 = new ElementSymbol("e2"); //$NON-NLS-1$
        CompareCriteria ccrit2 = new CompareCriteria(e2, CompareCriteria.EQ, new Constant("xyz")); //$NON-NLS-1$
        CompoundCriteria comp = new CompoundCriteria(CompoundCriteria.AND, ccrit1, ccrit2);
        
        UnitTestUtil.helpTestEquivalence(0, comp, comp.clone());        
    }
    
    public void testClone2() {
        ElementSymbol e1 = new ElementSymbol("e1"); //$NON-NLS-1$
        CompareCriteria ccrit1 = new CompareCriteria(e1, CompareCriteria.EQ, new Constant("abc")); //$NON-NLS-1$
        CompoundCriteria comp = new CompoundCriteria(CompoundCriteria.AND, ccrit1, null);
        
        UnitTestUtil.helpTestEquivalence(0, comp, comp.clone());        
    }
    
}
