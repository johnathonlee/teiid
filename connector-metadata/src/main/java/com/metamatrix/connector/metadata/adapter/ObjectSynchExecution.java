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

package com.metamatrix.connector.metadata.adapter;

import java.util.Iterator;
import java.util.List;

import com.metamatrix.connector.api.DataNotAvailableException;
import com.metamatrix.connector.api.ResultSetExecution;
import com.metamatrix.connector.basic.BasicExecution;
import com.metamatrix.connector.exception.ConnectorException;
import com.metamatrix.connector.language.IQuery;
import com.metamatrix.connector.metadata.MetadataConnectorPlugin;
import com.metamatrix.connector.metadata.internal.IObjectQuery;
import com.metamatrix.connector.metadata.internal.MetadataException;
import com.metamatrix.connector.metadata.internal.ObjectQuery;
import com.metamatrix.connector.metadata.internal.ObjectQueryProcessor;
import com.metamatrix.connector.metadata.runtime.RuntimeMetadata;

/**
 * Adapter to expose the object processing logic via the standard connector API.
 * Makes the batches coming from the objectSource match the batch sizes requested by the caller.
 */
public class ObjectSynchExecution extends BasicExecution implements ResultSetExecution {
    private final RuntimeMetadata metadata;
    private ObjectQueryProcessor processor;

    private IObjectQuery query;
    private Iterator queryResults;
    private ObjectConnection connection;
    private IQuery command;
    private volatile boolean cancel;

    public ObjectSynchExecution(IQuery command, RuntimeMetadata metadata, ObjectConnection connection) {
        this.metadata = metadata;
        this.connection = connection;
        this.command = command;
    }

    @Override
    public void execute() throws ConnectorException {
        this.processor = new ObjectQueryProcessor(connection.getMetadataObjectSource());
        
        this.query = new ObjectQuery(metadata, command);
        try {
			queryResults = processor.process(this.query);
		} catch (MetadataException e) {
			throw new ConnectorException(e);
		}              
    	
    }
    
    @Override
    public List next() throws ConnectorException, DataNotAvailableException {
    	if (cancel) {
            throw new ConnectorException(MetadataConnectorPlugin.Util.getString("ObjectSynchExecution.closed")); //$NON-NLS-1$
        }
    	if(this.queryResults == null) {
        	return null;
        }
    	int count = 0;
    	if (queryResults.hasNext()) {
    		return (List)queryResults.next();
    	}
    	return null;
    }

    /* 
     * @see com.metamatrix.data.Execution#cancel()
     */
    @Override
    public void cancel() throws ConnectorException {
       cancel = true;
    }

    /* 
     * @see com.metamatrix.data.Execution#close()
     */
    @Override
    public void close() throws ConnectorException {
    }

}
