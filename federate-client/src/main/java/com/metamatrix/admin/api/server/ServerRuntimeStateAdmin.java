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

package com.metamatrix.admin.api.server;

import com.metamatrix.admin.api.core.CoreRuntimeStateAdmin;
import com.metamatrix.admin.api.exception.AdminException;


/**
 * Interface that exposes the MetaMatrix system for runtime control.
 *
 * <p>Clients should <i>not</i> code directly to this interface but
 * should instead use {@link ServerAdmin}.</p>
 * @since 4.3
 */
public interface ServerRuntimeStateAdmin extends CoreRuntimeStateAdmin {

    /**
     * Stop the entire system.
     * @throws AdminException if there's a system error.
     * @since 4.3
     */
    void stopSystem() throws AdminException;
    
    
    /**
     * Stop and restart the entire system.
     * @param waitUntilDone If true, this method waits until the operation is finished before returning.  
     * This may take a long time to complete.  If false, this method returns immediately, even though the operation 
     * may not be finished.
     * NOTE: If <code>waitUntilDone</code> is true, this method will wait until the server is able to connect to,
     * but it may not wait until non-essential services are available.
     * @throws AdminException if there's a system error.
     * @since 4.3
     */
    void bounceSystem(boolean waitUntilDone) throws AdminException;

    /**
     * Synchronize the entire system.  Synchronization will attempt to make the
     * runtime state match the configuration.
     * @param waitUntilDone If true, this method waits until the operation is finished before returning.  
     * This may take a long time to complete.  If false, this method returns immediately, even though the operation 
     * may not be finished.
     * @throws AdminException if there's a system error.
     * @since 4.3
     */
    void synchronizeSystem(boolean waitUntilDone) throws AdminException;

    /**
     * Start the Host for this cluster
     * @param hostIdentifier the unique identifier for for a {@link com.metamatrix.admin.api.objects.Host Host}
     * in the system.
     * @param waitUntilDone If true, this method waits until the operation is finished before returning.  
     * This may take a long time to complete.  If false, this method returns immediately, even though the operation 
     * may not be finished.
     * @throws AdminException if there's a system error.
     * @since 4.3
     */
    void startHost(String hostIdentifier, boolean waitUntilDone) throws AdminException;

    /**
     * Stop the Host for this cluster
     * @param hostIdentifier the unique identifier for for a {@link com.metamatrix.admin.api.objects.Host Host}
     * in the system.
     * @param stopNow  If true, stop the host forcefully.  If false, wait until any pending work is done.
     * @param waitUntilDone If true, this method waits until the operation is finished before returning.  
     * This may take a long time to complete.  If false, this method returns immediately, even though the operation 
     * may not be finished.
     * @throws AdminException if there's a system error.
     * @since 4.3
     */
    void stopHost(String hostIdentifier, boolean stopNow, boolean waitUntilDone) throws AdminException;

    /**
     * Start a Process in the system.
     * @param processIdentifier the unique identifier for for a
     * {@link com.metamatrix.admin.api.objects.ProcessObject ProcessObject} in the system.
     * @param waitUntilDone If true, this method waits until the operation is finished before returning.  
     * This may take a long time to complete.  If false, this method returns immediately, even though the operation 
     * may not be finished.
     * @throws AdminException if there's a system error.
     * @since 4.3
     */
    void startProcess(String processIdentifier, boolean waitUntilDone) throws AdminException;

    /**
     * Stop Process running in the system.
     * @param processIdentifier the unique identifier for for a
     * {@link com.metamatrix.admin.api.objects.ProcessObject ProcessObject} in the system.
     * @param stopNow a <code>boolean</code> value indicating whether to halt the process immediately
     * or let it finish processing first.
     * @param waitUntilDone If true, this method waits until the operation is finished before returning.  
     * This may take a long time to complete.  If false, this method returns immediately, even though the operation 
     * may not be finished.
     * @throws AdminException if there's a system error.
     * @since 4.3
     */
    void stopProcess(String processIdentifier, boolean stopNow, boolean waitUntilDone) throws AdminException;
}
