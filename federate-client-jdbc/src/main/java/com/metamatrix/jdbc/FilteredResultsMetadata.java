/*
 * JBoss, Home of Professional Open Source.
 * Copyright (C) 2008 Red Hat, Inc.
 * Copyright (C) 2000-2007 MetaMatrix, Inc.
 * Licensed to Red Hat, Inc. under one or more contributor 
 * license agreements.  See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package com.metamatrix.jdbc;

import java.sql.SQLException;

import com.metamatrix.core.log.Logger;
import com.metamatrix.jdbc.api.ResultSetMetaData;

/**
 */
public class FilteredResultsMetadata extends WrapperImpl implements ResultSetMetaData {

    private ResultSetMetaData delegate; 
    private int actualColumnCount;
    Logger logger;
    
    static FilteredResultsMetadata newInstance (ResultSetMetaData rsmd, int actualColumnCount, Logger logger) {
        return new FilteredResultsMetadata(rsmd, actualColumnCount, logger);
    }    
    
    FilteredResultsMetadata(ResultSetMetaData rsmd, int actualColumnCount, Logger logger) {
        this.delegate = rsmd;
        this.actualColumnCount = actualColumnCount;
        this.logger = logger;
    }
    
    public int getColumnCount() throws SQLException {
        return actualColumnCount;
    }
    
    private void verifyColumnIndex(int index) throws SQLException {
        if(index > actualColumnCount) {
            throw new SQLException(JDBCPlugin.Util.getString("FilteredResultsMetadata.Invalid_index", index)); //$NON-NLS-1$
        }
    }

    public String getVirtualDatabaseName(int index) throws SQLException {
        verifyColumnIndex(index);
        return this.delegate.getVirtualDatabaseName(index);
    }

    public String getVirtualDatabaseVersion(int index) throws SQLException {
        verifyColumnIndex(index);
        return this.delegate.getVirtualDatabaseVersion(index);
    }

    public boolean isAutoIncrement(int column) throws SQLException {
        verifyColumnIndex(column);
        return this.delegate.isAutoIncrement(column);
    }

    public boolean isCaseSensitive(int column) throws SQLException {
        verifyColumnIndex(column);
        return this.delegate.isCaseSensitive(column);
    }

    public boolean isSearchable(int column) throws SQLException {
        verifyColumnIndex(column);
        return this.delegate.isSearchable(column);
    }

    public boolean isCurrency(int column) throws SQLException {
        verifyColumnIndex(column);
        return this.delegate.isCurrency(column);
    }

    public int isNullable(int column) throws SQLException {
        verifyColumnIndex(column);
        return this.delegate.isNullable(column);
    }

    public boolean isSigned(int column) throws SQLException {
        verifyColumnIndex(column);
        return this.delegate.isSigned(column);
    }

    public int getColumnDisplaySize(int column) throws SQLException {
        verifyColumnIndex(column);
        return this.delegate.getColumnDisplaySize(column);
    }

    public String getColumnLabel(int column) throws SQLException {
        verifyColumnIndex(column);
        return this.delegate.getColumnLabel(column);
    }

    public String getColumnName(int column) throws SQLException {
        verifyColumnIndex(column);
        return this.delegate.getColumnName(column);
    }

    public String getSchemaName(int column) throws SQLException {
        verifyColumnIndex(column);
        return this.delegate.getSchemaName(column);
    }

    public int getPrecision(int column) throws SQLException {
        verifyColumnIndex(column);
        return this.delegate.getPrecision(column);
    }

    public int getScale(int column) throws SQLException {
        verifyColumnIndex(column);
        return this.delegate.getScale(column);
    }

    public String getTableName(int column) throws SQLException {
        verifyColumnIndex(column);
        return this.delegate.getTableName(column);
    }

    public String getCatalogName(int column) throws SQLException {
        verifyColumnIndex(column);
        return this.delegate.getCatalogName(column);
    }

    public int getColumnType(int column) throws SQLException {
        verifyColumnIndex(column);
        return this.delegate.getColumnType(column);
    }

    public String getColumnTypeName(int column) throws SQLException {
        verifyColumnIndex(column);
        return this.delegate.getColumnTypeName(column);
    }

    public boolean isReadOnly(int column) throws SQLException {
        verifyColumnIndex(column);
        return this.delegate.isReadOnly(column);
    }

    public boolean isWritable(int column) throws SQLException {
        verifyColumnIndex(column);
        return this.delegate.isWritable(column);
    }

    public boolean isDefinitelyWritable(int column) throws SQLException {
        verifyColumnIndex(column);
        return this.delegate.isDefinitelyWritable(column);
    }

    public String getColumnClassName(int column) throws SQLException {
        verifyColumnIndex(column);
        return this.delegate.getColumnClassName(column);
    }

    public int getParameterCount() throws SQLException{
        return this.delegate.getParameterCount();
    }

}
