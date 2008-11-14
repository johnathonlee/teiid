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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.metamatrix.core.util.UnitTestUtil;
import com.metamatrix.query.sql.symbol.ElementSymbol;
import com.metamatrix.query.sql.symbol.GroupSymbol;

public class TestCreate extends TestCase {

	// ################################## FRAMEWORK ################################
	
	public TestCreate(String name) { 
		super(name);
	}	
	
	// ################################## TEST HELPERS ################################	

	public static final Create sample1() { 
        Create create = new Create();
        create.setTable(new GroupSymbol("temp_table"));//$NON-NLS-1$
        
		List elements = new ArrayList();
        elements.add(new ElementSymbol("a")); //$NON-NLS-1$
        elements.add(new ElementSymbol("b")); //$NON-NLS-1$

	    create.setColumns(elements);
	    return create;	
	}

	public static final Create sample2() { 
        Create create = new Create();
        create.setTable(new GroupSymbol("temp_table2"));//$NON-NLS-1$
        
        List elements = new ArrayList();
        elements.add(new ElementSymbol("a")); //$NON-NLS-1$
        elements.add(new ElementSymbol("b")); //$NON-NLS-1$

        create.setColumns(elements);
        return create;  	}
			
	// ################################## ACTUAL TESTS ################################
	
	public void testGetProjectedNoElements() {    
	    assertEquals(Command.getUpdateCommandSymbol(), sample1().getProjectedSymbols());
    }

	public void testSelfEquivalence(){
		Create c1 = sample1();
		int equals = 0;
		UnitTestUtil.helpTestEquivalence(equals, c1, c1);
	}

	public void testEquivalence(){
		Create c1 = sample1();
        Create c2 = sample1();
		int equals = 0;
		UnitTestUtil.helpTestEquivalence(equals, c1, c2);
	}
	
	public void testNonEquivalence(){
        Create c1 = sample1();
        Create c2 = sample2();
		int equals = -1;
		UnitTestUtil.helpTestEquivalence(equals, c1, c2);
	}

}
