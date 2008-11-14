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

package com.metamatrix.platform.vm.api.controller;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;

import com.metamatrix.admin.api.exception.AdminException;
import com.metamatrix.api.exception.MultipleException;
import com.metamatrix.common.config.api.ServiceComponentDefnID;
import com.metamatrix.common.log.LogConfiguration;
import com.metamatrix.platform.service.api.ServiceID;
import com.metamatrix.platform.service.api.exception.ServiceException;
import com.metamatrix.platform.vm.controller.VMControllerID;
import com.metamatrix.platform.vm.controller.VMStatistics;

public interface VMControllerInterface extends Remote {
    
	/**
	 * Starts the VM by invoking all the deployed services
	 * @throws ServiceException
	 * @throws RemoteException
	 */
	public void startVM() throws ServiceException, RemoteException;
	
	/**
	 *  Start the service identified by the ServiceComponentID
	 *  If synch is true then wait for service to start before returning.
	 *  Any exceptions will then be thrown to the caller.
	 *  If synch is false then start service asynchronously.
	 */
	public void startDeployedService(ServiceComponentDefnID id) throws ServiceException, RemoteException;

	/**
	 * Start a previously stopped service
	 */
	void startService(ServiceID serviceID) throws ServiceException, RemoteException;

	/**
	 * Kill all services (waiting for work to complete) and then kill the vm.
	 */
	void stopVM() throws ServiceException, RemoteException;

	/**
	 * Kill all services now, do not wait for work to complete, do not collect $200
	 */
	void stopVMNow() throws ServiceException, RemoteException;

	/**
	 * Kill service once work is complete
	 */
	void stopService(ServiceID id) throws ServiceException, RemoteException;

	/**
	 * Kill service now!!!
	 */
	void stopServiceNow(ServiceID id) throws ServiceException, RemoteException;

    /**
     * Kill all services once work is complete
     */
    void stopAllServices() throws MultipleException, ServiceException, RemoteException;

    /**
     * Kill all services now
     */
    void stopAllServicesNow() throws MultipleException, ServiceException, RemoteException;
    
    /**
     * Check the state of a service
     */
    void checkService(ServiceID serviceID) throws ServiceException, RemoteException;

	
    /**
     * Return current log configuration.
     */
    LogConfiguration getCurrentLogConfiguration() throws RemoteException;

    /**
     * Set the current log configuration.
     */
    void setCurrentLogConfiguration(LogConfiguration logConfiguration) throws RemoteException;

	/**
	 * Get the time the VM was initialized.
	 */
    Date getStartTime() throws RemoteException;

	/**
	 * Get the name of the host this VM is running on.
	 */
    String getHostname() throws RemoteException;

	/**
	 * Get the ID for this controller.
	 */
    VMControllerID getID() throws RemoteException;

	/**
	 * Get the name for this controller.
	 */
    String getName() throws RemoteException;

	/**
	 * Method called from registries to determine if VMController is alive.
	 */
	void ping() throws RemoteException;

    /**
     * Shut down all services waiting for work to complete.
     * Essential services will also be shutdown.
     */
    void shutdown() throws ServiceException, RemoteException;

    /**
     * Shut down all services without waiting for work to complete.
     * Essential services will also be shutdown.
     */
    void shutdownNow() throws ServiceException, RemoteException;

    /**
     * Shut down service waiting for work to complete.
     * Essential services will also be shutdown.
     */
    void shutdownService(ServiceID serviceID) throws ServiceException, RemoteException;

    /**
     * Shut down all services without waiting for work to complete.
     * Essential services will also be shutdown.
     */
    void shutdownServiceNow(ServiceID serviceID) throws ServiceException, RemoteException;

    /**
     * Returns true if system is being shutdown.
     */
    boolean isShuttingDown() throws RemoteException;

    /**
     * Return information about VM.
     * totalMemory, freeMemory, threadCount
     */
    VMStatistics getVMStatistics() throws RemoteException;

    /**
     * dumps stack trace to log file.
     */
    void dumpThreads() throws RemoteException;

    /**
     * Run GC on vm.
     */
    void runGC() throws RemoteException;

    
    /**
     * Export the server logs to a byte[].  The bytes contain the contents of a .zip file containing the logs. 
     * This will export all logs on the host that contains this VMController.
     * @return the logs, as a byte[].
     * @throws AdminException
     * @since 4.3
     */
    byte[] exportLogs() throws ServiceException, RemoteException;
}

