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

package com.metamatrix.platform.security.api;

import java.io.Serializable;
import java.util.UUID;

/**
 * This immutable class represents an identifier for a unique MetaMatrix session within a given MetaMatrix System. This object
 * will be returned to the Client when login to the MetaMatrix Server
 */
public final class MetaMatrixSessionID implements
                                      Serializable,
                                      Comparable<MetaMatrixSessionID> {

    public final static long serialVersionUID = -7872739911360962975L;
    
    private UUID id;
    
    /**
     * Used to create a deterministic id, mostly called by tests
     */
    public MetaMatrixSessionID(long id) {
    	this.id = new UUID(id, id);
    }
    
    public MetaMatrixSessionID() {
    	this.id = UUID.randomUUID();
    }
    
    /**
     * Converts the given string into a session id.
     * @throws IllegalArgumentException if id is not valid
     * @param id
     */
    public MetaMatrixSessionID(String id) {
    	this.id = UUID.fromString(id);
    }

    /**
     * Compares this object to another. If the specified object is an instance of the MetaMatrixSessionID class, then this method
     * compares the contents; otherwise, it throws a ClassCastException (as instances are comparable only to instances of the same
     *  class).
     * <p>
     * Note: this method <i>is </i> consistent with <code>equals()</code>, meaning that
     * <code>(compare(x, y)==0) == (x.equals(y))</code>.
     * <p>
     * 
     * @param obj
     *            the object that this instance is to be compared to.
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the
     *         specified object, respectively.
     * @throws NullPointerException
     *             if the specified object reference is null
     * @throws ClassCastException
     *             if the specified object's type prevents it from being compared to this instance.
     */
    public int compareTo(MetaMatrixSessionID obj) {
        return this.id.compareTo(obj.id);
    }
    /**
     * Returns true if the specified object is semantically equal to this instance. Note: this method is consistent with
     * <code>compareTo()</code>.
     * 
     * @param obj
     *            the object that this instance is to be compared to.
     * @return whether the object is equal to this object.
     */
    public boolean equals(Object obj) {
        // Check if instances are identical ...
        if (this == obj) {
            return true;
        }

        // Check if object can be compared to this one
        // (this includes checking for null ) ...
        if (obj instanceof MetaMatrixSessionID) {
            MetaMatrixSessionID that = (MetaMatrixSessionID)obj;
        	return this.id.equals(that.id);
        }

        // Otherwise not comparable ...
        return false;
    }

    /**
     * Returns the hash code value for this object.
     * 
     * @return a hash code value for this object.
     */
    public final int hashCode() {
        return id.hashCode();
    }

    /**
     * Returns a string representing the current state of the object.
     * 
     * @return the string representation of this instance.
     */
    public final String toString() {
        return id.toString();
    }

}

