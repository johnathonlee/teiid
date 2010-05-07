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

/**
 * 
 */
package org.teiid.dqp.internal.datamgr.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.teiid.connector.language.Argument;
import org.teiid.connector.language.Call;
import org.teiid.connector.language.Argument.Direction;
import org.teiid.resource.ConnectorException;
import org.teiid.resource.cci.ProcedureExecution;

import com.metamatrix.dqp.DQPPlugin;

class ProcedureBatchHandler {
	private Call proc;
	private ProcedureExecution procExec;
	private int paramCols = 0;
	private int resultSetCols = 0;
	private List filler;
    
	public ProcedureBatchHandler(Call proc, ProcedureExecution procExec) {
		this.proc = proc;
		this.procExec = procExec;
        List<Argument> params = proc.getArguments();
        resultSetCols = proc.getResultSetColumnTypes().length;
        if (proc.getReturnType() != null) {
        	paramCols++;
        }
        if(params != null && !params.isEmpty()){
        	for (Argument param : params) {
                if(param.getDirection() == Direction.OUT || param.getDirection() == Direction.INOUT){
                    paramCols += 1;
                }
            }
        }
        if (paramCols > 0) {
        	filler = Collections.nCopies(paramCols, null);
        }
	}
	
	List padRow(List row) throws ConnectorException {
        if (row.size() != resultSetCols) {
            throw new ConnectorException(DQPPlugin.Util.getString("ConnectorWorker.ConnectorWorker_result_set_unexpected_columns", new Object[] {proc, new Integer(resultSetCols), new Integer(row.size())})); //$NON-NLS-1$
        }
        if (paramCols == 0) {
        	return row;
        }
        List result = new ArrayList(resultSetCols + paramCols);
        result.addAll(row);
        result.addAll(filler);
        return result;
	}
	
	List getParameterRow() throws ConnectorException {
		if (paramCols == 0) {
			return null;
		}
        List<Object> result = new ArrayList<Object>(Arrays.asList(new Object[resultSetCols]));
        result.addAll(procExec.getOutputParameterValues());
        return result;
	}
	
}
