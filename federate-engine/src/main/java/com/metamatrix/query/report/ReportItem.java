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

package com.metamatrix.query.report;

import java.io.Serializable;

/**
 * Represents a single item on a report
 */
public class ReportItem implements Serializable {

	private String type;
	private String message;

	public ReportItem(String type) { 
		this.type = type;
	}
	
	public String getType() { 
	 	return this.type;   
	}
	
    /**
     * Gets the message.
     * @return Returns the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message.
     * @param message The message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String toString() {
    	return getType() + ": " + getMessage();     //$NON-NLS-1$
    }

}
