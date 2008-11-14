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

package com.metamatrix.common.config.api;

/**
 * Date Oct 21, 2002
 *
 * The SharedResourceID instance represents a SharedResource.
 */
public class SharedResourceID extends ComponentObjectID {

    /**
     * Create an instance with the specified full name.  The full name must be one or more atomic
     * name components delimited by this class' delimeter character.
     * @param fullName the string form of the full name from which this object is to be created;
     * never null and never zero-length.
     * @throws IllegalArgumentException if the full name is null
     */
    public SharedResourceID(String resourceName)  {
        super(resourceName);
    }


}
