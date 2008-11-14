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

package com.metamatrix.query.sql.symbol;

import junit.framework.TestCase;

import com.metamatrix.common.types.DataTypeManager;
import com.metamatrix.core.util.UnitTestUtil;

public class TestConstant extends TestCase {

	// ################################## FRAMEWORK ################################
	
	public TestConstant(String name) { 
		super(name);
	}	
	
	// ################################## TEST HELPERS ################################

	public static final Constant sample1() { 
		String s = "the string"; //$NON-NLS-1$
        return new Constant(s);		
	}

	public static final Constant sample2() { 
		Integer i = new Integer(5);
        return new Constant(i);
	}
	
	// ################################## ACTUAL TESTS ################################
	
	public void testString() {
		String s = "the string"; //$NON-NLS-1$
        Constant c = new Constant(s);
        assertEquals("Value is incorrect: ", s, c.getValue()); //$NON-NLS-1$
        assertEquals("Type is incorrect: ", DataTypeManager.DefaultDataClasses.STRING, c.getType()); //$NON-NLS-1$
        assertEquals("Should be non-null: ", false, c.isNull()); //$NON-NLS-1$
        assertEquals("Is not resolved: ", true, c.isResolved()); //$NON-NLS-1$
        assertEquals("Object does not equal itself", c, c); //$NON-NLS-1$
        
        Constant c2 = new Constant(s);
        assertEquals("Constants for same object aren't equal: ", c, c2); //$NON-NLS-1$
        
        Constant cc = (Constant) c.clone();
        assertEquals("Cloned object not equal to original: ", c, cc); //$NON-NLS-1$
	}

	public void testInteger() {
		Integer i = new Integer(5);
        Constant c = new Constant(i);
        assertEquals("Value is incorrect: ", i, c.getValue()); //$NON-NLS-1$
        assertEquals("Type is incorrect: ", DataTypeManager.DefaultDataClasses.INTEGER, c.getType()); //$NON-NLS-1$
        assertEquals("Should be non-null: ", false, c.isNull()); //$NON-NLS-1$
        assertEquals("Is not resolved: ", true, c.isResolved()); //$NON-NLS-1$
        assertEquals("Object does not equal itself", c, c); //$NON-NLS-1$
        
        Constant c2 = new Constant(i);
        assertEquals("Constants for same object aren't equal: ", c, c2); //$NON-NLS-1$
        
        Constant cc = (Constant) c.clone();
        assertEquals("Cloned object not equal to original: ", c, cc); //$NON-NLS-1$
	}

	public void testNoTypeNull() {
        Constant c = new Constant(null);
        assertEquals("Value is incorrect: ", null, c.getValue()); //$NON-NLS-1$
        assertEquals("Type is incorrect: ", DataTypeManager.DefaultDataClasses.NULL, c.getType()); //$NON-NLS-1$
        assertEquals("Should be null: ", true, c.isNull()); //$NON-NLS-1$
        assertEquals("Is not resolved: ", true, c.isResolved()); //$NON-NLS-1$
        assertEquals("Object does not equal itself", c, c); //$NON-NLS-1$
        
        Constant c2 = new Constant(null);
        assertEquals("Constants for same object aren't equal: ", c, c2); //$NON-NLS-1$
        
        Constant cc = (Constant) c.clone();
        assertEquals("Cloned object not equal to original: ", c, cc); //$NON-NLS-1$
    }

	public void testTypedNull() {
        Constant c = new Constant(null, DataTypeManager.DefaultDataClasses.STRING);
        assertEquals("Value is incorrect: ", null, c.getValue()); //$NON-NLS-1$
        assertEquals("Type is incorrect: ", DataTypeManager.DefaultDataClasses.STRING, c.getType()); //$NON-NLS-1$
        assertEquals("Should be null: ", true, c.isNull()); //$NON-NLS-1$
        assertEquals("Is not resolved: ", true, c.isResolved()); //$NON-NLS-1$
        assertEquals("Object does not equal itself", c, c); //$NON-NLS-1$
        
        Constant c2 = new Constant(null, DataTypeManager.DefaultDataClasses.STRING);
        assertEquals("Constants for same object aren't equal: ", c, c2); //$NON-NLS-1$
        
        Constant cc = (Constant) c.clone();
        assertEquals("Cloned object not equal to original: ", c, cc); //$NON-NLS-1$

        Constant c3 = new Constant(null);
        assertEquals("Typed null not equal to non-typed null: ", c, c3); //$NON-NLS-1$
    }
        
	public void testClone() { 
	    // Use this object as the "object"-type value for c1
	    StringBuffer value = new StringBuffer("x"); //$NON-NLS-1$
	    
		Constant c1 = new Constant(value, DataTypeManager.DefaultDataClasses.OBJECT);
		Constant copy = (Constant) c1.clone();			
		
		// Check equality
        assertEquals("Cloned object not equal to original: ", c1, copy); //$NON-NLS-1$
		
		// Check that modifying original value changes c1 and clone - this is expected as Constant 
		// uses a shallow clone
		value.append("y"); //$NON-NLS-1$
		
		assertTrue("Original object has not changed, but should have", ((StringBuffer)c1.getValue()).toString().equals("xy"));		 //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Cloned object has not changed, but should have", ((StringBuffer)copy.getValue()).toString().equals("xy"));						 //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testSelfEquivalence(){
		Object s1 = sample1();
		int equals = 0;
		UnitTestUtil.helpTestEquivalence(equals, s1, s1);
	}

	public void testEquivalence(){
		Object s1 = sample1();
		Object s1a = sample1();
		int equals = 0;
		UnitTestUtil.helpTestEquivalence(equals, s1, s1a);
	}
	
	public void testNonEquivalence(){
		Object s1 = sample1();
		Object s2 = sample2();
		int equals = -1;
		try{
            UnitTestUtil.helpTestEquivalence(equals, s1, s2);
        }catch(ClassCastException e) {
           // do nothing - this is caught because the method above compares two different objects
           // this exception should be thrown
        }
	}
    
    public void testCompareToString1() {
        Constant a = new Constant(new String("a")); //$NON-NLS-1$
        Constant b = new Constant(new String("b")); //$NON-NLS-1$
        int compare = a.compareTo(b);
        assertTrue("The String Values for these constants were incorrectly compared.", compare < 0); //$NON-NLS-1$
    }

    public void testCompareToString2() {
        Constant a = new Constant(new String("a")); //$NON-NLS-1$
        Constant b = new Constant(new String("a")); //$NON-NLS-1$
        int compare = a.compareTo(b);
        assertTrue("The String Values for these constants were incorrectly compared.", compare == 0); //$NON-NLS-1$
    }

    public void testCompareToString3() {
        Constant a = new Constant(new String("b")); //$NON-NLS-1$
        Constant b = new Constant(new String("a")); //$NON-NLS-1$
        int compare = a.compareTo(b);
        assertTrue("The String Values for these constants were incorrectly compared.", compare > 0); //$NON-NLS-1$
    }
    
    public void testCompareToInt1() {
        Constant a = new Constant(new Integer(1)); 
        Constant b = new Constant(new Integer(2)); 
        int compare = a.compareTo(b);
        assertTrue("The String Values for these constants were incorrectly compared.", compare < 0); //$NON-NLS-1$

    }

    public void testCompareToInt2() {
        Constant a = new Constant(new Integer(2)); 
        Constant b = new Constant(new Integer(2)); 
        int compare = a.compareTo(b);
        assertTrue("The String Values for these constants were incorrectly compared.", compare == 0); //$NON-NLS-1$

    }

    public void testCompareToInt3() {
        Constant a = new Constant(new Integer(2)); 
        Constant b = new Constant(new Integer(1)); 
        int compare = a.compareTo(b);
        assertTrue("The String Values for these constants were incorrectly compared.", compare > 0); //$NON-NLS-1$
    }
    
    public void testCompareToDifferingTypes() {
        Constant a = new Constant(new Integer(2)); 
        Constant b = new Constant(null); 
        try{
            a.compareTo(b);
        }catch(ClassCastException e) {
            //do nothing this should happen
        }        
    }
    
    public void testCompareToNull1() {
        Constant a = new Constant(null, Integer.class); 
        Constant b = new Constant(new Integer(1)); 
        int compare = a.compareTo(b);
        assertTrue("The String Values for these constants were incorrectly compared.", compare < 0); //$NON-NLS-1$        
    }
    
    public void testCompareToNull2() {
        Constant a = new Constant(new Integer(1)); 
        Constant b = new Constant(null, Integer.class); 
        int compare = a.compareTo(b);
        assertTrue("The String Values for these constants were incorrectly compared.", compare > 0); //$NON-NLS-1$        
    }

}
