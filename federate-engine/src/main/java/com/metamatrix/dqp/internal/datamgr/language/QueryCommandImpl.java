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

package com.metamatrix.dqp.internal.datamgr.language;

import java.util.Iterator;
import java.util.List;

import com.metamatrix.connector.language.ILimit;
import com.metamatrix.connector.language.IOrderBy;
import com.metamatrix.connector.language.ISelectSymbol;

public abstract class QueryCommandImpl extends BaseLanguageObject implements com.metamatrix.connector.language.IQueryCommand {

    private IOrderBy orderBy = null;
    private ILimit limit = null;

    /**
     * @see com.metamatrix.connector.language.IQuery#getOrderBy()
     */
    public IOrderBy getOrderBy() {
        return orderBy;
    }

    /**
     * @see com.metamatrix.connector.language.IQuery#getLimit()
     */
    public ILimit getLimit() {
        return limit;
    }
    
    public String[] getColumnNames() {
        List selectSymbols = getProjectedQuery().getSelect().getSelectSymbols();
        String[] columnNames = new String[selectSymbols.size()];
        int symbolIndex = 0;
        for (Iterator i = selectSymbols.iterator(); i.hasNext(); symbolIndex++) {
            columnNames[symbolIndex] = ((ISelectSymbol)i.next()).getOutputName();
        }
        return columnNames;
    }
    
    public Class[] getColumnTypes() {
        List selectSymbols = getProjectedQuery().getSelect().getSelectSymbols();
        Class[] columnTypes = new Class[selectSymbols.size()];
        int symbolIndex = 0;
        for (Iterator i = selectSymbols.iterator(); i.hasNext(); symbolIndex++) {
            ISelectSymbol symbol = (ISelectSymbol)i.next();
            if (symbol.getExpression() == null) {
                columnTypes[symbolIndex] = null;
            } else {
                columnTypes[symbolIndex] = symbol.getExpression().getType();
            }
        }
        return columnTypes;
    }
    
    /* 
     * @see com.metamatrix.data.language.IQuery#setOrderBy(com.metamatrix.data.language.IOrderBy)
     */
    public void setOrderBy(IOrderBy orderBy) {
        this.orderBy = orderBy;
    }
    
    /* 
     * @see com.metamatrix.data.language.IQuery#setOrderBy(com.metamatrix.data.language.IOrderBy)
     */
    public void setLimit(ILimit limit) {
        this.limit = limit;
    }
}
