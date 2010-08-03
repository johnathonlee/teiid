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

package org.teiid.query.processor;

import java.util.List;

import org.teiid.api.exception.query.ExpressionEvaluationException;
import org.teiid.common.buffer.BlockedException;
import org.teiid.common.buffer.TupleBatch;
import org.teiid.common.buffer.TupleBuffer;
import org.teiid.common.buffer.TupleSource;
import org.teiid.core.TeiidComponentException;
import org.teiid.core.TeiidProcessingException;


public class BatchCollector {
	
	public interface BatchProducer {
	    /**
	     * Get a batch of results or possibly an Exception.
	     * @return Batch of results
	     * @throws BlockedException indicating next batch is not available yet
	     * @throws TeiidComponentException for non-business rule exception
	     * @throws TeiidProcessingException for business rule exception, related
	     * to user input or modeling
	     */
	    TupleBatch nextBatch() throws BlockedException, TeiidComponentException, TeiidProcessingException;
	    
	    /**
	     * Get list of resolved elements describing output columns for this plan.
	     * @return List of SingleElementSymbol
	     */
	    List getOutputElements();
	}
	
	public static class BatchProducerTupleSource implements TupleSource {
		private final BatchProducer sourceNode;
		private TupleBatch sourceBatch;           // Current batch loaded from the source, if blocked
		private int sourceRow = 1;

		public BatchProducerTupleSource(BatchProducer sourceNode) {
			this.sourceNode = sourceNode;
		}

		@Override
		public List<?> nextTuple() throws TeiidComponentException,
				TeiidProcessingException {
			while (true) {
				if(sourceBatch == null) {
		            // Read next batch
		            sourceBatch = sourceNode.nextBatch();
		        }
		        
		        if(sourceBatch.getRowCount() > 0 && sourceRow <= sourceBatch.getEndRow()) {
		            // Evaluate expressions needed for grouping
		            List tuple = sourceBatch.getTuple(sourceRow);
		            tuple = updateTuple(tuple);
		            sourceRow++;
		            return tuple;
		        }
		        
		        // Check for termination condition
		        if(sourceBatch.getTerminationFlag()) {
		        	sourceBatch = null;			            
		            return null;
		        } 
		        sourceBatch = null;
			}
		}
		
		@SuppressWarnings("unused")
		protected List updateTuple(List tuple) throws ExpressionEvaluationException, BlockedException, TeiidComponentException {
			return tuple;
		}

		@Override
		public void closeSource() {
			
		}
	}
	
    private BatchProducer sourceNode;

    private boolean done = false;
    private TupleBuffer buffer;
    
    public BatchCollector(BatchProducer sourceNode, TupleBuffer buffer) {
        this.sourceNode = sourceNode;
        this.buffer = buffer;
    }

    public TupleBuffer collectTuples() throws TeiidComponentException, TeiidProcessingException {
        TupleBatch batch = null;
    	while(!done) {
            batch = sourceNode.nextBatch();

            flushBatch(batch);

            // Check for termination condition
            if(batch.getTerminationFlag()) {
            	done = true;
            	buffer.close();
                break;
            }
        }
        return buffer;
    }
    
    public TupleBuffer getTupleBuffer() {
		return buffer;
	}
    
    /**
     * Flush the batch by giving it to the buffer manager.
     */
    private void flushBatch(TupleBatch batch) throws TeiidComponentException, TeiidProcessingException {
    	if (batch.getRowCount() == 0 && !batch.getTerminationFlag()) {
    		return;
    	}
    	flushBatchDirect(batch, true);
    }
    
    @SuppressWarnings("unused")
	protected void flushBatchDirect(TupleBatch batch, boolean add) throws TeiidComponentException, TeiidProcessingException {
    	buffer.addTupleBatch(batch, add);
    }
    
    public int getRowCount() {
        return buffer.getRowCount();
    }
    
}
