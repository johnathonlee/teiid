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

package com.metamatrix.dqp.internal.datamgr.language;

import java.util.ArrayList;
import java.util.List;

import com.metamatrix.connector.language.IExpression;
import com.metamatrix.connector.language.IFunction;
import com.metamatrix.connector.visitor.framework.LanguageObjectVisitor;

public class FunctionImpl extends BaseLanguageObject implements IFunction {

    private String name;
    private List<IExpression> parameters;
    private Class type;
    
    public FunctionImpl(String name, List<? extends IExpression> params, Class type) {
        this.name = name;
        if (params == null) {
        	this.parameters = new ArrayList<IExpression>(0);
        } else {
        	this.parameters = new ArrayList<IExpression>(params);
        }
        this.type = type;
    }
    
    /**
     * @see com.metamatrix.connector.language.IFunction#getName()
     */
    public String getName() {
        return this.name;
    }

    /**
     * @see com.metamatrix.connector.language.IFunction#getParameters()
     */
    public List<IExpression> getParameters() {
        return parameters;
    }

    /**
     * @see com.metamatrix.connector.language.ILanguageObject#acceptVisitor(com.metamatrix.data.visitor.LanguageObjectVisitor)
     */
    public void acceptVisitor(LanguageObjectVisitor visitor) {
        visitor.visit(this);
    }

    /* 
     * @see com.metamatrix.data.language.IFunction#setName(java.lang.String)
     */
    public void setName(String name) {
        this.name = name;
    }

    /* 
     * @see com.metamatrix.data.language.IExpression#getType()
     */
    public Class getType() {
        return this.type;
    }

    /* 
     * @see com.metamatrix.data.language.IExpression#setType(java.lang.Class)
     */
    public void setType(Class type) {
        this.type = type;
    }

}
