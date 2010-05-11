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

package org.teiid.common.buffer;

import org.teiid.common.buffer.BufferManager;
import org.teiid.common.buffer.impl.BufferManagerImpl;
import org.teiid.common.buffer.impl.MemoryStorageManager;
import org.teiid.core.TeiidComponentException;


/**
 * <p>Factory for BufferManager instances.  One method will get
 * a server buffer manager, as it should be instantiated in a running
 * MetaMatrix server.  That BufferManager is configured mostly by the
 * passed in properties.</p>
 *
 * <p>The other method returns a stand-alone, in-memory buffer manager.  This
 * is typically used for either in-memory testing or any time the
 * query processor component is not expected to run out of memory, such as
 * within the modeler.</p>
 */
public class BufferManagerFactory {
	
	private static BufferManager INSTANCE;
	
    /**
     * Helper to get a buffer manager all set up for unmanaged standalone use.  This is
     * typically used for testing or when memory is not an issue.
     * @return BufferManager ready for use
     */
    public static BufferManager getStandaloneBufferManager() {
    	if (INSTANCE == null) {
	        BufferManagerImpl bufferMgr = createBufferManager();
	        INSTANCE = bufferMgr;
    	}

        return INSTANCE;
    }

	public static BufferManagerImpl createBufferManager() {
		BufferManagerImpl bufferMgr = new BufferManagerImpl();
		try {
			bufferMgr.initialize();
		} catch (TeiidComponentException e) {
			throw new RuntimeException(e);
		}

		// Add unmanaged memory storage manager
		bufferMgr.setStorageManager(new MemoryStorageManager());
		return bufferMgr;
	}

}
