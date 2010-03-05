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

package org.teiid.connector.jdbc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.teiid.connector.api.ConnectorException;
import org.teiid.connector.api.DataNotAvailableException;
import org.teiid.connector.api.ExecutionContext;
import org.teiid.connector.api.ProcedureExecution;
import org.teiid.connector.jdbc.translator.TranslatedCommand;
import org.teiid.connector.language.Argument;
import org.teiid.connector.language.Call;
import org.teiid.connector.language.Command;

/**
 */
public class JDBCProcedureExecution extends JDBCQueryExecution implements ProcedureExecution {

    /**
     * @param connection
     * @param sqlTranslator
     * @param logger
     * @param props
     * @param id
     */
	public JDBCProcedureExecution(Command command, Connection connection, ExecutionContext context, JDBCManagedConnectionFactory env) throws ConnectorException {
        super(command, connection, context, env);
    }

    @Override
    public void execute() throws ConnectorException {
    	Call procedure = (Call)command;
        columnDataTypes = procedure.getResultSetColumnTypes();

        //translate command
        TranslatedCommand translatedComm = translateCommand(procedure);
        
        //create statement or CallableStatement and execute
        String sql = translatedComm.getSql();
        try{
            //create parameter index map
            CallableStatement cstmt = getCallableStatement(sql);
            this.results = sqlTranslator.executeStoredProcedure(cstmt, translatedComm, procedure.getReturnType());
            addStatementWarnings();
        }catch(SQLException e){
            throw new ConnectorException(e, JDBCPlugin.Util.getString("JDBCQueryExecution.Error_executing_query__1", sql)); //$NON-NLS-1$
        }           
        
    }
    
    @Override
    public List<?> next() throws ConnectorException, DataNotAvailableException {
    	if (results == null) {
    		return null;
    	}
    	return super.next();
    }
        
    @Override
    public List<?> getOutputParameterValues() throws ConnectorException {
        try {
        	Call proc = (Call)this.command;
        	List<Object> result = new ArrayList<Object>();
        	int paramIndex = 1;
        	if (proc.getReturnType() != null) {
        		addParameterValue(result, paramIndex++, proc.getReturnType());
        	}
        	for (Argument parameter : proc.getArguments()) {
        		switch (parameter.getDirection()) {
        		case IN:
        			paramIndex++;
        			break;
        		case INOUT:
        		case OUT:
        			addParameterValue(result, paramIndex++, parameter.getType());
        			break;
        		}
			}
        	return result;
        } catch (SQLException e) {
            throw new ConnectorException(e);
        }
    }

	private void addParameterValue(List<Object> result, int paramIndex,
			Class<?> type) throws SQLException {
		Object value = sqlTranslator.retrieveValue((CallableStatement)this.statement, paramIndex, type);
		result.add(value);
	}
    
}
