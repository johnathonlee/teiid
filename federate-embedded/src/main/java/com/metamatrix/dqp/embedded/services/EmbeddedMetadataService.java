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

package com.metamatrix.dqp.embedded.services;

import java.util.Properties;

import com.metamatrix.api.exception.MetaMatrixComponentException;
import com.metamatrix.common.application.ApplicationEnvironment;
import com.metamatrix.common.application.exception.ApplicationInitializationException;
import com.metamatrix.common.application.exception.ApplicationLifecycleException;
import com.metamatrix.connector.metadata.internal.IObjectSource;
import com.metamatrix.dqp.service.ConfigurationService;
import com.metamatrix.dqp.service.DQPServiceNames;
import com.metamatrix.dqp.service.MetadataService;
import com.metamatrix.dqp.service.VDBLifeCycleListener;
import com.metamatrix.dqp.service.VDBService;
import com.metamatrix.dqp.service.metadata.IndexSelectorSource;
import com.metamatrix.dqp.service.metadata.QueryMetadataCache;
import com.metamatrix.query.metadata.QueryMetadataInterface;


/** 
 * @since 4.3
 */
public class EmbeddedMetadataService extends EmbeddedBaseDQPService implements MetadataService, IndexSelectorSource {

    private QueryMetadataCache metadataCache = null;    
    private VDBLifeCycleListener listener = new VDBLifeCycleListener() {
        public void loaded(String vdbName, String vdbVersion) {
        }
        public void unloaded(String vdbName, String vdbVersion) {
           metadataCache.removeFromCache(vdbName, vdbVersion);
        }            
    };

    /** 
     * @see com.metamatrix.dqp.embedded.services.EmbeddedBaseDQPService#initializeService(java.util.Properties)
     * @since 4.3
     */
    public void initializeService(Properties properties) throws ApplicationInitializationException {
    }

    /** 
     * @see com.metamatrix.dqp.embedded.services.EmbeddedBaseDQPService#startService(com.metamatrix.common.application.ApplicationEnvironment)
     * @since 4.3
     */
    public void startService(ApplicationEnvironment environment) throws ApplicationLifecycleException {
        try {
            ConfigurationService configSvc = this.getConfigurationService();
            this.metadataCache = new QueryMetadataCache(configSvc.getSystemVdb());            
            configSvc.register(listener);
        } catch (MetaMatrixComponentException e) {
            throw new ApplicationLifecycleException(e);
        }
    }

    /** 
     * @see com.metamatrix.dqp.embedded.services.EmbeddedBaseDQPService#stopService()
     * @since 4.3
     */
    public void stopService() throws ApplicationLifecycleException {
    	getConfigurationService().unregister(this.listener);
        this.metadataCache.clearCache();
    }

    /** 
     * @see com.metamatrix.dqp.service.MetadataService#lookupMetadata(java.lang.String, java.lang.String)
     * @since 4.3
     */
    public QueryMetadataInterface lookupMetadata(String vdbName, String vdbVersion) 
        throws MetaMatrixComponentException {
        
        QueryMetadataInterface qmi = this.metadataCache.lookupMetadata(vdbName, vdbVersion);
        if(qmi == null) {                        
            // First see if the vdbService can give the contents directly 
        	VDBService vdbService = ((VDBService)lookupService(DQPServiceNames.VDB_SERVICE));
            return this.metadataCache.lookupMetadata(vdbName, vdbVersion, vdbService.getVDBResource(vdbName, vdbVersion));
        }
        return qmi;
    }
    

	public IObjectSource getMetadataObjectSource(String vdbName, String vdbVersion) throws MetaMatrixComponentException {
		VDBService vdbService = (VDBService)lookupService(DQPServiceNames.VDB_SERVICE);
		return this.metadataCache.getCompositeMetadataObjectSource(vdbName, vdbVersion, vdbService);	
	}

}
