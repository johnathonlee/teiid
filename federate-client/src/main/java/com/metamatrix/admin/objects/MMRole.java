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

package com.metamatrix.admin.objects;

import java.util.Properties;

import com.metamatrix.admin.api.objects.Role;


/** 
 * @since 4.3
 */
public class MMRole extends MMAdminObject implements Role {
    /** 
     * Ctor
     * @param identifier the role identifier should contain the whole
     * dotted notation in one field of the identifier so that the name
     * contains all components.
     * @since 4.3
     */
    public MMRole(String[] identifier) {
        super(identifier);
    }
    
    /**
     * Get the names of all administrative roles in the system. 
     * @return the array of all role names suitable for adding to Principals.
     * @since 4.3
     */
    public static String[] getAvailableRoles() {
        return new String[] {Role.ADMIN_PRODUCT, Role.ADMIN_SYSTEM, Role.ADMIN_READONLY};
    }

    /** 
     * @see com.metamatrix.admin.api.objects.AdminObject#getIdentifier()
     * @since 4.3
     */
    public String getIdentifier() {
        return super.getIdentifier();
    }

    /** 
     * @see com.metamatrix.admin.api.objects.AdminObject#getName()
     * @since 4.3
     */
    public String getName() {
        // A Role name should not be broken into components
        // Role name should have have complete, dotted notation.
        return super.getIdentifier();
    }

    /** 
     * @see com.metamatrix.admin.api.objects.AdminObject#getProperties()
     * @since 4.3
     */
    public Properties getProperties() {
        return null;
    }

    /** 
     * @see com.metamatrix.admin.api.objects.AdminObject#getPropertyValue(java.lang.String)
     * @since 4.3
     */
    public String getPropertyValue(String name) {
        return null;
    }

    /** 
     * @see com.metamatrix.admin.objects.MMAdminObject#toString()
     * @since 4.3
     */
    public String toString() {
        return super.getIdentifier();
    }

}
