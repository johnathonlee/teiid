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

package org.teiid.query.processor.xml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.teiid.common.buffer.BufferManager;
import org.teiid.common.buffer.BufferManagerFactory;
import org.teiid.core.TeiidComponentException;
import org.teiid.query.mapping.xml.ResultSetInfo;
import org.teiid.query.processor.xml.PlanExecutor;
import org.teiid.query.processor.xml.XMLProcessorEnvironment;


/**
 */
public class FakeXMLProcessorEnvironment extends XMLProcessorEnvironment {

    private Map dataMap = new HashMap();
    private Map schemaMap = new HashMap();


    public void addData(String resultSetName, List schema, List[] rows) {
        dataMap.put(resultSetName, rows);
        schemaMap.put(resultSetName, schema);
    }

    public PlanExecutor createResultExecutor(final String resultSetName, ResultSetInfo info) 
        throws TeiidComponentException{
       return new FakePlanExecutor(resultSetName, (List)schemaMap.get(resultSetName), (List[])dataMap.get(resultSetName));
    }
        
    /**
     * @see ProcessorEnvironment#clone()
     */
    public Object clone() {
        FakeXMLProcessorEnvironment clone = new FakeXMLProcessorEnvironment();
        super.copyIntoClone(clone);
        clone.dataMap = this.dataMap;
        clone.schemaMap = this.schemaMap;
        return clone;
    }
    
    @Override
    public BufferManager getBufferManager() {
    	return BufferManagerFactory.getStandaloneBufferManager();
    }
}
