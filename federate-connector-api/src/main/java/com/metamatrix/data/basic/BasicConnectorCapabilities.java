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

package com.metamatrix.data.basic;

import java.util.List;

import com.metamatrix.data.api.ConnectorCapabilities;

/**
 * This class is a base implementation of the ConnectorCapabilities interface.
 * It is implemented to return false for all capabilities.  Subclass this base
 * class and override any methods necessary to specify capabilities the
 * connector actually supports.  
 */
public class BasicConnectorCapabilities implements ConnectorCapabilities {
    
    protected int maxInCriteriaSize = -1;
    
    /**
     * Construct the basic capabilities class.
     */
    public BasicConnectorCapabilities() {
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#getCapabilitiesScope()
     */
    public int getCapabilitiesScope() {
        return ConnectorCapabilities.SCOPE.GLOBAL;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsExecutionMode(int)
     */
    public boolean supportsExecutionMode(int executionMode) {
        if(executionMode == ConnectorCapabilities.EXECUTION_MODE.SYNCH_QUERY) {
            return true;
        }
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsSelectDistinct()
     */
    public boolean supportsSelectDistinct() {
        return false;
    }

    /** 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsSelectLiterals()
     * @since 4.2
     */
    public boolean supportsSelectLiterals() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsAliasedGroup()
     */
    public boolean supportsAliasedGroup() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsJoins()
     */
    public boolean supportsJoins() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsSelfJoins()
     */
    public boolean supportsSelfJoins() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsOuterJoins()
     */
    public boolean supportsOuterJoins() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsFullOuterJoins()
     */
    public boolean supportsFullOuterJoins() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsCriteria()
     */
    public boolean supportsCriteria() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsBetweenCriteria()
     */
    public boolean supportsBetweenCriteria() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsCompareCriteria()
     */
    public boolean supportsCompareCriteria() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsCompareCriteriaEquals()
     */
    public boolean supportsCompareCriteriaEquals() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsCompareCriteriaNotEquals()
     */
    public boolean supportsCompareCriteriaNotEquals() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsCompareCriteriaLessThan()
     */
    public boolean supportsCompareCriteriaLessThan() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsCompareCriteriaLessThanOrEqual()
     */
    public boolean supportsCompareCriteriaLessThanOrEqual() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsCompareCriteriaGreaterThan()
     */
    public boolean supportsCompareCriteriaGreaterThan() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsCompareCriteriaGreaterThanOrEqual()
     */
    public boolean supportsCompareCriteriaGreaterThanOrEqual() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsLikeCriteria()
     */
    public boolean supportsLikeCriteria() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsLikeCriteriaEscapeCharacter()
     */
    public boolean supportsLikeCriteriaEscapeCharacter() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsInCriteria()
     */
    public boolean supportsInCriteria() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsInCriteriaSubquery()
     */
    public boolean supportsInCriteriaSubquery() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsIsNullCriteria()
     */
    public boolean supportsIsNullCriteria() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsAndCriteria()
     */
    public boolean supportsAndCriteria() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsOrCriteria()
     */
    public boolean supportsOrCriteria() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsNotCriteria()
     */
    public boolean supportsNotCriteria() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsExistsCriteria()
     */
    public boolean supportsExistsCriteria() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsQuantifiedCompareCriteria()
     */
    public boolean supportsQuantifiedCompareCriteria() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsQuantifiedCompareCriteriaSome()
     */
    public boolean supportsQuantifiedCompareCriteriaSome() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsQuantifiedCompareCriteriaAll()
     */
    public boolean supportsQuantifiedCompareCriteriaAll() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsOrderBy()
     */
    public boolean supportsOrderBy() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsAggregates()
     */
    public boolean supportsAggregates() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsAggregatesSum()
     */
    public boolean supportsAggregatesSum() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsAggregatesAvg()
     */
    public boolean supportsAggregatesAvg() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsAggregatesMin()
     */
    public boolean supportsAggregatesMin() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsAggregatesMax()
     */
    public boolean supportsAggregatesMax() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsAggregatesCount()
     */
    public boolean supportsAggregatesCount() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsAggregatesCountStar()
     */
    public boolean supportsAggregatesCountStar() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsAggregatesDistinct()
     */
    public boolean supportsAggregatesDistinct() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsScalarSubqueries()
     */
    public boolean supportsScalarSubqueries() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsCorrelatedSubqueries()
     */
    public boolean supportsCorrelatedSubqueries() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsCaseExpressions()
     */
    public boolean supportsCaseExpressions() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsSearchedCaseExpressions()
     */
    public boolean supportsSearchedCaseExpressions() {
        return false;
    }

    /* 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsScalarFunctions()
     */
    public boolean supportsScalarFunctions() {
        return false;
    }

    /**
     * Return null to indicate no functions are supported.
     * @return null 
     * @see com.metamatrix.data.api.ConnectorCapabilities#getSupportedFunctions()
     */
    public List getSupportedFunctions() {
        return null;
    }

    /*
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsXATransactions()
     */
    public boolean supportsXATransactions() {
        return false;
    }

    public boolean supportsInlineViews() {
        return false;
    }
    
    public boolean supportsOrderByInInlineViews() {
        return false;
    }

    /** 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsUnionOrderBy()
     * @since 4.2
     */
    public boolean supportsUnionOrderBy() {
        return false;
    }
    
    /** 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsUnions()
     * @since 4.2
     */
    public boolean supportsUnions() {
        return false;
    }

    /** 
     * @see com.metamatrix.data.api.ConnectorCapabilities#getMaxInCriteriaSize()
     * @since 4.2
     */
    public int getMaxInCriteriaSize() {
        return this.maxInCriteriaSize;
    }
    
    /** 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsFunctionsInGroupBy()
     * @since 5.0
     */
    public boolean supportsFunctionsInGroupBy() {
        return false;
    }

    public boolean supportsRowLimit() {
        return false;
    }

    public boolean supportsRowOffset() {
        return false;
    }

    /** 
     * @see com.metamatrix.data.api.ConnectorCapabilities#getMaxFromGroups()
     */
    public int getMaxFromGroups() {
        return -1; //-1 indicates no max
    }

    /** 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsExcept()
     */
    public boolean supportsExcept() {
        return false;
    }

    /** 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsIntersect()
     */
    public boolean supportsIntersect() {
        return false;
    }

    /** 
     * @see com.metamatrix.data.api.ConnectorCapabilities#supportsSetQueryOrderBy()
     */
    public boolean supportsSetQueryOrderBy() {
        return false;
    }    
}
