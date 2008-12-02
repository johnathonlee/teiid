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

import java.util.Date;

/**
 * A Session is a lasting connection between a client and a MetaMatrix System.
 * 
 * A user may be allowed to have multiple sessions active simultaneously.
 * 
 * <p>an Session identifier usally is a number, this gets automatically asssigned 
 * to a connection, when user logs into the system</p>
 * 
 * @since 4.3
 */
public interface Session extends
                        AdminObject {
    
    
    /**
     * The session is open (active).
     */
    public static final int STATE_ACTIVE = 1;

    /**
     * The ejb server has passivated the session - it may become
     *open again in the future
     */
    public static final int STATE_PASSIVATED = 2;

    /**
     * The session is closed - this state cannot change once it
     *is reached.
     */
    public static final int STATE_CLOSED = 3;

    /**
     * The session has expired - this state cannot change once it
     *is reached.
     */
    public static final int STATE_EXPIRED = 4;

    /**
     * The session is terminated - this state cannot change once it
     *is reached.
     */
    public static final int STATE_TERMINATED = 5;
    
    /**
     * The description when the session has expired - this state cannot change once it
     * is reached.
     */    
    public static final String EXPIRED_STATE_DESC = "Expired"; //$NON-NLS-1$
    
    /**
     * The description when the session is open (active).
     */    
    public static final String ACTIVE_STATE_DESC = "Active";//$NON-NLS-1$
    
    /**
     * The description when the session is closed - this state cannot change once it
     * is reached.
     */    
    public static final String CLOSED_STATE_DESC = "Closed";//$NON-NLS-1$
    
    /**
     * The description when the session is terminated - this state cannot change once it
     *is reached.
     */    
    public static final String TERMINATED_STATE_DESC = "Terminated";//$NON-NLS-1$

    /**
     * The description when the ejb server has passivated the session - it may become
     * open again in the future
     */    
    public static final String PASSIVATED_STATE_DESC = "Passivated";//$NON-NLS-1$
    
    /**
     * The description when the state of the session is not known.
     */     
    public static final String UNKNOWN_STATE_DESC = "Unknown";//$NON-NLS-1$
    
    
    
    
    /**
     * Get the Last time Client has check to see if the server is still available
     * 
     * @return Date of the last ping to the server.
     */
    public Date getLastPingTime();
    

    /**
     * Get the Session State as a String. 
     * 
     * @return SessionState
     */
    public String getStateAsString();

    /**
     * Get the Application Name
     * 
     * @return String of the Application Name
     */
    public String getApplicationName();

       
    /**
     * Get the Product Name
     * 
     * @return String of the Product Name
     */
    public String getProductName();

    /**
     * Get the unique MetaMatrix session
     * within a given MetaMatrix System
     * 
     * @return String of the Session ID
     */
    public String getSessionID();

    /**
     * Get the State of the Session 
     * 
     * @return int of the Session's state
     */
    public int getState();

    /**
     * Get User Name for this Session
     * 
     * @return String of UserName
     */
    public String getUserName();

    /**
     * Get the VDB Name for this Session
     * 
     * @return String name of the VDB
     */
    public String getVDBName();

    /**
     * Get the VDB Version for this Session
     * 
     * @return String name/number of the VDB Version
     */
    public String getVDBVersion();
    
    /**
     * Get the IPAddress for this Session
     * @return IPAddress
     */
    public String getIPAddress();
      
 
    /**
     * Get the host name of the machine the client is 
     * accessing from
     * @return IPAddress
     */
    public String getHostName() ;


}