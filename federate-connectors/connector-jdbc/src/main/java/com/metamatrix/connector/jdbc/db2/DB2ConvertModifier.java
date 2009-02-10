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

package com.metamatrix.connector.jdbc.db2;

import java.util.ArrayList;
import java.util.List;

import com.metamatrix.connector.api.TypeFacility;
import com.metamatrix.connector.jdbc.extension.FunctionModifier;
import com.metamatrix.connector.jdbc.extension.impl.BasicFunctionModifier;
import com.metamatrix.connector.jdbc.extension.impl.DropFunctionModifier;
import com.metamatrix.connector.language.*;

/**
 */
public class DB2ConvertModifier extends BasicFunctionModifier implements FunctionModifier {

    private static DropFunctionModifier DROP_MODIFIER = new DropFunctionModifier();

    private ILanguageFactory langFactory;
    
    public DB2ConvertModifier(ILanguageFactory langFactory) {
        this.langFactory = langFactory;
    }

    public IExpression modify(IFunction function) {
        IExpression[] args = function.getParameters();
        Class sourceType = args[0].getType();
        String targetTypeString = getTargetType(args[1]);
        Class targetType = TypeFacility.getDataTypeClass(targetTypeString);
        IExpression returnExpr = null;
        
        if(targetType != null) {
        
            // targetType is always lower-case due to getTargetType implementation
            if(targetType.equals(TypeFacility.RUNTIME_TYPES.STRING)) { 
                returnExpr = convertToString(args[0], sourceType);
                
            } else if(targetType.equals(TypeFacility.RUNTIME_TYPES.TIMESTAMP)) { 
                returnExpr = convertToTimestamp(args[0], sourceType);
                
            } else if(targetType.equals(TypeFacility.RUNTIME_TYPES.DATE)) { 
                returnExpr = convertToDate(args[0], sourceType);
                
            } else if(targetType.equals(TypeFacility.RUNTIME_TYPES.TIME)) { 
                returnExpr = convertToTime(args[0], sourceType);
                
            } else if(targetType.equals(TypeFacility.RUNTIME_TYPES.BOOLEAN) || 
                            targetType.equals(TypeFacility.RUNTIME_TYPES.BYTE) || 
                            targetType.equals(TypeFacility.RUNTIME_TYPES.SHORT)) {  
                returnExpr = convertToSmallInt(args[0], sourceType, targetType);
                
            } else if(targetType.equals(TypeFacility.RUNTIME_TYPES.INTEGER)) {  
                returnExpr = convertToInteger(args[0], sourceType);
                
            } else if(targetType.equals(TypeFacility.RUNTIME_TYPES.LONG) || 
                            targetType.equals(TypeFacility.RUNTIME_TYPES.BIG_INTEGER)) {  
                returnExpr = convertToBigInt(args[0], sourceType);
                
            } else if(targetType.equals(TypeFacility.RUNTIME_TYPES.FLOAT)) {  
                returnExpr = convertToReal(args[0], sourceType);

            } else if(targetType.equals(TypeFacility.RUNTIME_TYPES.DOUBLE)) {  
                returnExpr = convertToDouble(args[0], sourceType);

            } else if(targetType.equals(TypeFacility.RUNTIME_TYPES.BIG_DECIMAL)) {  
                returnExpr = convertToBigDecimal(args[0], sourceType);

            } else if(targetType.equals(TypeFacility.RUNTIME_TYPES.CHAR)) { 
                returnExpr = convertToChar(args[0], sourceType);
            } 
            
            if(returnExpr != null) {
                return returnExpr;
            }
        }
        
        // Last resort - just drop the convert and let the db figure it out
        return DROP_MODIFIER.modify(function);  
    }

    /** 
     * @param expression
     * @return
     * @since 4.2
     */
    private String getTargetType(IExpression expression) {
        if(expression != null && expression instanceof ILiteral) {
            String target = (String) ((ILiteral)expression).getValue();
            return target.toLowerCase();
        } 
        
        return null;            
    }
    

    /** 
     * @param expression
     * @param sourceType
     * @return
     * @since 4.2
     */
    private IExpression convertToString(IExpression expression,
                                        Class sourceType) {
        if(sourceType.equals(TypeFacility.RUNTIME_TYPES.BOOLEAN)) {
            // BEFORE: convert(booleanExpression, string)
            // AFTER:  CASE WHEN booleanExpression = 0 THEN 'false' ELSE 'true' END

            ILiteral literalZero = this.langFactory.createLiteral(new Integer(0), TypeFacility.RUNTIME_TYPES.INTEGER);
            ICompareCriteria when = this.langFactory.createCompareCriteria(ICompareCriteria.EQ, expression, literalZero);
            List whens = new ArrayList(1);
            whens.add(when);
            
            ILiteral literalFalse = this.langFactory.createLiteral("false", TypeFacility.RUNTIME_TYPES.STRING); //$NON-NLS-1$
            List thens = new ArrayList(1);
            thens.add(literalFalse);
            
            ILiteral literalTrue = this.langFactory.createLiteral("true", TypeFacility.RUNTIME_TYPES.STRING); //$NON-NLS-1$
                        
            return this.langFactory.createSearchedCaseExpression(whens, thens, literalTrue, TypeFacility.RUNTIME_TYPES.STRING);
            
        } else if(sourceType.equals(TypeFacility.RUNTIME_TYPES.CHAR)) {
            // Drop convert entirely for char
            return null;
            
        } else {
            // BEFORE: convert(EXPR, string) 
            // AFTER:  char(EXPR) 
            return wrapNewFunction(expression, "char", TypeFacility.RUNTIME_TYPES.STRING); //$NON-NLS-1$            
        }
    }
    
    private IExpression convertToChar(IExpression expression,
                                        Class sourceType) {
        if(sourceType.equals(TypeFacility.RUNTIME_TYPES.STRING)) {
            ILiteral literalOne = this.langFactory.createLiteral(new Integer(1), TypeFacility.RUNTIME_TYPES.INTEGER);
            return this.langFactory.createFunction("char", new IExpression[] { expression, literalOne }, TypeFacility.RUNTIME_TYPES.CHAR); //$NON-NLS-1$
        } 
        
        return null;
    }

    private IExpression convertToSmallInt(IExpression expression,
                                        Class sourceType, Class targetType) {
        
        if(sourceType.equals(TypeFacility.RUNTIME_TYPES.STRING) && targetType.equals(TypeFacility.RUNTIME_TYPES.BOOLEAN)) { 
            // BEFORE: convert(stringExpression, boolean)
            // AFTER:  CASE WHEN stringExpression = 'true' THEN 1 ELSE 0 END
            ILiteral literalTrue = this.langFactory.createLiteral("true", TypeFacility.RUNTIME_TYPES.STRING); //$NON-NLS-1$
            ICompareCriteria when = this.langFactory.createCompareCriteria(ICompareCriteria.EQ, expression, literalTrue);
            List whens = new ArrayList(1);
            whens.add(when);
            
            ILiteral literalOne = this.langFactory.createLiteral(new Integer(1), TypeFacility.RUNTIME_TYPES.INTEGER);
            List thens = new ArrayList(1);
            thens.add(literalOne);
            
            ILiteral literalZero = this.langFactory.createLiteral(new Integer(0), TypeFacility.RUNTIME_TYPES.INTEGER);
                                    
            return this.langFactory.createSearchedCaseExpression(whens, thens, literalZero, TypeFacility.RUNTIME_TYPES.STRING);
            
        } else if(sourceType.equals(TypeFacility.RUNTIME_TYPES.BOOLEAN) || 
                        sourceType.equals(TypeFacility.RUNTIME_TYPES.BYTE) || 
                        sourceType.equals(TypeFacility.RUNTIME_TYPES.SHORT)){
            
            // Just drop these
            return null;
        }

        // BEFORE: convert(expression, [boolean,byte,short])
        // AFTER:  smallint(expression)
        return wrapNewFunction(expression, "smallint", targetType); //$NON-NLS-1$
    }

    private IExpression convertToInteger(IExpression expression, Class sourceType) {
          
          if(sourceType.equals(TypeFacility.RUNTIME_TYPES.BOOLEAN) || 
                          sourceType.equals(TypeFacility.RUNTIME_TYPES.BYTE) || 
                          sourceType.equals(TypeFacility.RUNTIME_TYPES.SHORT)){
              
              // Just drop these
              return null;
          } 

          // BEFORE: convert(expression, integer)
          // AFTER:  integer(expression)
          return wrapNewFunction(expression, "integer", TypeFacility.RUNTIME_TYPES.INTEGER); //$NON-NLS-1$
      }

    private IExpression convertToBigInt(IExpression expression, Class sourceType) {
        
        if(sourceType.equals(TypeFacility.RUNTIME_TYPES.STRING) ||
                        sourceType.equals(TypeFacility.RUNTIME_TYPES.FLOAT) || 
                        sourceType.equals(TypeFacility.RUNTIME_TYPES.DOUBLE) || 
                        sourceType.equals(TypeFacility.RUNTIME_TYPES.BIG_DECIMAL)){

            // BEFORE: convert(expression, [long, biginteger])
            // AFTER:  bigint(expression)
            return wrapNewFunction(expression, "bigint", TypeFacility.RUNTIME_TYPES.LONG); //$NON-NLS-1$

        } 

        // Just drop anything else
        return null;
    }

    private IExpression convertToReal(IExpression expression, Class sourceType) {
        
        if(sourceType.equals(TypeFacility.RUNTIME_TYPES.STRING) ||
                        sourceType.equals(TypeFacility.RUNTIME_TYPES.DOUBLE) || 
                        sourceType.equals(TypeFacility.RUNTIME_TYPES.BIG_DECIMAL)){

            // BEFORE: convert(expression, [double, bigdecimal])
            // AFTER:  real(expression)
            return wrapNewFunction(expression, "real", TypeFacility.RUNTIME_TYPES.FLOAT); //$NON-NLS-1$

        } 

        // Just drop anything else
        return null;
    }

    private IExpression convertToDouble(IExpression expression, Class sourceType) {
        
        if(sourceType.equals(TypeFacility.RUNTIME_TYPES.STRING)){

            // BEFORE: convert(expression, double)
            // AFTER:  double(expression)
            return wrapNewFunction(expression, "double", TypeFacility.RUNTIME_TYPES.DOUBLE); //$NON-NLS-1$

        } 

        // Just drop anything else
        return null;
    }

    private IExpression convertToBigDecimal(IExpression expression, Class sourceType) {
        
        if(sourceType.equals(TypeFacility.RUNTIME_TYPES.STRING)){

            // BEFORE: convert(expression, bigdecimal)
            // AFTER:  decimal(expression)
            return wrapNewFunction(expression, "decimal", TypeFacility.RUNTIME_TYPES.BIG_DECIMAL); //$NON-NLS-1$

        } 

        // Just drop anything else
        return null;
    }

    /** 
     * @param expression
     * @param sourceType
     * @return
     * @since 4.2
     */
    private IExpression convertToDate(IExpression expression,
                                      Class sourceType) {
                                
        // BEFORE: convert(EXPR, date) 
        // AFTER:  date(EXPR) 
        return wrapNewFunction(expression, "date", TypeFacility.RUNTIME_TYPES.DATE); //$NON-NLS-1$
    }

    private IExpression convertToTime(IExpression expression, Class sourceType) {
                                
        // BEFORE: convert(EXPR, time) 
        // AFTER:  time(EXPR) 
        return wrapNewFunction(expression, "time", TypeFacility.RUNTIME_TYPES.DATE); //$NON-NLS-1$
    }

    private IExpression convertToTimestamp(IExpression expression,
                                            Class sourceType) {
        
        if(sourceType.equals(TypeFacility.RUNTIME_TYPES.STRING)) {
            // BEFORE: convert(EXPR, timestamp)
            // AFTER:  timestamp(expr)
            return wrapNewFunction(expression, "timestamp", TypeFacility.RUNTIME_TYPES.TIMESTAMP); //$NON-NLS-1$
            
        } else if(sourceType.equals(TypeFacility.RUNTIME_TYPES.DATE)) {
            // BEFORE: convert(EXPR, timestamp)
            // AFTER:  timestamp(EXPR, '00:00:00')
            ILiteral timeString = this.langFactory.createLiteral("00:00:00", TypeFacility.RUNTIME_TYPES.STRING); //$NON-NLS-1$
            return this.langFactory.createFunction("timestamp", new IExpression[] {expression, timeString}, TypeFacility.RUNTIME_TYPES.TIMESTAMP);             //$NON-NLS-1$
            
        } else if(sourceType.equals(TypeFacility.RUNTIME_TYPES.TIME)) {
            // BEFORE: convert(EXPR, timestamp)
            // AFTER:  timestamp('1970-01-01', EXPR)
            ILiteral dateString = this.langFactory.createLiteral("1970-01-01", TypeFacility.RUNTIME_TYPES.STRING); //$NON-NLS-1$
            return this.langFactory.createFunction("timestamp", new IExpression[] {dateString, expression}, TypeFacility.RUNTIME_TYPES.TIMESTAMP);             //$NON-NLS-1$
        }
        
        return null;
    }
    
    /** 
     * @param expression
     * @param functionName
     * @param outputType
     * @return
     * @since 4.2
     */
    private IFunction wrapNewFunction(IExpression expression,
                                      String functionName,
                                      Class outputType) {
        return langFactory.createFunction(functionName, 
            new IExpression[] { expression },
            outputType);
    }

}
