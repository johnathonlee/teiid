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

/*
 */
package org.teiid.connector.jdbc.translator;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.teiid.connector.api.ExecutionContext;
import org.teiid.connector.api.TypeFacility;
import org.teiid.connector.jdbc.translator.Translator.NullOrder;
import org.teiid.connector.language.Argument;
import org.teiid.connector.language.Call;
import org.teiid.connector.language.Command;
import org.teiid.connector.language.Comparison;
import org.teiid.connector.language.DerivedColumn;
import org.teiid.connector.language.ExpressionValueSource;
import org.teiid.connector.language.Function;
import org.teiid.connector.language.In;
import org.teiid.connector.language.LanguageObject;
import org.teiid.connector.language.Like;
import org.teiid.connector.language.Literal;
import org.teiid.connector.language.SQLReservedWords;
import org.teiid.connector.language.SearchedCase;
import org.teiid.connector.language.SetClause;
import org.teiid.connector.language.SortSpecification;
import org.teiid.connector.language.Argument.Direction;
import org.teiid.connector.language.SQLReservedWords.Tokens;
import org.teiid.connector.language.SetQuery.Operation;
import org.teiid.connector.language.SortSpecification.Ordering;
import org.teiid.connector.visitor.util.SQLStringVisitor;


/**
 * This visitor takes an ICommand and does DBMS-specific conversion on it
 * to produce a SQL String.  This class is expected to be subclassed.
 */
public class SQLConversionVisitor extends SQLStringVisitor{

    private static DecimalFormat DECIMAL_FORMAT = 
        new DecimalFormat("#############################0.0#############################"); //$NON-NLS-1$    
    private static double SCIENTIC_LOW = Math.pow(10, -3);
    private static double SCIENTIC_HIGH = Math.pow(10, 7);
    
    private ExecutionContext context;
    private Translator translator;

    private boolean prepared;
    
    private List preparedValues = new ArrayList();
    
    private Set<LanguageObject> recursionObjects = Collections.newSetFromMap(new IdentityHashMap<LanguageObject, Boolean>());
    private Map<LanguageObject, Object> translations = new IdentityHashMap<LanguageObject, Object>(); 
    
    private boolean replaceWithBinding = false;
    
    public SQLConversionVisitor(Translator translator) {
        this.translator = translator;
        this.prepared = translator.usePreparedStatements();
    }
    
    @Override
    public void append(LanguageObject obj) {
        boolean replacementMode = replaceWithBinding;
        if (obj instanceof Command || obj instanceof Function) {
    	    /*
    	     * In general it is not appropriate to use bind values within a function
    	     * unless the particulars of the function parameters are know.  
    	     * As needed, other visitors or modifiers can set the literals used within
    	     * a particular function as bind variables.  
    	     */
        	this.replaceWithBinding = false;
        }
    	List<?> parts = null;
    	if (!recursionObjects.contains(obj)) {
    		Object trans = this.translations.get(obj);
    		if (trans instanceof List<?>) {
    			parts = (List<?>)trans;
    		} else if (trans instanceof LanguageObject) {
    			obj = (LanguageObject)trans;
    		} else {
    			parts = translator.translate(obj, context);
    			if (parts != null) {
    				this.translations.put(obj, parts);
    			} else {
    				this.translations.put(obj, obj);
    			}
    		}
    	}
		if (parts != null) {
			recursionObjects.add(obj);
			for (Object part : parts) {
			    if(part instanceof LanguageObject) {
			        append((LanguageObject)part);
			    } else {
			        buffer.append(part);
			    }
			}
			recursionObjects.remove(obj);
		} else {
			super.append(obj);
		}
        this.replaceWithBinding = replacementMode;
    }
    
	@Override
	public void visit(SortSpecification obj) {
		super.visit(obj);
		NullOrder nullOrder = this.translator.getDefaultNullOrder();
		if (!this.translator.supportsExplicitNullOrdering() || nullOrder == NullOrder.LOW) {
			return;
		}
		if (obj.getOrdering() == Ordering.ASC) {
			if (nullOrder != NullOrder.FIRST) {
				buffer.append(" NULLS FIRST"); //$NON-NLS-1$
			}
		} else if (nullOrder == NullOrder.FIRST) {
			buffer.append(" NULLS LAST"); //$NON-NLS-1$
		}
	}

    /**
     * @param type
     * @param object
     * @param valuesbuffer
     */
    private void translateSQLType(Class type, Object obj, StringBuilder valuesbuffer) {
        if (obj == null) {
            valuesbuffer.append(SQLReservedWords.NULL);
        } else {
            if(Number.class.isAssignableFrom(type)) {
                boolean useFormatting = false;
                
                if (Double.class.isAssignableFrom(type)){
                    double value = ((Double)obj).doubleValue();
                    useFormatting = (value <= SCIENTIC_LOW || value >= SCIENTIC_HIGH); 
                }
                else if (Float.class.isAssignableFrom(type)){
                    float value = ((Float)obj).floatValue();
                    useFormatting = (value <= SCIENTIC_LOW || value >= SCIENTIC_HIGH);
                }
                // The formatting is to avoid the so-called "scientic-notation"
                // where toString will use for numbers greater than 10p7 and
                // less than 10p-3, where database may not understand.
                if (useFormatting) {
                	synchronized (DECIMAL_FORMAT) {
                        valuesbuffer.append(DECIMAL_FORMAT.format(obj));
					}
                }
                else {
                    valuesbuffer.append(obj);
                }
            } else if(type.equals(TypeFacility.RUNTIME_TYPES.BOOLEAN)) {
                valuesbuffer.append(translator.translateLiteralBoolean((Boolean)obj));
            } else if(type.equals(TypeFacility.RUNTIME_TYPES.TIMESTAMP)) {
                valuesbuffer.append(translator.translateLiteralTimestamp((Timestamp)obj));
            } else if(type.equals(TypeFacility.RUNTIME_TYPES.TIME)) {
                valuesbuffer.append(translator.translateLiteralTime((Time)obj));
            } else if(type.equals(TypeFacility.RUNTIME_TYPES.DATE)) {
                valuesbuffer.append(translator.translateLiteralDate((java.sql.Date)obj));
            } else {
                // If obj is string, toSting() will not create a new String 
                // object, it returns it self, so new object creation. 
                valuesbuffer.append(Tokens.QUOTE)
                      .append(escapeString(obj.toString(), Tokens.QUOTE))
                      .append(Tokens.QUOTE);
            }
        }        
    }

    /**
     * @see org.teiid.connector.visitor.util.SQLStringVisitor#visit(org.teiid.connector.language.Call)
     */
    public void visit(Call obj) {
        this.prepared = true;
        /*
         * preparedValues is now a list of procedure params instead of just values
         */
        this.preparedValues = obj.getArguments();
        buffer.append(generateSqlForStoredProcedure(obj));
    }

    /**
     * @see org.teiid.connector.visitor.util.SQLStringVisitor#visit(org.teiid.connector.language.Literal)
     */
    public void visit(Literal obj) {
        if (this.prepared && (replaceWithBinding || TranslatedCommand.isBindEligible(obj) || obj.isBindValue())) {
            buffer.append(UNDEFINED_PARAM);
            preparedValues.add(obj);
        } else {
            translateSQLType(obj.getType(), obj.getValue(), buffer);
        }
    }
    
    @Override
    public void visit(In obj) {
        replaceWithBinding = true;
        super.visit(obj);
    }

    @Override
    public void visit(Like obj) {
        replaceWithBinding = true;
        super.visit(obj);
    }

    @Override
    public void visit(Comparison obj) {
        replaceWithBinding = true;
        super.visit(obj);
    }

    @Override
    public void visit(ExpressionValueSource obj) {
        replaceWithBinding = true;
        super.visit(obj);
    }
    
    @Override
    public void visit(SetClause obj) {
        replaceWithBinding = true;
        super.visit(obj);
    }
    
    @Override
    public void visit(DerivedColumn obj) {
    	replaceWithBinding = false;
    	super.visit(obj);
    }
    
    @Override
    public void visit(SearchedCase obj) {
    	replaceWithBinding = false;
    	super.visit(obj);
    }

    /**
     * Set the per-command execution context on this visitor. 
     * @param context ExecutionContext
     * @since 4.3
     */
    public void setExecutionContext(ExecutionContext context) {
        this.context = context;
    }
    
    /**
     * Retrieve the per-command execution context for this visitor 
     * (intended for subclasses to use).
     * @return
     * @since 4.3
     */
    protected ExecutionContext getExecutionContext() {
        return this.context;
    }

    protected String getSourceComment(Command command) {
    	return this.translator.getSourceComment(this.context, command);
    }
    
    /**
     * This is a generic implementation. Subclass should override this method
     * if necessary.
     * @param exec The command for the stored procedure.
     * @return String to be executed by CallableStatement.
     */
    protected String generateSqlForStoredProcedure(Call exec) {
        StringBuffer prepareCallBuffer = new StringBuffer();
        prepareCallBuffer.append("{ "); //$NON-NLS-1$

        List<Argument> params = exec.getArguments();

        //check whether a "?" is needed if there are returns
        boolean needQuestionMark = exec.getReturnType() != null;
        
        prepareCallBuffer.append(getSourceComment(exec));
        
        if(needQuestionMark){
            prepareCallBuffer.append("?="); //$NON-NLS-1$
        }

        prepareCallBuffer.append(" call ");//$NON-NLS-1$
        prepareCallBuffer.append(exec.getMetadataObject() != null ? getName(exec.getMetadataObject()) : exec.getProcedureName());
        prepareCallBuffer.append("("); //$NON-NLS-1$

        int numberOfParameters = 0;
        for (Argument param : params) {
            if(param.getDirection() == Direction.IN || param.getDirection() == Direction.OUT || param.getDirection() == Direction.INOUT){
                if(numberOfParameters > 0){
                    prepareCallBuffer.append(","); //$NON-NLS-1$
                }
                prepareCallBuffer.append("?"); //$NON-NLS-1$
                numberOfParameters++;
            }
        }
        prepareCallBuffer.append(")"); //$NON-NLS-1$
        prepareCallBuffer.append("}"); //$NON-NLS-1$
        return prepareCallBuffer.toString();
    }
    
    /** 
     * @return the preparedValues
     */
    List getPreparedValues() {
        return this.preparedValues;
    }
    
    public boolean isPrepared() {
		return prepared;
	}
    
    public void setPrepared(boolean prepared) {
		this.prepared = prepared;
	}
    
    @Override
    protected boolean useAsInGroupAlias() {
    	return this.translator.useAsInGroupAlias();
    }
        
    @Override
    protected boolean useParensForSetQueries() {
    	return translator.useParensForSetQueries();
    }
    	
	@Override
	protected String replaceElementName(String group, String element) {
		return translator.replaceElementName(group, element);
	}
	
	@Override
	protected void appendSetOperation(Operation operation) {
		buffer.append(translator.getSetOperationString(operation));
	}
    
	@Override
    protected boolean useParensForJoins() {
    	return translator.useParensForJoins();
    }
	
	protected boolean useSelectLimit() {
		return translator.useSelectLimit();
	}
	
}
