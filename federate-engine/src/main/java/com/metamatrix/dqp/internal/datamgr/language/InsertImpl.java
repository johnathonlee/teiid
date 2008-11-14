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

import java.util.List;

import com.metamatrix.data.language.IGroup;
import com.metamatrix.data.language.IInsert;
import com.metamatrix.data.visitor.framework.LanguageObjectVisitor;

public class InsertImpl extends BaseLanguageObject implements IInsert {
    
    private IGroup group = null;
    private List elements = null;
    private List values = null;
  
    public InsertImpl(IGroup group, List elements, List values) {
        this.group = group;
        this.elements = elements;
        this.values = values;
    }
    /**
     * @see com.metamatrix.data.language.IInsert#getGroup()
     */
    public IGroup getGroup() {
        return group;
    }

    /**
     * @see com.metamatrix.data.language.IInsert#getElements()
     */
    public List getElements() {
        return elements;
    }

    /**
     * @see com.metamatrix.data.language.IInsert#getValues()
     */
    public List getValues() {
        return values;
    }

    /**
     * @see com.metamatrix.data.language.ILanguageObject#acceptVisitor(com.metamatrix.data.visitor.LanguageObjectVisitor)
     */
    public void acceptVisitor(LanguageObjectVisitor visitor) {
        visitor.visit(this);
    }
    /* 
     * @see com.metamatrix.data.language.IInsert#setGroup(com.metamatrix.data.language.IGroup)
     */
    public void setGroup(IGroup group) {
        this.group = group;
    }
    /* 
     * @see com.metamatrix.data.language.IInsert#setElements(java.util.List)
     */
    public void setElements(List elements) {
        this.elements = elements;
    }
    /* 
     * @see com.metamatrix.data.language.IInsert#setValues(java.util.List)
     */
    public void setValues(List values) {
        this.values = values;
    }

}
