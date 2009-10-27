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

package com.metamatrix.common.buffer.impl;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.junit.Test;

import com.metamatrix.api.exception.MetaMatrixComponentException;
import com.metamatrix.api.exception.MetaMatrixProcessingException;
import com.metamatrix.common.buffer.BlockedOnMemoryException;
import com.metamatrix.common.buffer.BufferManager;
import com.metamatrix.common.buffer.BufferManagerPropertyNames;
import com.metamatrix.common.buffer.MemoryNotAvailableException;
import com.metamatrix.common.buffer.StorageManager;
import com.metamatrix.common.buffer.TupleBatch;
import com.metamatrix.common.buffer.TupleSource;
import com.metamatrix.common.buffer.TupleSourceID;
import com.metamatrix.common.buffer.TupleSourceNotFoundException;
import com.metamatrix.common.buffer.BufferManager.TupleSourceStatus;
import com.metamatrix.common.buffer.BufferManager.TupleSourceType;
import com.metamatrix.common.buffer.BufferManagerFactory.MemoryStorageManager;
import com.metamatrix.common.lob.ByteLobChunkStream;
import com.metamatrix.common.lob.LobChunk;
import com.metamatrix.common.lob.LobChunkInputStream;
import com.metamatrix.common.lob.LobChunkProducer;
import com.metamatrix.common.types.DataTypeManager;
import com.metamatrix.common.types.SQLXMLImpl;
import com.metamatrix.common.types.XMLType;
import com.metamatrix.core.util.UnitTestUtil;
import com.metamatrix.query.sql.symbol.ElementSymbol;

/**
 */
public class TestBufferManagerImpl {

    public static BufferManager getTestBufferManager(long bytesAvailable, StorageManager sm2) throws MetaMatrixComponentException {
        // Get the properties for BufferManager
        Properties bmProps = new Properties();                        
        bmProps.setProperty(BufferManagerPropertyNames.MEMORY_AVAILABLE, String.valueOf(bytesAvailable));
        bmProps.setProperty(BufferManagerPropertyNames.MANAGEMENT_INTERVAL, "0"); //$NON-NLS-1$
        BufferManager bufferManager = new BufferManagerImpl();
        bufferManager.initialize("local", bmProps); //$NON-NLS-1$

        if(sm2 != null) { 
            bufferManager.setStorageManager(sm2);
        }

        return bufferManager;
    }
    
    private StorageManager createFakeDatabaseStorageManager() {
        return new MemoryStorageManager();        
    }
    
    public void helpTestAddBatches(StorageManager sm2, int memorySize, int numBatches, int rowsPerBatch) throws MetaMatrixComponentException, MetaMatrixProcessingException {
        BufferManager mgr = getTestBufferManager(memorySize, sm2);

        List expectedRows = new ArrayList();

        List<ElementSymbol> schema = Arrays.asList(new ElementSymbol("col")); //$NON-NLS-1$
        schema.get(0).setType(DataTypeManager.DefaultDataClasses.STRING);
        
        TupleSourceID tsID = mgr.createTupleSource(schema, null, TupleSourceType.PROCESSOR);        
        
        long batchSize = -1;
        for(int b=0; b<numBatches; b++) {
            List rows = new ArrayList();
            for(int r=0; r<rowsPerBatch; r++) {
                List row = new ArrayList();                    
                String rowValue = "" + ((b*rowsPerBatch) + r + 1000); //$NON-NLS-1$
                row.add(rowValue.substring(rowValue.length()-4));
                rows.add(row);
            }
                            
            TupleBatch batch = new TupleBatch( (b*rowsPerBatch) + 1, rows );                            
            mgr.addTupleBatch(tsID, batch);    
            
            // Remember batch size
            batchSize = SizeUtility.getSize(rows);
            
            // Collect into expected results
            expectedRows.addAll(rows);
        }    
        
        // Close the tuple source
        mgr.setStatus(tsID, TupleSourceStatus.FULL);
        
        
        // Check batches in sm1 - 3 should fit in 1000 bytes of memory
        int batchesInMemory = (int) (memorySize * 1000000 / batchSize);
        if (sm2 != null) {
	        for(int b=0; b<batchesInMemory; b++) {
	            int begin = (b*rowsPerBatch) + 1;
	            try {
	            	sm2.getBatch(tsID, begin, null);
            		fail("Expected exception"); //$NON-NLS-1$
	            } catch (TupleSourceNotFoundException e) {
	            	
	            } catch (MetaMatrixComponentException e) {
	            	
	            }
	        }
	        
	        // Check batches in sm2
	        for(int b=batchesInMemory+1; b<numBatches; b++) {
	            int begin = (b*rowsPerBatch) + 1;
	            
	            TupleBatch batch = sm2.getBatch(tsID, begin, null);
	            assertNotNull("batch " + (b+1) + " is null ", batch); //$NON-NLS-1$ //$NON-NLS-2$
	        }
    	}
        
        
        // Check row count
        int rowCount = mgr.getRowCount(tsID);
        assertEquals("Incorrect row count", numBatches * rowsPerBatch, rowCount); //$NON-NLS-1$
        
        // Compare against tuple source
        TupleSource ts = mgr.getTupleSource(tsID);
        List tuple = ts.nextTuple();
        int count = 0;
        while(tuple != null) { 
            assertEquals("Row " + count + " doesn't match ", expectedRows.get(count), tuple); //$NON-NLS-1$ //$NON-NLS-2$
            tuple = ts.nextTuple();   
            count++;            
        }            
        ts.closeSource();
    }    

    @Test public void testSpanStorage() throws Exception {
        helpTestAddBatches(createFakeDatabaseStorageManager(), 
                           1, 
                           50, 
                           100);    
    }
 
    @Test public void testStandalone() throws Exception {
        helpTestAddBatches(null, 
                           10, 
                           5, 
                           10);    
    }
 
    public TupleBatch exampleBatch(int begin, int end) {
        int count = end-begin+1;
        List[] rows = new List[count];
        for(int i=0; i < count; i++) {
            rows[i] = new ArrayList();
            rows[i].add(new Integer(i+begin));
            rows[i].add("" + (i+begin));     //$NON-NLS-1$
        }
        return new TupleBatch(begin, rows);        
    }

    public void helpCompareBatches(TupleBatch expectedBatch, TupleBatch actualBatch) {
        List[] expectedRows = expectedBatch.getAllTuples();
        List[] actualRows = actualBatch.getAllTuples();

        assertEquals("Differing number of rows ", expectedRows.length, actualRows.length); //$NON-NLS-1$
        for(int i=0; i<expectedRows.length; i++) {
            assertEquals("Differing rows at " + i, expectedRows[i], actualRows[i]);     //$NON-NLS-1$
        }
    }
    
    public List[] helpGetRows(TupleBatch batch, int begin, int end) {
        List[] allRows = batch.getAllTuples();
        if(begin == batch.getBeginRow() && end == batch.getEndRow()) {
            return allRows;
        }
        int firstOffset = begin - batch.getBeginRow();
        int count = end - begin + 1;
    
        List[] subRows = new List[count];
        System.arraycopy(allRows, firstOffset, subRows, 0, count);
        return subRows;
    }
    
    @Test public void testCreateLobReference() throws Exception {
        final BufferManagerImpl mgr = (BufferManagerImpl)getTestBufferManager(1, createFakeDatabaseStorageManager());

        XMLType xml1 = new XMLType(new SQLXMLImpl("<foo/>")); //$NON-NLS-1$
        XMLType xml2 = new XMLType(new SQLXMLImpl("<bar/>")); //$NON-NLS-1$
        List<ElementSymbol> schema = Arrays.asList(new ElementSymbol("col")); //$NON-NLS-1$
        schema.get(0).setType(DataTypeManager.DefaultDataClasses.XML);
                
        final TupleSourceID id = mgr.createTupleSource(schema, "GROUP1", TupleSourceType.PROCESSOR); //$NON-NLS-1$
        
        List xmlList1 = new ArrayList();
        xmlList1.add(xml1);
        List xmlList2 = new ArrayList();
        xmlList2.add(xml2);
        
        TupleBatch batch = new TupleBatch(1, new List[] {xmlList1, xmlList2});
        mgr.addTupleBatch(id, batch);
        
        // when adding to the tuple source the reference id is assigned
        // but not the persistence stream id.
        assertNotNull(xml1.getReferenceStreamId());
        assertNotNull(xml2.getReferenceStreamId());
        assertNull(xml1.getPersistenceStreamId());
        assertNull(xml2.getPersistenceStreamId());
        
        assertNotNull(mgr.getStreamable(id, xml1.getReferenceStreamId()));
        
        final TupleSourceID id1 = mgr.createTupleSource(schema, "GROUP1", TupleSourceType.PROCESSOR); //$NON-NLS-1$
        
        TupleBatch batch1 = new TupleBatch(1, new List[] {xmlList1, xmlList2});
        mgr.addTupleBatch(id1, batch1);

        //assure that even though we've removed a later tuple source, the reference still exists
        mgr.removeTupleSource(id1);
        assertNotNull(mgr.getStreamable(id, xml1.getReferenceStreamId()));
    }
    
    @Test public void testAddStreamablePart() throws Exception {
         final BufferManager mgr = getTestBufferManager(1, createFakeDatabaseStorageManager());
        
        // save the lob
	     List<ElementSymbol> schema = Arrays.asList(new ElementSymbol("col")); //$NON-NLS-1$
	     schema.get(0).setType(DataTypeManager.DefaultDataClasses.XML);
           
        final TupleSourceID id = mgr.createTupleSource(schema, "GROUP1", TupleSourceType.PROCESSOR); //$NON-NLS-1$
        
        ByteLobChunkStream stream = new ByteLobChunkStream(new FileInputStream(UnitTestUtil.getTestDataPath()+"/LicenseMappingExample.xml"), 11); //$NON-NLS-1$
        int i = 1;
        LobChunk chunk = null;
        do {
            chunk = stream.getNextChunk();
            mgr.addStreamablePart(id, chunk, i);
            i++;
        }while(!chunk.isLast());
        
        
        // read the lob
        InputStreamReader reader = new InputStreamReader(new LobChunkInputStream(new LobChunkProducer() {
            int position = 0;
            public LobChunk getNextChunk() throws IOException {
                try {
                    position++;
                    return mgr.getStreamablePart(id, position);
                } catch (Exception e) {
                    fail(e.getMessage());
                }
                return null;
            }
            public void close() throws IOException {
            }            
        }));
        
        String actual = readFile(reader);
        String expected = readFile(new FileReader(UnitTestUtil.getTestDataPath()+"/LicenseMappingExample.xml")); //$NON-NLS-1$
        assertEquals(expected, actual);
        
        mgr.removeTupleSource(id);
        
        try {
            mgr.getStreamablePart(id, 0);
            fail("should have gone from bufffer manager by now"); //$NON-NLS-1$
        } catch (TupleSourceNotFoundException e) {
        } catch (MetaMatrixComponentException e) {
        }
    }
        
    private String readFile(Reader reader) throws IOException{
        StringBuffer sb = new StringBuffer();
        
        int chr = reader.read();
        while(chr != -1) {
            sb.append((char)chr);
            chr = reader.read();
        }
        reader.close();
        return sb.toString();
    }     
    
    
    @Test public void testPinning1() throws Exception {
        BufferManager mgr = getTestBufferManager(1, createFakeDatabaseStorageManager());
        
        List<ElementSymbol> schema = Arrays.asList(new ElementSymbol("val"), new ElementSymbol("col")); //$NON-NLS-1$ //$NON-NLS-2$
        schema.get(0).setType(DataTypeManager.DefaultDataClasses.INTEGER);
        schema.get(1).setType(DataTypeManager.DefaultDataClasses.STRING);
        String group = "test"; //$NON-NLS-1$
        TupleSourceID tsID = mgr.createTupleSource(schema, group, TupleSourceType.PROCESSOR);
        
        // Add some batches, 1000 at a time
        int maxRows = 4000;
        int writePerBatch = 1000;
        for(int i=1; i<maxRows; i+=writePerBatch) {                
            mgr.addTupleBatch(tsID, exampleBatch(i, i+writePerBatch-1));
        }
        
        // Walk through by 100's pinning and unpinning
        int readPerBatch = 1000;
        for(int i=1; i<maxRows; i=i+readPerBatch) {
            int end = i+readPerBatch-1;
            TupleBatch checkBatch = mgr.pinTupleBatch(tsID, i);
            helpCompareBatches(exampleBatch(i, end), checkBatch);
            mgr.unpinTupleBatch(tsID, i);
        }
        
        // Remove
        mgr.removeTupleSource(tsID);
             
    }

    @Test public void testUnpinOfUnpinnedBatch() throws Exception {
        BufferManager mgr = getTestBufferManager(1, createFakeDatabaseStorageManager());
        
        List<ElementSymbol> schema = Arrays.asList(new ElementSymbol("val"), new ElementSymbol("col")); //$NON-NLS-1$ //$NON-NLS-2$
        schema.get(0).setType(DataTypeManager.DefaultDataClasses.INTEGER);
        schema.get(1).setType(DataTypeManager.DefaultDataClasses.STRING);

        String group = "test"; //$NON-NLS-1$
        TupleSourceID tsID = mgr.createTupleSource(schema, group, TupleSourceType.PROCESSOR);
        
        // Add some batches, 1000 at a time
        int maxRows = 4000;
        int writePerBatch = 1000;
        for(int i=1; i<maxRows; i+=writePerBatch) {                
            mgr.addTupleBatch(tsID, exampleBatch(i, i+writePerBatch-1));
        }
        
        // Walk through by 100's unpinning each batch (but never pinning) - this *should* have no effect
        int readPerBatch = 100;
        for(int i=1; i<maxRows; i=i+readPerBatch) {
            int end = i+readPerBatch-1;
            mgr.unpinTupleBatch(tsID, i);
        }

        // Walk through by 100's pinning and unpinning
        helpTestPinAndUnpin(mgr, tsID, maxRows, writePerBatch, readPerBatch);

        // Remove
        mgr.removeTupleSource(tsID);
    }

	private void helpTestPinAndUnpin(BufferManager mgr, TupleSourceID tsID,
			int maxRows, int writePerBatch, int readPerBatch)
			throws TupleSourceNotFoundException, MemoryNotAvailableException,
			MetaMatrixComponentException {
		for(int i=1; i<maxRows; i=i+readPerBatch) {
            int end = (1+i/writePerBatch)*writePerBatch;
            int begin = end - writePerBatch + 1;
            TupleBatch checkBatch = mgr.pinTupleBatch(tsID, i);
            helpCompareBatches(exampleBatch(begin, end), checkBatch);
            mgr.unpinTupleBatch(tsID, i);
        }
	}
    
    
    private TupleBatch exampleBigBatch(int begin, int end, int charsPerRow) {        
        int count = end-begin+1;
        List[] rows = new List[count];
        
        StringBuffer s = new StringBuffer();
        for(int i=0; i<charsPerRow; i++) {
            s.append("A"); //$NON-NLS-1$
        }
        String bigString = s.toString();
        
        for(int i=0; i < count; i++) {
            rows[i] = new ArrayList();
            rows[i].add(bigString);
        }
        return new TupleBatch(begin, rows);        
    }

    @Test public void testDeadlockOnMultiThreadClean() throws Exception {
        BufferManager mgr = getTestBufferManager(1, createFakeDatabaseStorageManager());

        List<ElementSymbol> schema = Arrays.asList(new ElementSymbol("col")); //$NON-NLS-1$
        schema.get(0).setType(DataTypeManager.DefaultDataClasses.STRING);
        
        String group = "test"; //$NON-NLS-1$
        int count = 20;
        int pins = 50;
        int batches = 25;
        int rowsPerBatch = 100;
        int rowSize = 1000;
        
        TupleSourceID[] ids = new TupleSourceID[count];
        ReaderThread[] threads = new ReaderThread[count];
               
        // Setup
        for(int t=0; t<count; t++) {
            ids[t] = mgr.createTupleSource(schema, group, TupleSourceType.PROCESSOR);
            for(int i=1; i<(batches*rowsPerBatch); i=i+rowsPerBatch) {                
                mgr.addTupleBatch(ids[t], exampleBigBatch(i, i+rowsPerBatch-1, rowSize));
            }                            
            threads[t] = new ReaderThread(mgr, ids[t], pins, batches, rowsPerBatch);
        }
        
        // Start
        for(int t=0; t<count; t++) {
            threads[t].start();
        }
                       
        // Join and assert
        for(int t=0; t<count; t++) {
            threads[t].join();
            assertFalse("Thread " + ids[t] + " failed", threads[t].failed()); //$NON-NLS-1$ //$NON-NLS-2$
        }        
    }
    
    @Test public void testSessionMax_Fail() throws Exception {
        BufferManager mgr = getTestBufferManager(1, createFakeDatabaseStorageManager());
        TupleSourceID tsID = null;
        try {
            List<ElementSymbol> schema = Arrays.asList(new ElementSymbol("val"), new ElementSymbol("col")); //$NON-NLS-1$ //$NON-NLS-2$
            schema.get(0).setType(DataTypeManager.DefaultDataClasses.INTEGER);
            schema.get(1).setType(DataTypeManager.DefaultDataClasses.STRING);

            String group = "test"; //$NON-NLS-1$
            tsID = mgr.createTupleSource(schema, group, TupleSourceType.PROCESSOR);
            
            // Add some batches, 1000  at a time
            int maxRows = 10000;
            int writePerBatch = 1000;
            for(int i=1; i<maxRows; i+=writePerBatch) {                
                mgr.addTupleBatch(tsID, exampleBatch(i, i+writePerBatch-1));
            }
            
            // Walk through by 1000's pinning and unpinning
            int readPerBatch = 1000;
            for(int i=1; i<maxRows; i=i+readPerBatch) {
            	int end = (1+i/writePerBatch)*writePerBatch;
                TupleBatch checkBatch = mgr.pinTupleBatch(tsID, i);
                helpCompareBatches(exampleBatch(i, end), checkBatch);
            }
            fail("Should have failed"); //$NON-NLS-1$
        } catch (MemoryNotAvailableException e) {
            // Should catch this exception
        } finally {
            // Remove
            mgr.removeTupleSource(tsID);

        }
    }
    
    /**
     * Ensure that the buffer manager's internal tuplesource doesn't throw a MemoryNotAvailableException
     * when a pin fails, but rather converts it to a BlockedOnMemoryException 
     * @throws Exception
     * @since 4.3
     */
    @Test public void testDefect_18499() throws Exception {
        BufferManager mgr = getTestBufferManager(1, createFakeDatabaseStorageManager());
        List<ElementSymbol> schema = Arrays.asList(new ElementSymbol("col")); //$NON-NLS-1$
        schema.get(0).setType(DataTypeManager.DefaultDataClasses.STRING);
        
        TupleSourceID tsID = mgr.createTupleSource(schema, null, TupleSourceType.PROCESSOR);
        mgr.addTupleBatch(tsID, exampleBigBatch(1, 1000, 512));
        mgr.setStatus(tsID, TupleSourceStatus.FULL);
        TupleSource ts = mgr.getTupleSource(tsID);
        try {
            ts.nextTuple();
            fail("Should have failed attempt to pin the batch."); //$NON-NLS-1$
        } catch (BlockedOnMemoryException e) {
            // Success
        } finally {
            ts.closeSource();
            mgr.removeTupleSource(tsID);
        }
    }
    
    @Test public void testDefect18497() throws Exception {
        BufferManager mgr = getTestBufferManager(1, createFakeDatabaseStorageManager());
        TupleSourceID tsID = null;
        try {
            List<ElementSymbol> schema = Arrays.asList(new ElementSymbol("val"), new ElementSymbol("col")); //$NON-NLS-1$ //$NON-NLS-2$
            schema.get(0).setType(DataTypeManager.DefaultDataClasses.INTEGER);
            schema.get(1).setType(DataTypeManager.DefaultDataClasses.STRING);

            String group = "test"; //$NON-NLS-1$
            tsID = mgr.createTupleSource(schema, group, TupleSourceType.PROCESSOR);
            
            // Add some batches, 1000  at a time
            int maxRows = 10000;
            int writePerBatch = 1000;
            for(int i=1; i<maxRows; i+=writePerBatch) {                
                mgr.addTupleBatch(tsID, exampleBatch(i, i+writePerBatch-1));
            }
            
            // Walk through by 1000's pinning and unpinning
            int readPerBatch = 1000;
            helpTestPinAndUnpin(mgr, tsID, maxRows, writePerBatch, readPerBatch);
        } finally {
            // Remove
            mgr.removeTupleSource(tsID);

        }
    }
    
    //two threads do the cleaning at the same time
    @Test public void testDefect19325() throws Exception{
        BufferManagerImpl mgr = (BufferManagerImpl)getTestBufferManager(1, createFakeDatabaseStorageManager());
        TupleSourceID tsID = null;
        List<ElementSymbol> schema = Arrays.asList(new ElementSymbol("val"), new ElementSymbol("col")); //$NON-NLS-1$ //$NON-NLS-2$
        schema.get(0).setType(DataTypeManager.DefaultDataClasses.INTEGER);
        schema.get(1).setType(DataTypeManager.DefaultDataClasses.STRING);

        String group = "test1"; //$NON-NLS-1$
        tsID = mgr.createTupleSource(schema, group, TupleSourceType.PROCESSOR);
        
        TupleSourceID tsID2 = null;
        group = "test2"; //$NON-NLS-1$
        tsID2 = mgr.createTupleSource(schema, group, TupleSourceType.PROCESSOR);

        // Add some batches, 1000  at a time
        int maxRows = 50000;
        int writePerBatch = 100;
        for(int i=1; i<maxRows; i+=writePerBatch) {                
            mgr.addTupleBatch(tsID, exampleBatch(i, i+writePerBatch-1));
            mgr.addTupleBatch(tsID2, exampleBatch(i, i+writePerBatch-1));
        }

        new CleanThread(mgr, tsID).start();
        new CleanThread(mgr, tsID2).start();
        
        for(int i=1; i<maxRows; i+=writePerBatch) {                
            mgr.addTupleBatch(tsID, exampleBatch(i, i+writePerBatch-1));
            mgr.addTupleBatch(tsID2, exampleBatch(i, i+writePerBatch-1));
        }
    }
    
    //test many small batches
    @Test public void testSmallBatches() throws Exception{
        BufferManager mgr = getTestBufferManager(50, createFakeDatabaseStorageManager());
        TupleSourceID tsID = null;
        try {
            List<ElementSymbol> schema = Arrays.asList(new ElementSymbol("val"), new ElementSymbol("col")); //$NON-NLS-1$ //$NON-NLS-2$
            schema.get(0).setType(DataTypeManager.DefaultDataClasses.INTEGER);
            schema.get(1).setType(DataTypeManager.DefaultDataClasses.STRING);

            String group = "test"; //$NON-NLS-1$
            tsID = mgr.createTupleSource(schema, group, TupleSourceType.PROCESSOR);
            
            // Add some batches
            int maxRows = 10000;
            int writePerBatch = 1;
            for(int i=1; i<maxRows; i+=writePerBatch) {                
                mgr.addTupleBatch(tsID, exampleBatch(i, i+writePerBatch-1));
            }
            
            mgr.setStatus(tsID, TupleSourceStatus.FULL);

            TupleSource ts = mgr.getTupleSource(tsID); 
            int i=0;
            while(ts.nextTuple() != null){
                i++;
            }
            ts.closeSource();
            assertEquals(i, 9999);
        } finally {
            // Remove
            mgr.removeTupleSource(tsID);
        }
    }
    
    //going backward
    @Test public void testPinning2() throws Exception {
        BufferManager mgr = getTestBufferManager(1, createFakeDatabaseStorageManager());
        
        List<ElementSymbol> schema = Arrays.asList(new ElementSymbol("val"), new ElementSymbol("col")); //$NON-NLS-1$ //$NON-NLS-2$
        schema.get(0).setType(DataTypeManager.DefaultDataClasses.INTEGER);
        schema.get(1).setType(DataTypeManager.DefaultDataClasses.STRING);

        String group = "test"; //$NON-NLS-1$
        TupleSourceID tsID = mgr.createTupleSource(schema, group, TupleSourceType.PROCESSOR);
        
        // Add some batches, 1000 at a time
        int maxRows = 4000;
        int writePerBatch = 1000;
        for(int i=1; i<maxRows; i+=writePerBatch) {                
            mgr.addTupleBatch(tsID, exampleBatch(i, i+writePerBatch-1));
        }
        
        // Walk through by 100's pinning and unpinning
        int readPerBatch = 100;
        helpTestPinAndUnpin(mgr, tsID, maxRows, writePerBatch, readPerBatch);
        
        // Walk through by 2000's pinning and unpinning
        readPerBatch = 2000;
        helpTestPinAndUnpin(mgr, tsID, maxRows, writePerBatch, readPerBatch);
        
        // Remove
        mgr.removeTupleSource(tsID);
    }
    
    private static class CleanThread extends Thread {
        BufferManagerImpl mgr;
        TupleSourceID tsID;
        CleanThread(BufferManagerImpl mgr, TupleSourceID tsID){
            CleanThread.this.mgr = mgr;
            CleanThread.this.tsID = tsID;
        }
        
        public void run() {
            try {
                TupleSourceInfo info = mgr.getTupleSourceInfo(tsID, true);
                for(int i=0; i<10000; i++) {
                    mgr.clean(50000, info.getGroupInfo());
                }
            }catch(Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }
    
    private static class ReaderThread extends Thread {
        
        private BufferManager bufferMgr;
        private TupleSourceID tsID;
        private int pins;
        private int batches;
        private int rowsPerBatch;
        
        private boolean failed = false;
        Random random = new Random();
        
        public ReaderThread(BufferManager bufferMgr, TupleSourceID tsID, int pins, int batches, int rowsPerBatch) {
            this.bufferMgr = bufferMgr;
            this.tsID = tsID;
            this.pins = pins;
            this.batches = batches;
            this.rowsPerBatch = rowsPerBatch;
        }
        
        public boolean failed() {
            return this.failed;
        }
        
        public void run() {
            int begin = 0;
            int end = 0;
            try {
                for(int i=0; i<pins; i++) {
                    try {
                        int batch = random.nextInt(batches);
                        begin = 1 + (batch * rowsPerBatch);
                        end = begin + rowsPerBatch - 1;
                        bufferMgr.pinTupleBatch(tsID, begin);
                        bufferMgr.unpinTupleBatch(tsID, begin);
                    } catch(MemoryNotAvailableException e) {
                        
                    }
                }
            } catch(Exception e) {
                this.failed = true;
                e.printStackTrace();
            } finally {
                try {
                    bufferMgr.unpinTupleBatch(tsID, begin);
                } catch(Exception e) {
                    // ignore
                }
                    
            }
        }
    }
    
}
