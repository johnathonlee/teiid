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
package com.metamatrix.connector.salesforce.execution;

import java.util.List;

import junit.framework.Test;

import com.metamatrix.data.exception.ConnectorException;

public class InsertExecutionTest extends BaseExecutionTest {
	
	// This test break all kinds of testing rules.
	
	public void testInsertContact() {
		try {
			
			List result = host.executeCommand("DELETE FROM Contact WHERE LastName = 'Girlfriend'");
			result = host.executeCommand("DELETE FROM Contact WHERE LastName = 'Monarch'");
			
			result = host.executeCommand("INSERT INTO Contact (Salutation, LastName) VALUES ('Dr.', 'Girlfriend')");
			assertEquals(1, result.size());
			List row = (List)result.get(0);
			assertEquals(1, row.size());
			assertEquals("1", ((Integer)row.get(0)).toString());
			
			result = host.executeCommand("INSERT INTO Contact (Salutation, LastName) VALUES ('The', 'Monarch')");
			assertEquals(1, result.size());
			row = (List)result.get(0);
			assertEquals(1, row.size());
			assertEquals("1", ((Integer)row.get(0)).toString());
			
			result = host.executeCommand("DELETE FROM Contact WHERE LastName = 'Girlfriend'");
			assertEquals(1, result.size());
			row = (List)result.get(0);
			assertEquals(1, row.size());
			assertEquals("1", ((Integer)row.get(0)).toString());
			
			result = host.executeCommand("DELETE FROM Contact WHERE LastName = 'Monarch'");
			assertEquals(1, result.size());
			row = (List)result.get(0);
			assertEquals(1, row.size());
			assertEquals("1", ((Integer)row.get(0)).toString());
			
		} catch (ConnectorException e) {
			fail(e.getMessage());
		}
	}

}
