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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.metamatrix.connector.language.IDelete;
import com.metamatrix.connector.language.IInsert;
import com.metamatrix.connector.language.IUpdate;
import com.metamatrix.query.sql.lang.BatchedUpdateCommand;


/** 
 * @since 4.2
 */
public class TestBatchedUpdatesImpl extends TestCase {

    public TestBatchedUpdatesImpl(String name) {
        super(name);
    }

    public static BatchedUpdateCommand helpExample() {
        List updates = new ArrayList();
        updates.add(TestInsertImpl.helpExample("a.b")); //$NON-NLS-1$
        updates.add(TestUpdateImpl.helpExample());
        updates.add(TestDeleteImpl.helpExample());
        return new BatchedUpdateCommand(updates);
    }
    
    public static BatchedUpdatesImpl example() throws Exception {
        return (BatchedUpdatesImpl)TstLanguageBridgeFactory.factory.translate(helpExample());
    }

    public void testGetUpdateCommands() throws Exception {
        List updates = example().getUpdateCommands();
        assertEquals(3, updates.size());
        assertTrue(updates.get(0) instanceof IInsert);
        assertTrue(updates.get(1) instanceof IUpdate);
        assertTrue(updates.get(2) instanceof IDelete);
    }

}
