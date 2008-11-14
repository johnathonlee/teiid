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

package com.metamatrix.admin.api.objects;

/** 
 * A user of the MetaMatrix system may a participant or an
 * administrator of the MetaMatrix system.
 * <p>
 * A User is an actor in the MetaMatrix system.  A User
 * can log in (authenticate) and perform actions on the system.</p>
 * 
 * <p>See {@link Principal} for identity pattern and other 
 * abilities.</p>
 * @since 4.3
 */
public interface User extends Principal {
    
    /**
     * Optional properties for Users
     */
    /** User's common name */
    static final String COMMON_NAME = "commonName"; //$NON-NLS-1$
    /** User's given name */
    static final String GIVEN_NAME = "givenName"; //$NON-NLS-1$
    /** User's surname (last name) */
    static final String SURNAME = "surName"; //$NON-NLS-1$
    /** User's location */
    static final String LOCATION = "location"; //$NON-NLS-1$
    /** User's telephone number */
    static final String TELEPHONE_NUMBER = "telephoneNumber"; //$NON-NLS-1$

}
