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

package org.teiid.language;

import java.util.*;

import org.teiid.language.AndOr.Operator;


/**
 * Helpful utility methods to work with language interfaces.  
 */
public final class LanguageUtil {

    /** 
     * Can't construct - this contains only static utility methods
     */
    private LanguageUtil() { 
    }

    /**
     * Take a criteria, which may be null, a single IPredicateCriteria or a 
     * complex criteria built using ICompoundCriteria and breaks it apart 
     * at ANDs such that a List of ICriteria conjuncts are returned.  For
     * example, ((a=1 OR b=2) AND (c=3 AND d=4)) would return the list
     * (a=1 OR b=2), c=3, d=4.  If criteria is null, an empty list is 
     * returned.
     * @param criteria Criteria to break, may be null    
     * @return List of ICriteria, never null
     */
    public static final List<Condition> separateCriteriaByAnd(Condition criteria) {
        if(criteria == null) { 
            return Collections.emptyList();
        }
        
        List<Condition> parts = new ArrayList<Condition>();
        separateCriteria(criteria, parts);
        return parts;           
    }
    
    /**
     * Helper method for {@link #separateCriteriaByAnd(Condition)} that 
     * can be called recursively to collect parts.
     * @param crit Crit to break apart
     * @param parts List to add parts to
     */
    private static void separateCriteria(Condition crit, List<Condition> parts) {
        if(crit instanceof AndOr) {
            AndOr compCrit = (AndOr) crit;
            if(compCrit.getOperator() == Operator.AND) {
            	separateCriteria(compCrit.getLeftCondition(), parts);
            	separateCriteria(compCrit.getRightCondition(), parts);
            } else {
                parts.add(crit);    
            }
        } else {
            parts.add(crit);        
        }   
    }

    /**
     * This utility method can be used to combine two criteria using an AND.
     * If both criteria are null, then null will be returned.  If either is null,
     * then the other will be returned.  
     * @param primaryCrit Primary criteria - may be modified
     * @param additionalCrit Won't be modified, but will likely be attached to the returned crit
     * @param languageFactory Will be used to construct new ICompoundCriteria if necessary
     * @return Combined criteria
     */
    public static Condition combineCriteria(Condition primaryCrit, Condition additionalCrit, LanguageFactory languageFactory) {
        if(primaryCrit == null) {
            return additionalCrit;
        } else if(additionalCrit == null) { 
            return primaryCrit;
        } else {
            return languageFactory.createAndOr(Operator.AND, primaryCrit, additionalCrit);
        }               
    }   
    
}
