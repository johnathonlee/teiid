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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.metamatrix.api.exception.MetaMatrixComponentException;
import com.metamatrix.common.application.ApplicationEnvironment;
import com.metamatrix.common.application.exception.ApplicationInitializationException;
import com.metamatrix.common.application.exception.ApplicationLifecycleException;
import com.metamatrix.common.config.api.ConnectorBinding;
import com.metamatrix.common.vdb.api.ModelInfo;
import com.metamatrix.common.vdb.api.SystemVdbUtility;
import com.metamatrix.common.vdb.api.VDBArchive;
import com.metamatrix.common.vdb.api.VDBDefn;
import com.metamatrix.core.util.StringUtil;
import com.metamatrix.core.vdb.VDBStatus;
import com.metamatrix.dqp.embedded.DQPEmbeddedPlugin;
import com.metamatrix.dqp.service.DQPServiceNames;
import com.metamatrix.dqp.service.DQPServiceRegistry;
import com.metamatrix.dqp.service.VDBService;
import com.metamatrix.vdb.runtime.BasicModelInfo;
import com.metamatrix.vdb.runtime.BasicVDBDefn;


/** 
 * A VDBService implementation for Embedded DQP.
 * @since 4.3
 */
public class EmbeddedVDBService extends EmbeddedBaseDQPService implements VDBService {   
    static final String[] VDB_STATUS = {"INCOMPLETE", "INACTIVE", "ACTIVE", "DELETED"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    /**
     * ctor 
     * @param configSvc
     * @throws MetaMatrixComponentException
     * @since 4.3
     */
    public EmbeddedVDBService(DQPServiceRegistry svcRegistry) 
        throws MetaMatrixComponentException{
        super(DQPServiceNames.VDB_SERVICE, svcRegistry);
    }
    
    /**
     * Find the VDB in the list of VDBs available 
     * @param name
     * @param version
     * @return vdb if one found; execption otherwise
     * @since 4.3
     */
    VDBArchive getVDB(String vdbName, String vdbVersion) 
        throws MetaMatrixComponentException {        
        VDBArchive vdb = getConfigurationService().getVDB(vdbName, vdbVersion);
        // Make sure VDB is not null
        if (vdb == null) {
            throw new MetaMatrixComponentException(DQPEmbeddedPlugin.Util.getString("VDBService.VDB_does_not_exist._2", new Object[] {vdbName, vdbVersion})); //$NON-NLS-1$    
        }        
        return vdb;
    }
        
    private boolean isSystemModel(String modelName) {
        return modelName.equalsIgnoreCase(SYSTEM_PHYSICAL_MODEL_NAME);
    }
    
    private ModelInfo getModel(VDBDefn vdb, String modelName) {
        Collection c = vdb.getModels();
        Iterator it = c.iterator();
        while (it.hasNext()) {
            ModelInfo model = (ModelInfo)it.next();
            if (model.getName().equals(modelName)) {
                return model;
            }
        }
        return null;
    }
    
    /** 
     * This should changed to connectorBindingNames.
     * @see com.metamatrix.dqp.service.VDBService#getConnectorBindings(java.lang.String, java.lang.String, java.lang.String)
     * @since 4.3
     */
    public List getConnectorBindingNames(String vdbName, String vdbVersion, String modelName) 
        throws MetaMatrixComponentException {

        // If the request for System model, we have a single name always
        if (isSystemModel(modelName)) {
            List list = new ArrayList();
            list.add(SYSTEM_PHYSICAL_MODEL_NAME);
            return list;
        }
        
        // Otherwise get these from the database. 
        VDBArchive vdb = getVDB(vdbName, vdbVersion);
        BasicVDBDefn def = vdb.getConfigurationDef();
        
        ModelInfo m = def.getModel(modelName);
        if(m == null) {
            // For multi-source bindings, the model name has been replaced with a binding name which should be
            // used instead
            List bindingList = new ArrayList();
            bindingList.add(modelName);
            return bindingList;            
        }
        List<String> localNames = m.getConnectorBindingNames();
        Map deployedBindingsMap = def.getConnectorBindings();
        ArrayList<String> matches = new ArrayList<String>();
 		if (localNames != null && !localNames.isEmpty()) {
 			for(String localName:localNames) {
 				ConnectorBinding deployedBinding = (ConnectorBinding)deployedBindingsMap.get(localName);
 				if (deployedBinding != null) {
 					matches.add(deployedBinding.getDeployedName());
 				}
 			}
 		}
 		return matches;
    }
        
    /** 
     * @see com.metamatrix.dqp.service.VDBService#getModelVisibility(java.lang.String, java.lang.String, java.lang.String)
     * @since 4.3
     */
    public int getModelVisibility(String vdbName, String vdbVersion, String modelName) 
        throws MetaMatrixComponentException {
        
        // If this is system Model
        if (isSystemModel(modelName)) {
            return ModelInfo.PRIVATE;
        }
        
        // If this is any of the Public System Models, like JDBC,ODBC system models
        if(SystemVdbUtility.isSystemModelWithSystemTableType(modelName)){
            return ModelInfo.PUBLIC;
        }        
        
        VDBArchive vdb = getVDB(vdbName, vdbVersion);
        VDBDefn def = vdb.getConfigurationDef();
        
        ModelInfo model = getModel(def, modelName);
        if(model != null) {
            return model.getVisibility();
        }

        return ModelInfo.PRIVATE;
    }

    
    /** 
     * @see com.metamatrix.dqp.service.VDBService#getFileVisibility(java.lang.String, java.lang.String, java.lang.String)
     * @since 4.3
     */
    public int getFileVisibility(String vdbName, String vdbVersion, String pathInVDB) 
        throws MetaMatrixComponentException {        

    	String modelName = StringUtil.getFirstToken(StringUtil.getLastToken(pathInVDB, "/"), "."); //$NON-NLS-1$ //$NON-NLS-2$

        // If this is system Model
        if (isSystemModel(modelName)) {
            return ModelInfo.PRIVATE;
        }
        
        // If this is any of the Public System Models, like JDBC,ODBC system models
        if(SystemVdbUtility.isSystemModelWithSystemTableType(modelName)){
            return ModelInfo.PUBLIC;
        }        
        
        VDBArchive vdb = getVDB(vdbName, vdbVersion);
        VDBDefn def = vdb.getConfigurationDef();
        
        ModelInfo model = getModel(def, modelName);
        if(model != null) {
            return model.getVisibility();
        }        
        return def.isVisible(pathInVDB)?ModelInfo.PUBLIC:ModelInfo.PRIVATE;
    }

    /** 
     * @see com.metamatrix.dqp.service.VDBService#getVDBResource(java.lang.String, java.lang.String)
     * @since 4.3
     */
    public InputStream getVDBResource(String vdbName, String vdbVersion) throws MetaMatrixComponentException {        
        try {
			VDBArchive vdb = getVDB(vdbName, vdbVersion);
			return vdb.getInputStream();
		} catch (IOException e) {
			throw new MetaMatrixComponentException(e);
		}
    }

    /** 
     * @see com.metamatrix.dqp.service.VDBService#getMultiSourceModels(java.lang.String, java.lang.String)
     * @since 4.3
     */
    public List<String> getMultiSourceModels(String vdbName, String vdbVersion) 
        throws MetaMatrixComponentException {
                
        VDBArchive vdb = getVDB(vdbName, vdbVersion);
        BasicVDBDefn def = vdb.getConfigurationDef();
        
        List<String> multiSourceModels = new ArrayList(); 
        Collection<BasicModelInfo> models = def.getModels();
        for(BasicModelInfo model:models) {
        	if(model.isMultiSourceBindingEnabled()) {
        		multiSourceModels.add(model.getName());
        	}
        }
        
        return multiSourceModels;
    }


    /** 
     * @see com.metamatrix.dqp.service.VDBService#getAvailableVDBs()
     * @since 4.3
     */
    public List<VDBArchive> getAvailableVDBs() throws MetaMatrixComponentException {
        List<VDBArchive> fullList = getConfigurationService().getVDBs();
        ArrayList activeList = new ArrayList();
        for (VDBArchive vdb: fullList) {
            if (vdb.getStatus() != VDBStatus.DELETED) {
                activeList.add(vdb);
            }
        }
        return activeList;
    }
    
    /** 
     * @see com.metamatrix.dqp.service.VDBService#getVDBStatus(java.lang.String, java.lang.String)
     * @since 4.3
     */
    public int getVDBStatus(String vdbName, String vdbVersion) 
        throws MetaMatrixComponentException {
        VDBArchive vdb = getVDB(vdbName, vdbVersion);        
        return vdb.getStatus();
    }

    /** 
     * @see com.metamatrix.dqp.service.VDBService#changeVDBStatus(java.lang.String, java.lang.String, int)
     * @since 4.3
     */
    public void changeVDBStatus(String vdbName, String vdbVersion, int status) 
        throws MetaMatrixComponentException {
        
        VDBArchive vdb = getVDB(vdbName, vdbVersion);
        
        int currentStatus = vdb.getStatus();
        
        if (status != currentStatus) {            
            // Change the VDB's status
        	VDBArchive sameVdb = vdb;
            if (currentStatus != VDBStatus.ACTIVE
                    && (status == VDBStatus.ACTIVE || status == VDBStatus.ACTIVE_DEFAULT) ) {
                if (!isValidVDB(sameVdb) || !isFullyConfiguredVDB(sameVdb)) {
                    throw new MetaMatrixComponentException(DQPEmbeddedPlugin.Util.getString("VDBService.vdb_missing_bindings", new Object[] {vdb.getName(), vdb.getVersion()})); //$NON-NLS-1$
                }
            }
            sameVdb.setStatus((short)status);

            // make sure we got what we asked for
            if (status != sameVdb.getStatus()) {
                throw new MetaMatrixComponentException(DQPEmbeddedPlugin.Util.getString("VDBService.vdb_change_status_failed", new Object[] {vdbName, vdbVersion, VDB_STATUS[currentStatus-1], VDB_STATUS[status-1]})); //$NON-NLS-1$                
            }
            
            // now save the change in the configuration. 
            getConfigurationService().saveVDB(vdb, vdb.getVersion());
            DQPEmbeddedPlugin.logInfo("VDBService.vdb_change_status", new Object[] {vdbName, vdbVersion, VDB_STATUS[currentStatus-1], VDB_STATUS[status-1]}); //$NON-NLS-1$
        }
    }
    
    
    //TODO: to be removed later..
    public String getConnectorName(String connectorBindingID) {
        return connectorBindingID;
    }
    
    /** 
     * @see com.metamatrix.dqp.embedded.services.EmbeddedBaseDQPService#initializeService(java.util.Properties)
     * @since 4.3
     */
    public void initializeService(Properties properties) throws ApplicationInitializationException {
    }
    
    /** 
     * @see com.metamatrix.dqp.embedded.services.EmbeddedBaseDQPService#start(com.metamatrix.common.application.ApplicationEnvironment)
     * @since 4.3
     */
    public void startService(ApplicationEnvironment environment) throws ApplicationLifecycleException {        
        // deploying VDB at this stage created issues with data service prematurely
        // asking for unfinished VDB and starting it
    }

    /** 
     * @see com.metamatrix.dqp.embedded.services.EmbeddedBaseDQPService#bindService()
     * @since 4.3
     */
    public void bindService() throws ApplicationLifecycleException {
    }

    /** 
     * @see com.metamatrix.dqp.embedded.services.EmbeddedBaseDQPService#unbindService()
     * @since 4.3
     */
    public void unbindService() throws ApplicationLifecycleException {
    }

    /** 
     * @see com.metamatrix.dqp.embedded.services.EmbeddedBaseDQPService#stopService()
     * @since 4.3
     */
    public void stopService() throws ApplicationLifecycleException {
    }    
}
