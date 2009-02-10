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

import com.metamatrix.connector.language.ILiteral;
import com.metamatrix.connector.visitor.framework.LanguageObjectVisitor;

public class LiteralImpl extends BaseLanguageObject implements ILiteral {
    
    Object value;
    Class type;
    private boolean bindValue = false;
    
    public LiteralImpl(Object value, Class type) {
        this.value = value;
        this.type = type;
    }
    
    /**
     * @see com.metamatrix.connector.language.ILiteral#getValue()
     */
    public Object getValue() {
        return this.value;
    }

    /**
     * @see com.metamatrix.connector.language.ILanguageObject#acceptVisitor(com.metamatrix.data.visitor.LanguageObjectVisitor)
     */
    public void acceptVisitor(LanguageObjectVisitor visitor) {
        visitor.visit(this);
    }

    /* 
     * @see com.metamatrix.data.language.ILiteral#getType()
     */
    public Class getType() {
        return this.type;
    }

    /* 
     * @see com.metamatrix.data.language.ILiteral#setValue(java.lang.Object)
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /* 
     * @see com.metamatrix.data.language.ILiteral#setType(java.lang.Class)
     */
    public void setType(Class type) {
        this.type = type;
    }

    /** 
     * @see com.metamatrix.connector.language.ILiteral#isBindValue()
     */
    public boolean isBindValue() {
        return bindValue;
    }

    /** 
     * @see com.metamatrix.connector.language.ILiteral#setBindValue(boolean)
     */
    public void setBindValue(boolean bindValue) {
        this.bindValue = bindValue;
    }

}
