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

import java.util.*;
import junit.framework.*;
import com.metamatrix.core.util.UnitTestUtil;
import com.metamatrix.query.sql.symbol.*;

public class TestDrop extends TestCase {

	// ################################## FRAMEWORK ################################
	
	public TestDrop(String name) { 
		super(name);
	}	
	
	// ################################## TEST HELPERS ################################	

	public static final Drop sample1() { 
        Drop Drop = new Drop();
        Drop.setTable(new GroupSymbol("temp_table"));//$NON-NLS-1$
        
		List elements = new ArrayList();
        elements.add(new ElementSymbol("a")); //$NON-NLS-1$
        elements.add(new ElementSymbol("b")); //$NON-NLS-1$
	    return Drop;	
	}

	public static final Drop sample2() { 
        Drop Drop = new Drop();
        Drop.setTable(new GroupSymbol("temp_table2"));//$NON-NLS-1$
        
        List elements = new ArrayList();
        elements.add(new ElementSymbol("a")); //$NON-NLS-1$
        elements.add(new ElementSymbol("b")); //$NON-NLS-1$
        return Drop;  	}
			
	// ################################## ACTUAL TESTS ################################
	
	public void testGetProjectedNoElements() {    
	    assertEquals(Command.getUpdateCommandSymbol(), sample1().getProjectedSymbols());
    }

	public void testSelfEquivalence(){
		Drop c1 = sample1();
		int equals = 0;
		UnitTestUtil.helpTestEquivalence(equals, c1, c1);
	}

	public void testEquivalence(){
		Drop c1 = sample1();
        Drop c2 = sample1();
		int equals = 0;
		UnitTestUtil.helpTestEquivalence(equals, c1, c2);
	}
	
	public void testNonEquivalence(){
        Drop c1 = sample1();
        Drop c2 = sample2();
		int equals = -1;
		UnitTestUtil.helpTestEquivalence(equals, c1, c2);
	}	
	
}
