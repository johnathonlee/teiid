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

package com.metamatrix.query.mapping.xml;

import java.util.HashMap;
import java.util.Map;

import com.metamatrix.query.sql.symbol.ElementSymbol;
import com.metamatrix.query.sql.symbol.GroupSymbol;

/** 
 * This noe represents a source node. A source node is which produces results from
 * executing a relational query.
 */
public class MappingSourceNode extends MappingBaseNode {
    
    ResultSetInfo resultSetInfo;
    private Map symbolMap = new HashMap();
    
    protected MappingSourceNode() {        
    }
    
    public MappingSourceNode(String source) {
        setProperty(MappingNodeConstants.Properties.NODE_TYPE, MappingNodeConstants.SOURCE);
        setSource(source);
    }
        
    public void acceptVisitor(MappingVisitor visitor) {
        visitor.visit(this);
    }

    public String getResultName() {
        return (String) getProperty(MappingNodeConstants.Properties.RESULT_SET_NAME);
    }
    
    public String getAliasResultName() {
        return (String) getProperty(MappingNodeConstants.Properties.ALIAS_RESULT_SET_NAME);
    }    
    
    /**
     * in the case of recursive node we need to know the original source node name; this represents
     * that name.
     * @param alias
     */
    public void setAliasResultName(String alias) {
        setProperty(MappingNodeConstants.Properties.ALIAS_RESULT_SET_NAME, alias);
    }    
    
    public MappingBaseNode setSource(String source) {
        if (source != null) {
            setProperty(MappingNodeConstants.Properties.RESULT_SET_NAME, source);
        }
        return this;
    }        
    
    public MappingSourceNode getSourceNode() {
        return this;
    }
    
    public boolean isRootSourceNode() {
        return getParentSourceNode() == null;
    }
    
    public MappingSourceNode getParentSourceNode() {
        MappingBaseNode parent = getParentNode();
        while (parent != null) {
            if (parent instanceof MappingSourceNode) {
                return (MappingSourceNode)parent;
            }
            parent = parent.getParentNode();
        }
        return null;
    }
    
    public ResultSetInfo getResultSetInfo() {
        if (this.resultSetInfo == null) {
            this.resultSetInfo = new ResultSetInfo(getActualResultSetName());
            setProperty(MappingNodeConstants.Properties.RESULT_SET_INFO, this.resultSetInfo);
        }
        return this.resultSetInfo;
    }

    public void setResultSetInfo(ResultSetInfo resultSetInfo) {
        this.resultSetInfo = resultSetInfo;        
    }
    
    public String toString() {
        return "[" + getProperty(MappingNodeConstants.Properties.NODE_TYPE) + "]" //$NON-NLS-1$ //$NON-NLS-2$ 
                + getProperty(MappingNodeConstants.Properties.RESULT_SET_NAME);
    }
    
    public Map getSymbolMap() {
        return this.symbolMap;
    }

    public void setSymbolMap(Map symbolMap) {
        this.symbolMap = symbolMap;
        
        updateSymbolMapDependentValues();        
    }

    public void updateSymbolMapDependentValues() {
        // based on the symbol map modify the getalias name
        if (getAliasResultName() != null) {
            GroupSymbol newGroup = getMappedSymbol(new GroupSymbol(getAliasResultName()));
            setAliasResultName(newGroup.getName());
        }
        
        ElementSymbol mappingClassSymbol = this.getResultSetInfo().getMappingClassSymbol();
        
        if (mappingClassSymbol != null) {
            this.getResultSetInfo().setMappingClassSymbol(getMappedSymbol(mappingClassSymbol));
        }
    }
    
    public String getActualResultSetName() {
        GroupSymbol group = getMappedSymbol(new GroupSymbol(getResultName()));
        return group.getName();
    }
    
    public Map buildFullSymbolMap() {
        HashMap map = new HashMap();
        MappingSourceNode sourceNode = this;
        
        while(sourceNode != null) {
            map.putAll(sourceNode.getSymbolMap());
            sourceNode = sourceNode.getParentSourceNode();
        }
        return map;
    }
    
    public ElementSymbol getMappedSymbol(ElementSymbol symbol) {
        ElementSymbol mappedSymbol = (ElementSymbol)symbolMap.get(symbol);
        if (mappedSymbol == null) {
            MappingSourceNode parentSourceNode = getParentSourceNode();
            if (parentSourceNode != null) {
                return parentSourceNode.getMappedSymbol(symbol);
            }
        }
        
        if (mappedSymbol == null) {
            return symbol;
        }
        
        return mappedSymbol;
    }

    public GroupSymbol getMappedSymbol(GroupSymbol symbol) {
        GroupSymbol mappedSymbol = (GroupSymbol)symbolMap.get(symbol);
        if (mappedSymbol == null) {
            MappingSourceNode parentSourceNode = getParentSourceNode();
            if (parentSourceNode != null) {
                return parentSourceNode.getMappedSymbol(symbol);
            }
        }
        
        if (mappedSymbol == null) {
            return symbol;
        }
        
        return mappedSymbol;
    }    
}
