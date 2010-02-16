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

package com.metamatrix.common.buffer;

import java.util.List;

import com.metamatrix.api.exception.MetaMatrixComponentException;

/**
 * The buffer manager controls how memory is used and how data flows through 
 * the system.  It uses {@link StorageManager storage managers}
 * to retrieve data, store data, and 
 * transfer data.  The buffer manager has algorithms that tell it when and 
 * how to store data.  The buffer manager should also be aware of memory 
 * management issues.
 */
public interface BufferManager {
	
	public enum TupleSourceType {
		/**
		 * Indicates that a tuple source is use during query processing as a 
		 * temporary results.
		 */
		PROCESSOR,
		/**
		 * Indicates that a tuple source represents a query's final results.
		 */
		FINAL
	}
	
	public enum BufferReserveMode {
		WAIT,
		FORCE,
		NO_WAIT
	}

	public static int DEFAULT_CONNECTOR_BATCH_SIZE = 1024;
	public static int DEFAULT_PROCESSOR_BATCH_SIZE = 512;
	public static int DEFAULT_MAX_PROCESSING_BATCHES = 128;
	
	/**
	 * This is the maximum number of batch columns used for processing.
	 * See {@link #reserveBuffers(int, boolean)}
	 */
	public static int DEFAULT_RESERVE_BUFFERS = 16384;
	
    /**
     * Get the batch size to use during query processing.  
     * @return Batch size (# of rows)
     */
    int getProcessorBatchSize();

    /**
     * Get the batch size to use when reading data from a connector.  
     * @return Batch size (# of rows)
     */
    int getConnectorBatchSize();
    
	TupleBuffer createTupleBuffer(List elements, String groupName, TupleSourceType tupleSourceType) 
    throws MetaMatrixComponentException;
	
	/**
	 * Return the maximum number of batches that can be temporarily held potentially 
	 * across even a blocked exception.
	 * @return
	 */
    int getMaxProcessingBatchColumns();
    
    /**
     * Creates a new {@link FileStore}.  See {@link FileStore#setCleanupReference(Object)} to
     * automatically cleanup the underlying resources.
     * @param name
     * @return
     */
    FileStore createFileStore(String name);
    
    /**
     * Reserve up to count buffers for use.
     * @param count
     * @param mode
     * @return
     */
    int reserveBuffers(int count, BufferReserveMode mode);
    
    /**
     * Releases the buffers reserved by a call to {@link BufferManager#reserveBuffers(int, boolean)}
     * @param count
     */
    void releaseBuffers(int count);
    
    /**
     * Get the size estimate for the given schema.
     */
    int getSchemaSize(List elements);
    
}
