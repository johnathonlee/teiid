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

package com.metamatrix.query.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import com.metamatrix.common.buffer.BlockedException;
import com.metamatrix.common.buffer.BufferManager;
import com.metamatrix.common.buffer.BufferManagerFactory;
import com.metamatrix.common.buffer.TupleBatch;
import com.metamatrix.common.buffer.TupleSource;
import com.metamatrix.common.buffer.TupleSourceID;
import com.metamatrix.common.buffer.BufferManager.TupleSourceType;
import com.metamatrix.common.types.DataTypeManager;
import com.metamatrix.core.MetaMatrixCoreException;
import com.metamatrix.query.sql.symbol.ElementSymbol;
import com.metamatrix.query.sql.symbol.SingleElementSymbol;
import com.metamatrix.query.util.CommandContext;

/**
 */
public class TestQueryProcessor extends TestCase {

    /**
     * Constructor for TestQueryProcessor.
     * @param name
     */
    public TestQueryProcessor(String name) {
        super(name);
    }
    
    public void helpTestProcessor(FakeProcessorPlan plan, long timeslice, List[] expectedResults) throws MetaMatrixCoreException {
        BufferManager bufferMgr = BufferManagerFactory.getStandaloneBufferManager();
        List schema = plan.getSchema();
        ArrayList typeNames = new ArrayList();
        for(Iterator s = schema.iterator(); s.hasNext();) {
            SingleElementSymbol es = (SingleElementSymbol)s.next();            
            typeNames.add(DataTypeManager.getDataTypeName(es.getType()));
        }
        String[] types = (String[])typeNames.toArray(new String[typeNames.size()]);              
        TupleSourceID tsID = bufferMgr.createTupleSource(plan.getSchema(), types, "group", TupleSourceType.FINAL); //$NON-NLS-1$
        FakeDataManager dataManager = new FakeDataManager();

        CommandContext context = new CommandContext("pid", "group", tsID, 100, null, null, null, null); //$NON-NLS-1$ //$NON-NLS-2$
        QueryProcessor processor = new QueryProcessor(plan, context, bufferMgr, dataManager);
                 
        while(true) {
            try {
                boolean done = processor.process(timeslice);
                if(done) {
                    break;
                }
            } catch(BlockedException e) {
            }
        }
        
        // Compare # of rows in actual and expected
        assertEquals("Did not get expected # of rows", expectedResults.length, bufferMgr.getFinalRowCount(tsID)); //$NON-NLS-1$
        
        // Compare actual with expected results
        TupleSource actual = bufferMgr.getTupleSource(tsID);
        if(expectedResults.length > 0) {
            for(int i=0; i<expectedResults.length; i++) {
                List actRecord = actual.nextTuple();
                List expRecord = expectedResults[i];                    
                assertEquals("Did not match row at row index " + i, expRecord, actRecord); //$NON-NLS-1$
            }
        }
    }
    
    public void testNoResults() throws Exception {
        List elements = new ArrayList();
        elements.add(new ElementSymbol("a")); //$NON-NLS-1$
        FakeProcessorPlan plan = new FakeProcessorPlan(elements, null);
        helpTestProcessor(plan, 1000, new List[0]);    
    }

    public void testBlockNoResults() throws Exception {
        List elements = new ArrayList();
        elements.add(new ElementSymbol("a")); //$NON-NLS-1$
        
        List batches = new ArrayList();
        batches.add(BlockedException.INSTANCE);
        TupleBatch batch = new TupleBatch(1, new List[0]);
        batch.setTerminationFlag(true);
        batches.add(batch);
        
        FakeProcessorPlan plan = new FakeProcessorPlan(elements, batches);
        helpTestProcessor(plan, 1000, new List[0]);    
    }
    
    public void testProcessWithOccasionalBlocks() throws Exception {
        List elements = new ArrayList();
        elements.add(new ElementSymbol("a")); //$NON-NLS-1$
                
        HashSet blocked = new HashSet(Arrays.asList(new Integer[] { new Integer(0), new Integer(2), new Integer(7) }));
        int numBatches = 10;
        int batchRow = 1;        
        int rowsPerBatch = 50;
        List[] expectedResults = new List[rowsPerBatch*(numBatches-blocked.size())];
        List batches = new ArrayList();
        for(int b=0; b<numBatches; b++) {
            if(blocked.contains(new Integer(b))) {
                batches.add(BlockedException.INSTANCE);
            } else {    
                List[] rows = new List[rowsPerBatch];
                for(int i=0; i<rowsPerBatch; i++) {
                    rows[i] = new ArrayList();
                    rows[i].add(new Integer(batchRow));
                    expectedResults[batchRow-1] = rows[i];
                    batchRow++;
                }
                                                
                TupleBatch batch = new TupleBatch(batchRow-rows.length, rows);
                if(b == numBatches-1) {
                    batch.setTerminationFlag(true);
                } 
                batches.add(batch);
            }
        }
        
        FakeProcessorPlan plan = new FakeProcessorPlan(elements, batches);
        helpTestProcessor(plan, 1000, expectedResults);                    
    }
    
    public void testProcessWhenClientNeedsBatch() throws Exception {
        List elements = new ArrayList();
        elements.add(new ElementSymbol("a")); //$NON-NLS-1$
                
        HashSet blocked = new HashSet(Arrays.asList(new Integer[] { new Integer(2)}));
        int numBatches = 5;
        int batchRow = 1;        
        int rowsPerBatch = 10;
        List[] expectedResults = new List[rowsPerBatch*(numBatches-blocked.size())];
        List batches = new ArrayList();
        for(int b=0; b<numBatches; b++) {
            if(blocked.contains(new Integer(b))) {
                batches.add(BlockedException.INSTANCE);
            } else {    
                List[] rows = new List[rowsPerBatch];
                for(int i=0; i<rowsPerBatch; i++) {
                    rows[i] = new ArrayList();
                    rows[i].add(new Integer(batchRow));
                    expectedResults[batchRow-1] = rows[i];
                    batchRow++;
                }
                                                
                TupleBatch batch = new TupleBatch(batchRow-rows.length, rows);
                if(b == numBatches-1) {
                    batch.setTerminationFlag(true);
                } 
                batches.add(batch);
            }
        }
        
        final FakeProcessorPlan plan = new FakeProcessorPlan(elements, batches);
        
        BufferManager bufferMgr = BufferManagerFactory.getStandaloneBufferManager();
        List schema = plan.getOutputElements();
        ArrayList typeNames = new ArrayList();
        for(Iterator s = schema.iterator(); s.hasNext();) {
            SingleElementSymbol es = (SingleElementSymbol)s.next();            
            typeNames.add(DataTypeManager.getDataTypeName(es.getType()));
        }
        String[] types = (String[])typeNames.toArray(new String[typeNames.size()]);              
        TupleSourceID tsID = bufferMgr.createTupleSource(plan.getSchema(), types, "group", TupleSourceType.FINAL); //$NON-NLS-1$
        FakeDataManager dataManager = new FakeDataManager();

        CommandContext context = new CommandContext("pid", "group", tsID, 100, null, null, null, null); //$NON-NLS-1$ //$NON-NLS-2$
        final QueryProcessor processor = new QueryProcessor(plan, context, bufferMgr, dataManager);
        
        processor.setBatchHandler(new QueryProcessor.BatchHandler() {
        	
        	int count = 0;
        	
			public void batchProduced(TupleBatch batch)
					throws MetaMatrixCoreException {
		
				assertEquals(++count, plan.batchIndex);
				if (count == 2) {
					processor.closeProcessing();
				}
			}
        });
        
        // Give the processor plenty of time to process
        processor.process(30000);
    }
}
