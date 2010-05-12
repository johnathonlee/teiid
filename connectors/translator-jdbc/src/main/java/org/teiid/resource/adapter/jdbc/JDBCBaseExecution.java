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

package org.teiid.resource.adapter.jdbc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.List;

import org.teiid.language.Command;
import org.teiid.language.Literal;
import org.teiid.logging.LogConstants;
import org.teiid.logging.LogManager;
import org.teiid.logging.MessageLevel;
import org.teiid.translator.BasicExecution;
import org.teiid.translator.ConnectorException;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.jdbc.TranslatedCommand;
import org.teiid.translator.jdbc.Translator;


/**
 */
public abstract class JDBCBaseExecution extends BasicExecution  {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    // Passed to constructor
    protected Connection connection;
    protected Translator sqlTranslator;
    protected ExecutionContext context;

    // Derived from properties
    protected boolean trimString;
    protected int fetchSize;
    protected int maxResultRows;

    // Set during execution
    protected Statement statement;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    protected JDBCBaseExecution(Connection connection, ExecutionContext context, JDBCExecutionFactory env, Translator translator) {
        this.connection = connection;
        this.sqlTranslator = translator;
        this.context = context;

        trimString = env.isTrimStrings();
        fetchSize = (env.getFetchSize() != -1)?env.getFetchSize():context.getBatchSize();
        maxResultRows = env.getMaxResultRows();
      
        //if the connector work needs to throw an excpetion, set the size plus 1
        if (maxResultRows > 0 && env.isExceptionOnMaxRows()) {
        	maxResultRows++;
        }
        if (maxResultRows > 0) {
        	fetchSize = Math.min(fetchSize, maxResultRows);
        }
    }
    
    /**
     * Return true if this is a batched update
     */
    protected void bindPreparedStatementValues(PreparedStatement stmt, TranslatedCommand tc, int rowCount) throws SQLException {
        List<?> params = tc.getPreparedValues();

        for (int row = 0; row < rowCount; row++) {
	        for (int i = 0; i< params.size(); i++) {
	            Literal paramValue = (Literal)params.get(i);
	            Object value = paramValue.getValue();
	            if (paramValue.isMultiValued()) {
	            	value = ((List<?>)value).get(row);
	            }
	            Class<?> paramType = paramValue.getType();
	            sqlTranslator.bindValue(stmt, value, paramType, i+1);
	        }
	        if (rowCount > 1) {
            	stmt.addBatch();
            }
        }
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    protected TranslatedCommand translateCommand(Command command) throws ConnectorException {
        TranslatedCommand translatedCommand = new TranslatedCommand(context, sqlTranslator);
        translatedCommand.translateCommand(command);

        if (translatedCommand.getSql() != null && LogManager.isMessageToBeRecorded(LogConstants.CTX_CONNECTOR, MessageLevel.DETAIL)) {
            LogManager.logDetail("Source-specific command: " + translatedCommand.getSql()); //$NON-NLS-1$
        }

        return translatedCommand;
    }

    /*
     * @see com.metamatrix.data.Execution#close()
     */
    public synchronized void close() throws ConnectorException {
        try {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
            	connection.close();
            }
        } catch (SQLException e) {
            throw new ConnectorException(e);
        }
    }

    /*
     * @see com.metamatrix.data.Execution#cancel()
     */
    public synchronized void cancel() throws ConnectorException {
        // if both the DBMS and driver support aborting an SQL
        try {
            if (statement != null) {
                statement.cancel();
            }
        } catch (SQLException e) {
            // Defect 16187 - DataDirect does not support the cancel() method for
            // Statement.cancel() for DB2 and Informix. Here we are tolerant
            // of these and other JDBC drivers that do not support the cancel() operation.
        }
    }

    protected void setSizeContraints(Statement statement) throws SQLException {
        if (maxResultRows > 0) {
            statement.setMaxRows(maxResultRows);
        }
    	statement.setFetchSize(fetchSize);
    }

    protected synchronized Statement getStatement() throws SQLException {
        if (statement != null) {
            statement.close();
            statement = null;
        }
        statement = connection.createStatement();
        setSizeContraints(statement);
        return statement;
    }

    protected synchronized CallableStatement getCallableStatement(String sql) throws SQLException {
        if (statement != null) {
            statement.close();
            statement = null;
        }
        statement = connection.prepareCall(sql);
        setSizeContraints(statement);
        return (CallableStatement)statement;
    }

    protected synchronized PreparedStatement getPreparedStatement(String sql) throws SQLException {
        if (statement != null) {
            statement.close();
            statement = null;
        }
        statement = connection.prepareStatement(sql);
        setSizeContraints(statement);
        return (PreparedStatement)statement;
    }

    /**
     * Returns the JDBC connection used by the execution object.
     * 
     * @return Returns the connection.
     * @since 4.1.1
     */
    public Connection getConnection() {
        return this.connection;
    }
    
    public Translator getSqlTranslator() {
		return sqlTranslator;
	}
    
    public void addStatementWarnings() throws SQLException {
    	SQLWarning warning = this.statement.getWarnings();
    	while (warning != null) {
    		SQLWarning toAdd = warning;
    		warning = toAdd.getNextWarning();
    		toAdd.setNextException(null);
    		if (LogManager.isMessageToBeRecorded(LogConstants.CTX_CONNECTOR, MessageLevel.DETAIL)) {
    			LogManager.logDetail(context.getRequestIdentifier() + " Warning: ", warning); //$NON-NLS-1$
    		}
    		context.addWarning(toAdd);
    	}
    	this.statement.clearWarnings();
    }
}
