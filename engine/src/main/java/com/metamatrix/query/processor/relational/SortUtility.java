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

package com.metamatrix.query.processor.relational;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import com.metamatrix.api.exception.MetaMatrixComponentException;
import com.metamatrix.api.exception.MetaMatrixProcessingException;
import com.metamatrix.common.buffer.BlockedException;
import com.metamatrix.common.buffer.BufferManager;
import com.metamatrix.common.buffer.IndexedTupleSource;
import com.metamatrix.common.buffer.TupleBuffer;
import com.metamatrix.common.buffer.TupleSource;
import com.metamatrix.common.buffer.BufferManager.TupleSourceType;
import com.metamatrix.core.util.Assertion;
import com.metamatrix.query.sql.lang.OrderBy;
import com.metamatrix.query.sql.symbol.SingleElementSymbol;

/**
 * Implements several modes of a multi-pass sort.
 */
public class SortUtility {
	
	public enum Mode {
		SORT,
		/** Removes duplicates, no sort elements need to be specified.
		 *  may perform additional passes as new batches become available 
		 */
		DUP_REMOVE, 
		/** Removes duplicates, but guarantees order based upon the sort elements.
		 */
		DUP_REMOVE_SORT
	}
	
	/**
	 * state holder for the merge algorithm
	 */
	private class SortedSublist implements Comparable<SortedSublist> {
		List<?> tuple;
		int index;
		boolean duplicate;
		IndexedTupleSource its;
		
		@Override
		public int compareTo(SortedSublist o) {
			return comparator.compare(this.tuple, o.tuple);
		}
	}

	//constructor state
    private TupleSource sourceID;
    private Mode mode;
    private BufferManager bufferManager;
    private String groupName;
    private List<SingleElementSymbol> schema;
	private ListNestedSortComparator comparator;

    private TupleBuffer output;
    private boolean doneReading;
    private int phase = INITIAL_SORT;
    private List<TupleBuffer> activeTupleBuffers = new ArrayList<TupleBuffer>();
    private int masterSortIndex;

    // Phase constants for readability
    private static final int INITIAL_SORT = 1;
    private static final int MERGE = 2;
    private static final int DONE = 3;
    
    public SortUtility(TupleSource sourceID, List sortElements, List<Boolean> sortTypes, Mode mode, BufferManager bufferMgr,
                        String groupName) {
        this.sourceID = sourceID;
        this.mode = mode;
        this.bufferManager = bufferMgr;
        this.groupName = groupName;
        this.schema = this.sourceID.getSchema();
        int distinctIndex = sortElements != null? sortElements.size() - 1:0;
        if (mode != Mode.SORT) {
	        if (sortElements == null) {
	    		sortElements = this.schema;
	    		sortTypes = Collections.nCopies(sortElements.size(), OrderBy.ASC);
	        } else if (sortElements.size() < schema.size()) {
	        	sortElements = new ArrayList(sortElements);
	        	List<SingleElementSymbol> toAdd = new ArrayList<SingleElementSymbol>(schema);
	        	toAdd.removeAll(sortElements);
	        	sortElements.addAll(toAdd);
	        	sortTypes = new ArrayList<Boolean>(sortTypes);
	        	sortTypes.addAll(Collections.nCopies(sortElements.size() - sortTypes.size(), OrderBy.ASC));
        	}
        }
        
        int[] cols = new int[sortElements.size()];

        for (ListIterator<SingleElementSymbol> iter = sortElements.listIterator(); iter.hasNext();) {
            SingleElementSymbol elem = iter.next();
            
            cols[iter.previousIndex()] = schema.indexOf(elem);
            Assertion.assertTrue(cols[iter.previousIndex()] != -1);
        }
        this.comparator = new ListNestedSortComparator(cols, sortTypes);
        this.comparator.setDistinctIndex(distinctIndex);
    }
    
    public boolean isDone() {
    	return this.doneReading && this.phase == DONE;
    }
    
    public TupleBuffer sort()
        throws MetaMatrixComponentException, MetaMatrixProcessingException {

        if(this.phase == INITIAL_SORT) {
            initialSort();
        }
        
        if(this.phase == MERGE) {
            mergePhase();
        }
        if (this.output != null) {
        	return this.output;
        }
        return this.activeTupleBuffers.get(0);
    }

	private TupleBuffer createTupleBuffer() throws MetaMatrixComponentException {
		return bufferManager.createTupleBuffer(this.schema, this.groupName, TupleSourceType.PROCESSOR);
	}
    
	/**
	 * creates sorted sublists stored in tuplebuffers
	 */
    protected void initialSort() throws MetaMatrixComponentException, MetaMatrixProcessingException {
    	while(!doneReading) {
            List<List<?>> workingTuples = new ArrayList<List<?>>();
            int maxRows = bufferManager.getMaxProcessingBatches() * bufferManager.getProcessorBatchSize();
	        while(!doneReading && workingTuples.size() < maxRows) {
	            try {
	            	List<?> tuple = sourceID.nextTuple();
	            	
	            	if (tuple == null) {
	            		doneReading = true;
	            		break;
	            	}
	            	
                    addTuple(workingTuples, tuple);
	            } catch(BlockedException e) {
	            	if (workingTuples.isEmpty() && (mode != Mode.DUP_REMOVE || activeTupleBuffers.isEmpty())) {
	            		throw e; //block if no work can be performed
	            	}
	            	break;
	            } 
	        }
	
	        if(workingTuples.isEmpty()) {
	        	break;
	        }
		
	        TupleBuffer activeID = createTupleBuffer();
	        activeTupleBuffers.add(activeID);
	        if (this.mode == Mode.SORT) {
	        	//perform a stable sort
	    		Collections.sort(workingTuples, comparator);
	        }
	        for (List<?> list : workingTuples) {
				activeID.addTuple(list);
			}
        }
    	
    	if (this.activeTupleBuffers.isEmpty()) {
            activeTupleBuffers.add(createTupleBuffer());
        }  

        this.phase = MERGE;
    }

	protected void addTuple(List<List<?>> workingTuples, List<?> tuple) {
		if (this.mode == Mode.SORT) {
			workingTuples.add(tuple);
			return;
		}
		int index = Collections.binarySearch(workingTuples, tuple, comparator);
		if (index >= 0) {
			return; //it's already there
		}
		workingTuples.add(-index - 1, tuple);
	}
		
    protected void mergePhase() throws MetaMatrixComponentException, MetaMatrixProcessingException {
    	while(this.activeTupleBuffers.size() > 1) {    		
    		ArrayList<SortedSublist> workingTuples = new ArrayList<SortedSublist>(activeTupleBuffers.size());
            
            TupleBuffer merged = createTupleBuffer();

            int sortedIndex = 0;
            int maxSortIndex = Math.min(this.bufferManager.getMaxProcessingBatches() * 2, activeTupleBuffers.size());
            for(; sortedIndex<maxSortIndex; sortedIndex++) { 
            	TupleBuffer activeID = activeTupleBuffers.get(sortedIndex);
            	SortedSublist sortedSublist = new SortedSublist();
            	sortedSublist.its = activeID.createIndexedTupleSource();
            	sortedSublist.index = sortedIndex;
            	addWorkingTuple(workingTuples, sortedSublist);
            }
            
            // iteratively process the lowest tuple
            while (workingTuples.size() > 0) {
            	SortedSublist sortedSublist = workingTuples.remove(0);
            	if (!sortedSublist.duplicate) {
                	merged.addTuple(sortedSublist.tuple);
                    if (this.output != null && sortedSublist.index != masterSortIndex && sortedIndex > masterSortIndex) {
                    	this.output.addTuple(sortedSublist.tuple); //a new distinct row
                    }
            	}
            	addWorkingTuple(workingTuples, sortedSublist);
            }                

            // Remove merged sublists
            for(int i=0; i<sortedIndex; i++) {
            	TupleBuffer id = activeTupleBuffers.remove(0);
            	if (id != this.output) {
            		id.remove();
            	}
            }

            this.activeTupleBuffers.add(merged);           
            masterSortIndex = masterSortIndex - sortedIndex + 1;
            if (masterSortIndex < 0) {
            	masterSortIndex = this.activeTupleBuffers.size() - 1;
            }
        }
    	
        // Close sorted source (all others have been removed)
        if (doneReading) {
        	activeTupleBuffers.get(0).close();
	        if (this.output != null) {
	        	this.output.close();
	        }
	        this.phase = DONE;
	        return;
        }
    	Assertion.assertTrue(mode == Mode.DUP_REMOVE);
    	if (this.output == null) {
    		this.output = activeTupleBuffers.get(0);
    	}
    	this.phase = INITIAL_SORT;
    }

	private void addWorkingTuple(ArrayList<SortedSublist> workingTuples, SortedSublist sortedSublist) throws MetaMatrixComponentException, MetaMatrixProcessingException {
		sortedSublist.tuple = null;
		try {
			sortedSublist.tuple = sortedSublist.its.nextTuple();
        } catch (BlockedException e) {
        	//intermediate sources aren't closed
        }  
        if (sortedSublist.tuple == null) {
        	return; // done with this sublist
        }
        sortedSublist.duplicate = false;
		int index = Collections.binarySearch(workingTuples, sortedSublist);
		if (index >= 0) {
			sortedSublist.duplicate = mode != Mode.SORT;
			workingTuples.add(index, sortedSublist);
		} else {
			workingTuples.add(-index - 1, sortedSublist);
		}
	} 

    public boolean isDistinct() {
    	return this.comparator.isDistinct();
    }
    
}
