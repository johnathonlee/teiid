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

package com.metamatrix.connector.xmlsource;

import java.sql.SQLXML;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.Source;

import com.metamatrix.connector.DataPlugin;
import com.metamatrix.connector.api.ConnectorEnvironment;
import com.metamatrix.connector.api.DataNotAvailableException;
import com.metamatrix.connector.api.ProcedureExecution;
import com.metamatrix.connector.exception.ConnectorException;
import com.metamatrix.connector.language.IParameter;


/** 
 * This is main class which will execute request in the XML Source
 */
public abstract class XMLSourceExecution implements ProcedureExecution {

    // Connector environment
    protected ConnectorEnvironment env;
    private boolean returnedResult;
    
    /**
     * ctor 
     * @param context
     * @param metadata
     */
    public XMLSourceExecution(ConnectorEnvironment env) {
        this.env = env;
    }
    
    protected SQLXML convertToXMLType(Source value) throws ConnectorException {
    	if (value == null) {
    		return null;
    	}
    	Object result = env.getTypeFacility().convertToRuntimeType(value);
    	if (!(result instanceof SQLXML)) {
    		throw new ConnectorException(DataPlugin.Util.getString("unknown_object_type_to_tranfrom_xml"));
    	}
    	return (SQLXML)result;
    }
    
    protected abstract Source getReturnValue();

    @Override
    public List next() throws ConnectorException, DataNotAvailableException {
    	if (!returnedResult) {
    		returnedResult = true;
    		return Arrays.asList(convertToXMLType(getReturnValue()));
    	}
    	return null;
    }  
    
    /** 
     * @see com.metamatrix.connector.api.ProcedureExecution#getOutputValue(com.metamatrix.connector.language.IParameter)
     */
    public Object getOutputValue(IParameter parameter) throws ConnectorException {
        throw new ConnectorException(XMLSourcePlugin.Util.getString("No_outputs_allowed")); //$NON-NLS-1$
    }

    /** 
     * @see com.metamatrix.connector.api.Execution#close()
     */
    public void close() throws ConnectorException {
        // no-op
    }

    /** 
     * @see com.metamatrix.connector.api.Execution#cancel()
     */
    public void cancel() throws ConnectorException {
        // no-op
    }

}
