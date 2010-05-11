/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
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

package org.teiid.dqp.internal.datamgr.language;

import junit.framework.TestCase;

import org.teiid.connector.language.ColumnReference;
import org.teiid.connector.language.DerivedColumn;
import org.teiid.core.types.DataTypeManager;


public class TestSelectSymbolImpl extends TestCase {

    /**
     * Constructor for TestSelectSymbolImpl.
     * @param name
     */
    public TestSelectSymbolImpl(String name) {
        super(name);
    }

    public static DerivedColumn example(String symbolName, String alias) throws Exception {
        DerivedColumn selectSymbol = new DerivedColumn(alias, new ColumnReference(null, symbolName, null, DataTypeManager.DefaultDataClasses.INTEGER));
        return selectSymbol;
    }

    public void testHasAlias() throws Exception {
        assertNotNull(example("testName", "testAlias").getAlias()); //$NON-NLS-1$ //$NON-NLS-2$
        assertNull(example("testName", null).getAlias()); //$NON-NLS-1$
    }

    public void testGetOutputName() throws Exception {
        assertEquals("testAlias", example("testName", "testAlias").getAlias()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    public void testGetExpression() throws Exception {
        assertNotNull(example("testName", null).getExpression()); //$NON-NLS-1$
    }

}
