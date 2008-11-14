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

package com.metamatrix.common.object;

public class PropertyDefinitionImplFactory {

    private static final PropertyType DEFAULT_TYPE = PropertyType.STRING;
    private static final Multiplicity DEFAULT_MULTIPLICITY = Multiplicity.getInstance();

    /**
     * Create a new ObjectDefinition instance.
     * @return the new instance
     */
    public ObjectDefinition create() {
        return new PropertyDefinitionImpl();
    }
    /**
     * Create a definition object with the specified name.
     * @param name the name
     * @return the new instance
     */
    public ObjectDefinition create(String name ) {
        return new PropertyDefinitionImpl(name,DEFAULT_TYPE,DEFAULT_MULTIPLICITY);
    }
    /**
     * Create a definition object with the specified set of attributes.
     * @param name the name
     * @param displayName the displayable name for this property; or null
     * if the display name is the same as the property name.
     * @return the new instance
     */
    public ObjectDefinition create(String name, String displayName ) {
        return new PropertyDefinitionImpl(name,displayName,DEFAULT_TYPE,DEFAULT_MULTIPLICITY);
    }
}

