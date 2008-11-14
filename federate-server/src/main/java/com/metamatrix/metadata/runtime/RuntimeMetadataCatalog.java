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

package com.metamatrix.metadata.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.metamatrix.cache.Cache;
import com.metamatrix.cache.CacheConfiguration;
import com.metamatrix.cache.CacheFactory;
import com.metamatrix.cache.Cache.Type;
import com.metamatrix.cache.CacheConfiguration.Policy;
import com.metamatrix.common.config.CurrentConfiguration;
import com.metamatrix.common.config.ResourceNames;
import com.metamatrix.common.config.api.exceptions.ConfigurationException;
import com.metamatrix.common.connection.ManagedConnectionException;
import com.metamatrix.common.connection.TransactionMgr;
import com.metamatrix.common.log.LogManager;
import com.metamatrix.common.messaging.MessageBus;
import com.metamatrix.common.messaging.NoOpMessageBus;
import com.metamatrix.common.vdb.api.VDBArchive;
import com.metamatrix.core.CoreConstants;
import com.metamatrix.core.event.EventObjectListener;
import com.metamatrix.core.vdb.VDBStatus;
import com.metamatrix.dqp.ResourceFinder;
import com.metamatrix.dqp.service.metadata.QueryMetadataCache;
import com.metamatrix.dqp.service.metadata.SingletonMetadataCacheHolder;
import com.metamatrix.metadata.runtime.api.MetadataSourceAPI;
import com.metamatrix.metadata.runtime.api.Model;
import com.metamatrix.metadata.runtime.api.ModelID;
import com.metamatrix.metadata.runtime.api.RuntimeMetadataPropertyNames;
import com.metamatrix.metadata.runtime.api.VirtualDatabase;
import com.metamatrix.metadata.runtime.api.VirtualDatabaseID;
import com.metamatrix.metadata.runtime.api.VirtualDatabaseMetadata;
import com.metamatrix.metadata.runtime.event.RuntimeMetadataEvent;
import com.metamatrix.metadata.runtime.event.RuntimeMetadataListener;
import com.metamatrix.metadata.runtime.exception.InvalidStateException;
import com.metamatrix.metadata.runtime.exception.VirtualDatabaseDoesNotExistException;
import com.metamatrix.metadata.runtime.exception.VirtualDatabaseException;
import com.metamatrix.metadata.runtime.model.BasicVirtualDatabaseMetadata;
import com.metamatrix.metadata.runtime.model.MetadataCache;
import com.metamatrix.metadata.runtime.model.UpdateController;
import com.metamatrix.metadata.runtime.spi.MetaBaseConnector;
import com.metamatrix.metadata.runtime.util.LogRuntimeMetadataConstants;
import com.metamatrix.metadata.util.ErrorMessageKeys;
import com.metamatrix.metadata.util.LogMessageKeys;
import com.metamatrix.query.metadata.QueryMetadataInterface;

/**
 * <p>The RuntimeMetadataCatalog follows the Singleton pattern so there should be only one instance per VM
 *    of this class.  All consumers of RuntimeMetadata should be calling this class. </p>
 * <p>In order to get runtime metadata, you must first request a <code>VirtualDatabaseMetadata</code>
 *    object based on a specific VirtualDatabase name and version.   Therefore, for each different
 *    VirtualDatabase you need information, you will need to request a different VirtualDatabaseMetadata.
 *    Once you have this object, the metadata for that VirtualDatabase can be interrogated. </p>
 * <p>However, the VirtualDatabaseMetadata is "READ ONLY".  For creating new VirtualDatabases or updating
 *    existing ones, there are methods here that provide that functionality.</p>
 */
public class RuntimeMetadataCatalog  {

    //############################################################################################################################
	//# Static Variables                                                                                                         #
	//############################################################################################################################

    /*
     * Contains cache of models for a given VdbID from the database
     * This cache is primarily for the query service.
     */

    private static Cache vdbModelsCache = null;

    /*
     * Used for update of the virtual database.
     */
    private static UpdateController controller = null;
    
    /*
     * A flag to indicate whether init has been called. Only want to init once.
     */
    private static boolean isInit = false;
    
    /*
     * Contains cache of vdb metadata.  This information
     * is used by the console, not by the query service.
     */
    private static Cache vdbMetadataCache = null;
       
    /**
     * Manages the obtaining of transactions
     */
    private static TransactionMgr transMgr = null;

    /*
     * Cache properties.
     */
    private static Properties allProps;

    /*
     * JMS
     */
    private static MessageBus messageBus;

    /*
     * Whether to persist runtime metadata. Defaults to true.
     */
    private static boolean persist = true;
    
    private static MetadataCache systemModels = null;
    

    //############################################################################################################################
	//# Static Methods                                                                                                           #
	//############################################################################################################################
    
    public static EventObjectListener registerRuntimeMetadataListener(RuntimeMetadataListener listener) throws VirtualDatabaseException {
        init();
        
        try{
            EventObjectListener elistener = new VDBListener(listener);
            messageBus.addListener(RuntimeMetadataEvent.class, elistener);
            return elistener;
        }catch(Exception e){
            String msg = RuntimeMetadataPlugin.Util.getString("RuntimeMetadataCatalog.Error_adding_listener"); //$NON-NLS-1$
            throw new VirtualDatabaseException(e, msg);  
        }
    }
    
    public static void removeRuntimeMetadataListener(EventObjectListener listener) throws VirtualDatabaseException {
        init();
        
        try{
            messageBus.removeListener(RuntimeMetadataEvent.class, listener);
        }catch(Exception e){
//            String msg = RuntimeMetadataPlugin.Util.getString("RuntimeMetadataCatalog.Error_adding_listener");
//            RuntimeMetadataPlugin.Util.log(IStatus.ERROR, e, msg);  
//            throw new VirtualDatabaseException(msg);  //$NON-NLS-1$
        }
    }    


    private static class VDBListener implements EventObjectListener{
        private final RuntimeMetadataListener vdblistener;
        
        public VDBListener(final RuntimeMetadataListener listener) {
            vdblistener = listener;
        }
        // Null CTOR is used when the RuntimeMetadataCatalog is the listener 
        public VDBListener() {
            vdblistener = null;
        }        

        public void processEvent(EventObject obj){
            if(obj instanceof RuntimeMetadataEvent){
                if (vdblistener != null) {
                    vdblistener.processEvent((RuntimeMetadataEvent)obj);
                } else {
                    RuntimeMetadataCatalog.processEvent((RuntimeMetadataEvent)obj);
                }
            }
        }
    }    


    /**
     * Call to create and return a <code>VirtualDatabase</code>, based on the
     * VDBInfo that contains a list of ModelInfo instances for that virtual database.
     * @param vdbInfo contains the VDB information used to create the VDB.
     * @param userName of the person creating the virtual database
     * @param vdbIndexFile the jar file that contains all the index files used by the models in this vdb.
     * @return VirtualDatabaseID for the created VirtualDatabase
     * @exception VirtualDatabaseException is thrown if a problem occurs during the creation process.
     */
    public static VirtualDatabase createVirtualDatabase(VDBArchive vdbArchive, String userName) throws VirtualDatabaseException {
        init();
        
        VirtualDatabase vdb = getUpdateController().createVirtualDatabase(vdbArchive, userName);
                
        fireEvent(vdb.getVirtualDatabaseID(), RuntimeMetadataEvent.ADD_VDB);

        
        return vdb;
    }

    /**
     * Returns a <code>VirtualDatabaseMetadata</code> that contains the <code>VirtualDatabase</code> based on the virtual database name and version.
     * @param vdbID is the VirtualDatabase to retrieve
     * @return VirtualDatabaseMetadata
     * @exception VirtualDatabaseException is thrown if a problem occurs during retrieval process.
     */
    public static VirtualDatabaseMetadata getVirtualDatabaseMetadata(VirtualDatabaseID vdbID ) throws VirtualDatabaseException  {
        return RuntimeMetadataCatalog.getVirtualDatabaseMetadata(vdbID, true);
    }

    /**
     * Returns a <code>VirtualDatabaseMetadata</code> that contains the <code>VirtualDatabase</code> based on the virtual database name and version.
     * If the metadata related to the model details (i.e., groups and elements) is not needed, pass in false
     * to indicate not to load the details and save that overhead in processing.
     * Therefore, if the metadata for this VDB has not been cached yet and <code>false</code> is passed
     * in for includeMetadata, then the details will not be loaded, saving processing overhead.
     * @param vdbID is the VirtualDatabase to retrieve
     * @param includeMetadata indicates whether the metadata details for each model will
     *          be loaded.  
     * @return VirtualDatabaseMetadata
     * @exception VirtualDatabaseException is thrown if a problem occurs during retrieval process.
     */
    private static VirtualDatabaseMetadata getVirtualDatabaseMetadata(VirtualDatabaseID vdbID, boolean includeMetadata ) throws VirtualDatabaseException  {
        init();
        
        LogManager.logTrace(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA,"Creating new BasicVirtualDatabaseMetadata instance for VDB ID \""+vdbID+")\""); //$NON-NLS-1$ //$NON-NLS-2$         
        VirtualDatabaseMetadata vDBMetadata = new BasicVirtualDatabaseMetadata(loadVDB(vdbID, includeMetadata), vdbID);

        return vDBMetadata;
    }

    
    /**
     * Returns the id for the VirtualDatabase that is in an 'Active' state.  If it is not found or not in an active state, a VirtualDatabaseDoesNotExistException will be thrown.
     * @return VirtualDatabaseID that is in an 'Active' state
     * @param vdbName is the name of the VirtualDatabase
     * @param vdbVersion is the version. Default to latest version if it is null.
     * @exception VirtualDatabaseDoesNotExistException is thrown if the VirtualDatabase is not found in an active state
     * @exception VirtualDatabaseException is thrown if a problem occurs during retrieval process.
     */
    public static VirtualDatabaseID getActiveVirtualDatabaseID(String vdbName, String vdbVersion) throws VirtualDatabaseDoesNotExistException, VirtualDatabaseException {
        if(vdbName == null){
            throw new IllegalArgumentException(RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0001) );
        }

        init();

        MetaBaseConnector conn= null;
        VirtualDatabaseID vdbID = null;

        try{
            conn = getReadTransaction();
            vdbID = conn.getActiveVirtualDatabaseID(vdbName, vdbVersion);
        }catch(ManagedConnectionException e){
            LogManager.logError(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0002, vdbName));
            throw new VirtualDatabaseException(e, ErrorMessageKeys.RTMDC_0002, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0002, vdbName) );
        }finally {
            if ( conn != null ) {
                try {
                    conn.close();
                } catch ( Exception e2 ) {
                    LogManager.logError(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, e2, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.GEN_0001));
                }
            }
        }

        if(vdbID == null){
            throw new VirtualDatabaseDoesNotExistException(ErrorMessageKeys.RTMDC_0003, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0003, vdbName, vdbVersion));
        }
        return vdbID;
    }

    /**
     * Returns the id for the VirtualDatabase that is in the repository.  If it is not found or not in an active state, a VirtualDatabaseDoesNotExistException will be thrown.
     * @return VirtualDatabaseID that is in an 'Active' state
     * @param vdbName is the name of the VirtualDatabase
     * @param vdbVersion is the version
     * @exception VirtualDatabaseDoesNotExistException is thrown if the VirtualDatabase is not found in an active state
     * @exception VirtualDatabaseException is thrown if a problem occurs during retrieval process.
     */
    public static VirtualDatabaseID getVirtualDatabaseID(String vdbName, String vdbVersion) throws VirtualDatabaseDoesNotExistException, VirtualDatabaseException {
        if(vdbName == null){
            throw new IllegalArgumentException(RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0001) );
        }

        init();

        MetaBaseConnector conn= null;
        VirtualDatabaseID vdbID = null;

        try{
            conn = getReadTransaction();
            vdbID = conn.getVirtualDatabaseID(vdbName, vdbVersion);

        }catch(ManagedConnectionException e){
            LogManager.logError(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0002, vdbName));
            throw new VirtualDatabaseException(e, ErrorMessageKeys.RTMDC_0002, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0002, vdbName) );
        }finally {
            if ( conn != null ) {
                try {
                    conn.close();
                } catch ( Exception e2 ) {
                    LogManager.logError(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, e2, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.GEN_0001));
                }
            }
        }

        if(vdbID == null){
            throw new VirtualDatabaseDoesNotExistException(ErrorMessageKeys.RTMDC_0003, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0003, vdbName, (vdbVersion==null?"NoVersion":vdbVersion))); //$NON-NLS-1$
        }

        return vdbID;
    }

    /**
     * Updates the <code>VirtualDatabase</code> status.
     * The following four status are valid. They are defined in <code>MetadataConstants</code>:
     * <p>Incomplete: The virtual database is not fully created yet. Set by runtime metadata during the creation of a new virtual database.</p>
     * <p>Inactive: Not ready for use.</p>
     * <p>Active: Ready for use.</p>
     * <p>Deleted: Ready for deletion.</p>
     * @param userName of the person requesting the change
     * @param status is the state the VirtualDatabase should be set to
     * @exception VirtualDatabaseException if unable to perform update.
     */
    public static void setVDBStatus(VirtualDatabaseID virtualDBID, short status, String userName) throws VirtualDatabaseException{
        if(!persist){
        	return;
        }
        try{
            init();
            getUpdateController().setVBDStatus(virtualDBID, status, userName);
            
            // changed to remove from the cache when the state becomes unusabe
            switch (status) {
                case VDBStatus.INACTIVE:
                case VDBStatus.INCOMPLETE:                    
                {
                    RuntimeMetadataCatalog.removeFromMetadataCache(virtualDBID);
                    fireEvent(virtualDBID, RuntimeMetadataEvent.CLEAR_CACHE_FOR_VDB);
                    break;
                }
            }
        }catch(InvalidStateException ie){
            throw new VirtualDatabaseException(ie, ErrorMessageKeys.RTMDC_0004, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0004, virtualDBID.getName() ) );
        }
    }

    /**
     * Returns a <code>Collection</code> of all the VirtualDatabase's in the system.  This would include all virtual databases flagged as incomplete, active, inactive or deleted.
     * @return Collection
     * @throws VirtualDatabaseException is thrown if a problem occurs during the retrieval process.
     */
    public static Collection getVirtualDatabases() throws VirtualDatabaseException  {
        init();
        Collection vdbs;

        MetaBaseConnector conn= null;

        try{
            conn = getReadTransaction();
            vdbs = conn.getVirtualDatabases();
        }catch(ManagedConnectionException e){
            LogManager.logError(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, e, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0005));
            throw new VirtualDatabaseException(e, ErrorMessageKeys.RTMDC_0005, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0005) );
        }finally {
            if ( conn != null ) {
                try {
                    conn.close();
                } catch ( Exception e2 ) {
                    LogManager.logError(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, e2, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.GEN_0001));
                }
            }
        }

        return vdbs;
    }

    /**
     * Returns the VDB Archive specified by the virtual database ID.
     * @return byte[] VDB Archive
     * @throws VirtualDatabaseException is thrown if a problem occurs during the retrieval process.
     */
     public static byte[] getVDBArchive(VirtualDatabaseID vdbID) throws VirtualDatabaseException  {
         byte[] archive = null;
 
         VirtualDatabase vdb = RuntimeMetadataCatalog.getVirtualDatabase(vdbID);
         archive = getUpdateController().getVDBArchive(vdb.getFileName());
         return archive;
     }

    /**
     * Returns the System VDB Archive 
     * @return byte[] VDB Archive
     * @throws VirtualDatabaseException is thrown if a problem occurs during the retrieval process.
     */
     public static byte[] getSystemVDBArchive() throws VirtualDatabaseException  {
          
         return getUpdateController().getVDBArchive(CoreConstants.SYSTEM_VDB);
     }

    /**
     * Returns the virtual database specified by the virtual database ID.
     * @return VirtualDatabase
     * @throws VirtualDatabaseException is thrown if a problem occurs during the retrieval process.
     */
    public static VirtualDatabase getVirtualDatabase(VirtualDatabaseID vdbID) throws VirtualDatabaseException  {
        init();
        VirtualDatabase vdb;
        MetaBaseConnector conn= null;

        try{
            conn = getReadTransaction();
            vdb = conn.getVirtualDatabase(vdbID);
            
        }catch(ManagedConnectionException e){
            LogManager.logError(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0006, vdbID.getName()));
            throw new VirtualDatabaseException(e, ErrorMessageKeys.RTMDC_0006, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0006, vdbID.getName()) );
        }finally {
            if ( conn != null ) {
                try {
                    conn.close();
                } catch ( Exception e2 ) {
                    LogManager.logError(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, e2, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.GEN_0001));
                }
            }
        }

        return vdb;
    }

    /**
     * Returns a collection of VirtualDatabaseID's that are marked for deletion.
     * @return Collection of type VirtualDatabaseID
     * @throws VirtualDatabaseException is thrown if a problem occurs during retrieval process.
     */
    public static Collection getDeletedVirtualDatabaseIDs() throws VirtualDatabaseException {
        init();
        Collection vdbs;
        MetaBaseConnector conn = null;

        try{
            conn = getReadTransaction();
            vdbs = conn.getDeletedVirtualDatabaseIDs();
        }catch(ManagedConnectionException e){
            LogManager.logError(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, e, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0007) );
            throw new VirtualDatabaseException(e, ErrorMessageKeys.RTMDC_0007, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0007) );
        }finally {
            if ( conn != null ) {
                try {
                    conn.close();
                } catch ( Exception e2 ) {
                    LogManager.logError(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, e2, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.GEN_0001));
                }
            }
        }

        return vdbs;
    }

    /**
     * Call to delete a VirtualDatabase from the system. The VirtualDatabase must be marked for deletion, otherwise an InvalidStateException will be thrown.
     * @param vdbID is the VirtualDatabase to be deleted
     * @throws VirtualDatabaseException is thrown if a problem occurs during the deletion process.
     */
    public static void deleteVirtualDatabase(VirtualDatabaseID vdbID) throws VirtualDatabaseException {
        init();
        if(persist){
        	getUpdateController().deleteVirtualDatabase(vdbID);
            fireEvent(vdbID, RuntimeMetadataEvent.DELETE_VDB);
        }
        removeFromCache(vdbID);
    }

     /**
     * call to refresh the runtime metadata properties.
     */
    public static void refreshProperties() throws VirtualDatabaseException {
        init();
    }

    /**
     * Returns a <code>Collection</code> of type <code>Model</code> that represents
     * all the models that where deployed in the specified virtual database. This method
     * shoud only used by Admin Console.
     * @param vdbID is the VirtualDatabaseID
     * @return Collection of type Model
     * @throws VirtualDatabaseException an error occurs while trying to read the data.
     */
    public static Collection getModels(VirtualDatabaseID vdbID) throws VirtualDatabaseException {
 
        Map modelMap = RuntimeMetadataCatalog.getModelMap(vdbID);
        Collection models = new ArrayList(modelMap.size());
        models.addAll(modelMap.values());
        return models;
    }

    /**
     * Returns a <code>Collection</code> of type <code>String</code> that represents
     * all the models that where deployed in the specified virtual database. This method
     * shoud only used by Admin Console.
     * @param vdbID is the VirtualDatabaseID
     * @return Collection of type String (model full name)
     * @throws VirtualDatabaseException an error occurs while trying to read the data.
     */
    public static List getMutiSourcedModels(VirtualDatabaseID vdbID) throws VirtualDatabaseException {
        List models=null;
 
        Map modelMap = RuntimeMetadataCatalog.getModelMap(vdbID);
        if(modelMap != null && modelMap.size() > 0) {
            models = new ArrayList(modelMap.size());
            for (Iterator it=modelMap.keySet().iterator(); it.hasNext();) {
                final Object o=it.next();
                final Model m = (Model) modelMap.get(o);
                if (m.isMultiSourceBindingEnabled()) {
                    models.add(m.getFullName());
                }
            }
        }
        
        if (models == null) {
            return Collections.EMPTY_LIST;
        }

        return models;
    }    

    /**
     * Returns a <code>Map</code> of type <code>Model</code> that represents
     * all the models that where deployed in the specified virtual database. 
     * This method uses a map because the QueryEngine looks for one model at
     * a time and therefore the map cache is more efficient than a list.
     * @param vdbID is the VirtualDatabaseID
     * @return Map of Models, key = model name
     * @throws VirtualDatabaseException an error occurs while trying to read the data.
     */
    private static Map getModelMap(VirtualDatabaseID vdbID) throws VirtualDatabaseException {
        init();
        Map modelMap=null;
        MetaBaseConnector conn= null;
        
        
        Object v = vdbModelsCache.get(vdbID);
        if (v != null) {
            LogManager.logTrace(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, "VDB " + vdbID + " is in cache"); //$NON-NLS-1$ //$NON-NLS-2$
            modelMap = (Map) v;
            return modelMap;
        } 
        LogManager.logTrace(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, "VDB " + vdbID + " is NOT in cache"); //$NON-NLS-1$ //$NON-NLS-2$
        
        try{
        	
            conn = getReadTransaction();
            Collection models = conn.getModels(vdbID);
            modelMap = new HashMap(models.size());
            for (Iterator it=models.iterator(); it.hasNext();) {
                final Model m = (Model) it.next();
                modelMap.put(m.getName(), m);
            }
            vdbModelsCache.put(vdbID, modelMap);

        }catch(ManagedConnectionException e){
            LogManager.logError(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0009, vdbID.getName() ));
            throw new VirtualDatabaseException(e, ErrorMessageKeys.RTMDC_0009, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0009, vdbID.getName() ) );
        }finally {
            if ( conn != null ) {
                try {
                    conn.close();
                } catch ( Exception e2 ) {
                    LogManager.logError(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, e2, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.GEN_0001));
                }
            }
        }
        return modelMap;
    }

    /**
     * Returns the visibility for a resource path that exist in a given vdb
     * @param resourcePath
     * @param vdbID is the id for which the resource must exist
     * @return <code>true</code> if the resource is visible.
     * @since 4.2
     */
    public static boolean isVisible(String resourcePath, VirtualDatabaseID vdbID) throws VirtualDatabaseException {
        VirtualDatabaseMetadata vdbm = RuntimeMetadataCatalog.getVirtualDatabaseMetadata(vdbID, false);
        return vdbm.isVisible(resourcePath);
    }

    /**
     * Returns a <code>Collection</code> of type <code>Model</code> that represents
     * all the models that where deployed in the specified virtual database. This method
     * shoud only used by Admin Console.
     * @param vdbID is the VirtualDatabaseID
     * @return Collection of type Model
     * @throws VirtualDatabaseException an error occurs while trying to read the data.
     */
    public static Model getModel(String modelName, VirtualDatabaseID vdbID) throws VirtualDatabaseException {
 
        Map modelMap = RuntimeMetadataCatalog.getModelMap(vdbID);
        if (modelMap.containsKey(modelName)) {
            Model m = (Model) modelMap.get(modelName);
            return m;
        }
        //only if needed, asked for the model from the VirtualDatabaseMetadata
        //so that the VDB Caching is performed unless necessary
        VirtualDatabaseMetadata vdbm = RuntimeMetadataCatalog.getVirtualDatabaseMetadata(vdbID, false);
        if (vdbm != null) {
            return vdbm.getModel(modelName);
        }

        return null;
    }    

    /**
     * Set connector binding names for models in a virtual database. If the names
     * are set for all the models, the virtual database status is changed to Inactive.
     * @param vdbID is the VirtualDatabaseID
     * @param modelAndCBNames contains Model name and connector binding name pare.
     * @param userName of the person setting the connection binding names for the virtual database.
     * @throws VirtualDatabaseException an error occurs while trying to read the data.
     */
    public static void setConnectorBindingNames(VirtualDatabaseID vdbID, Map modelAndCBNames, String userName)throws VirtualDatabaseException{
        init();
        Collection models = getModels(vdbID);
        Map cNamesByIDs = new HashMap();
        Iterator iter = modelAndCBNames.keySet().iterator();
        Iterator mIter;
        String mName;
        ModelID mID = null;
        try {
            while(iter.hasNext()){
                mName = (String)iter.next();
                if(modelAndCBNames.get(mName) == null) 
                    continue;
                mIter = models.iterator();
                while(mIter.hasNext()){
                    mID = (ModelID)((Model)mIter.next()).getID();
                    if(mID.getFullName().equalsIgnoreCase(mName))
                        break;
                }
                if(mID == null){
                    throw new VirtualDatabaseException(ErrorMessageKeys.RTMDC_0010, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0010, mName, vdbID.getName() ) );
                }
                
                List bindings = (List) modelAndCBNames.get(mName);
                for (Iterator bit=bindings.iterator(); bit.hasNext();) {
                    String bRouting = (String) bit.next();
                    if (CurrentConfiguration.getConfiguration().getConnectorBindingByRoutingID(bRouting) == null) {
                        throw new VirtualDatabaseException(RuntimeMetadataPlugin.Util.getString("RuntimeMetadataCatalog.No_connector_binding_found", new Object[] {mName, bRouting} ) ); //$NON-NLS-1$

                        
                    }
                }
                cNamesByIDs.put(mID, modelAndCBNames.get(mName));
            }
        } catch(ConfigurationException ce) {
            throw new VirtualDatabaseException(ce);
        }
        getUpdateController().setConnectorBindingNames(vdbID, cNamesByIDs, userName);
        refreshCache(vdbID);
        fireEvent(vdbID, RuntimeMetadataEvent.REFRESH_MODELS);
    }

    /**
     * Update VDB attributes. Only the attributes defined in <code>VirtualDatabase.ModifiableAttributes</code>
     * can be modefied. Call VirtualDatabase.update(String attribute, Object value)
     * to update each attribute of the VDB before calling this method.
     * @param vdb VDB to be updated.
     * @param userName of the person updating the virtual database.
     */
    public static void updateVirtualDatabase(VirtualDatabase vdb, String userName) throws VirtualDatabaseException{
        init();
        getUpdateController().updateVirtualDatabase(vdb, userName);
    }

    /**
     * The init method needs to be called prior to executing any other methods.
     * @exception VirtualDatabaseException if the RuntimeMetadataCatalog cannot be initialized.
     */
    private static void init() throws VirtualDatabaseException {

        synchronized(RuntimeMetadataCatalog.class){
            if (isInit) {
		        return;
		    }
            allProps = getProperties();
            LogManager.logDetail(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, RuntimeMetadataPlugin.Util.getString(LogMessageKeys.GEN_0001) );

            transMgr = getTransactionMgr(allProps);
            LogManager.logDetail(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, RuntimeMetadataPlugin.Util.getString(LogMessageKeys.RTMDC_0001));

            
            messageBus = ResourceFinder.getMessageBus();
            try{
                VDBListener l = new VDBListener();

                messageBus.addListener(RuntimeMetadataEvent.class, l);
            }catch(Exception e){
                LogManager.logCritical(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0013));

                //If we can't add the listeners... create the default NoOpMessageBus.
                //Done for defect 4228 2/19/02 LLP
				messageBus= new NoOpMessageBus();
//                    throw new VirtualDatabaseException(me, "Error adding listener.");
            }
            LogManager.logDetail(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, RuntimeMetadataPlugin.Util.getString(LogMessageKeys.RTMDC_0002));

            final CacheFactory factory = ResourceFinder.getCacheFactory();
            CacheConfiguration config = new CacheConfiguration(Policy.MRU, 0, 0); // MRU with no limit on time and nodes
            vdbMetadataCache = factory.get(Type.VDBMETADATA, config); 
            vdbModelsCache = factory.get(Type.VDBMODELS, config);

            controller = new UpdateController(transMgr);
            
            loadSystemMetadataCache();
            
            LogManager.logDetail(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, RuntimeMetadataPlugin.Util.getString(LogMessageKeys.RTMDC_0003));

            //finally, if everything goes well, set init flag to true.
            isInit = true;
        }
    }

    public static QueryMetadataInterface getQueryMetadata(final VirtualDatabaseID vdbID) throws VirtualDatabaseException {
        init();
        QueryMetadataInterface qmi = getQueryMetadataCache().lookupMetadata(vdbID.getName(), vdbID.getVersion());
        if(qmi == null) {
	        try {
	            return getQueryMetadataCache().lookupMetadata(vdbID.getName(), vdbID.getVersion(), getVDBArchive(vdbID));
	        } catch(Exception e) {
	            throw new VirtualDatabaseException(e);
	        }
        }
        return qmi;
    }

    public static QueryMetadataCache getQueryMetadataCache() throws VirtualDatabaseException {
        try {
	        QueryMetadataCache sharedCache = null;
	        if(SingletonMetadataCacheHolder.hasCache()) {
	            sharedCache = SingletonMetadataCacheHolder.getMetadataCache();
	        } else {
	            sharedCache = SingletonMetadataCacheHolder.getMetadataCache(getSystemVDBArchive());
	        }
	        return sharedCache;
	    } catch(Exception e) {
	        throw new VirtualDatabaseException(e);
	    }
    }

    private synchronized static void refreshCache(VirtualDatabaseID vdbID) {
        try {
            // clean up caches
            removeFromCache(vdbID);
            getModels(vdbID);
            // reload query metadta
            getQueryMetadata(vdbID);
            // do not reload the metadatacache, it will be reloaded only if needed
        } catch(VirtualDatabaseException e){
            LogManager.logError(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0009 , e, new Object[]{vdbID.getName()}));
        } 
    }

    /**
     * remove the VirtualDatabaseMetadata from cache, This should be called when
     * the vdb is actually deleted not when an even is fired.
     */
    private synchronized static void removeFromCache(VirtualDatabaseID vdbID) {
        LogManager.logTrace(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, "VDB " + vdbID + " is being removed from cache"); //$NON-NLS-1$ //$NON-NLS-2$
        try {
            // clear all cached query metadata instances and indexes for the given vdb
            getQueryMetadataCache().removeFromCache(vdbID.getName(), vdbID.getVersion());
        } catch(Exception e) {
            LogManager.logError(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, e, RuntimeMetadataPlugin.Util.getString("Error trying to get QueryMetadataCache")); //$NON-NLS-1$
        }


        removeFromMetadataCache(vdbID);
    }
    
    /**
     * remove the VirtualDatabaseMetadata from cache
     */
    private synchronized static void removeFromMetadataCache(VirtualDatabaseID vdbID) {
        LogManager.logTrace(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, "VDB " + vdbID + " is being removed from cache"); //$NON-NLS-1$ //$NON-NLS-2$

        vdbMetadataCache.remove(vdbID);
        
        vdbModelsCache.remove(vdbID);
    }    

    /**
     * clear the cache. This should be called only from the installer, results in
     * removing the temp files for all vdbs.
     */
    public synchronized static void clearCache() throws VirtualDatabaseException{
        LogManager.logTrace(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, "VDB cache is being cleared"); //$NON-NLS-1$
        init();
        vdbMetadataCache.clear();

        vdbModelsCache.clear();

        // clear all cached query metadata instances and indexes for the given vdb
        getQueryMetadataCache().clearCache();
    }    

    /**
     * obtain the properties from the CurrentConfiguration.
     */
    private static Properties getProperties() throws VirtualDatabaseException{
        Properties prop = new Properties();
        //String messageBusURL = null;
        try{
            // obtain the resource connection properties
            Properties resourceProps = CurrentConfiguration.getResourceProperties(ResourceNames.RUNTIME_METADATA);

            Properties runtimeProps = CurrentConfiguration.getProperties();
            String value = runtimeProps.getProperty(RuntimeMetadataPropertyNames.PERSIST);
            if(value != null) {
                persist = Boolean.valueOf(value).booleanValue();
            }
            //messageBusURL = runtimeProps.getProperty(ServicePropertyNames.SERVER_URL);
            //if(messageBusURL == null)
            //    throw new ConfigurationException("Server url not found.");
            prop.putAll(runtimeProps);
            prop.putAll(resourceProps);
        }catch(ConfigurationException ice){
            LogManager.logError(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, ice, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.GEN_0003));
            throw new VirtualDatabaseException(ErrorMessageKeys.GEN_0003, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.GEN_0003) );
        }

        addProperty(prop, RuntimeMetadataPropertyNames.CONNECTION_FACTORY, prop, TransactionMgr.FACTORY );
        return prop;
    }

    private static TransactionMgr getTransactionMgr(Properties props) throws VirtualDatabaseException {
        TransactionMgr aTransMgr = null;
        try {

            aTransMgr = new TransactionMgr(props, "RuntimeMetadata"); //$NON-NLS-1$

        } catch ( Throwable e ) {
            LogManager.logError(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA, e, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0017));
            throw new VirtualDatabaseException(ErrorMessageKeys.RTMDC_0017, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.RTMDC_0017) );
        }
        return aTransMgr;

    }

    protected static void setTransactionManager( final TransactionMgr transactionMgr ) {
        transMgr = transactionMgr;
    }

    private static UpdateController getUpdateController() {
        return controller;
    }

    private static void addProperty(Properties source, String sourceName, Properties props, String propName) {
        String value = source.getProperty(sourceName);
        if (value != null) {
            props.setProperty(propName, value);
        }
    }
    
    private static void fireEvent(VirtualDatabaseID vdbID, int type) {
        if(messageBus != null){
           try{
               messageBus.processEvent(new RuntimeMetadataEvent(new RuntimeMetadataSource(), vdbID, type));
           }catch(Exception e){
               LogManager.logError(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA,e, RuntimeMetadataPlugin.Util.getString(ErrorMessageKeys.GEN_0002));
           }
        }
    }

    private static MetaBaseConnector getReadTransaction() throws ManagedConnectionException {
        return (MetaBaseConnector) transMgr.getReadTransaction();
    }

    // ==================================================================================
    //          R U N T I M E   M E T A D A T A   A D M I N   A P I
    // ==================================================================================

    private static MetadataSourceAPI loadVDB(VirtualDatabaseID vdbID, boolean includeMetadata) throws VirtualDatabaseException{
        MetadataCache mc = null;
        Object v = vdbMetadataCache.get(vdbID);
       
        if (v != null) {
            mc = (MetadataCache) v;
            if (includeMetadata) {
                if (!mc.isModelDetailsLoaded()) {
                    mc.loadModelDetails();
                }
            }
        } else {
            final VirtualDatabase vdb = RuntimeMetadataCatalog.getVirtualDatabase(vdbID);
            final Collection models = getModels(vdbID);
            final byte[] vdbcontents = RuntimeMetadataCatalog.getVDBArchive(vdbID);
            mc = new MetadataCache();
            mc.init(vdb, models, includeMetadata, vdbcontents, systemModels.getModelMap());
            
            vdbMetadataCache.put(vdbID, mc);
            
            LogManager.logTrace(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA,"Creating MetadataCache for " + vdbID); //$NON-NLS-1$
            
        }
        return mc;
    }    

     private static void loadSystemMetadataCache() throws VirtualDatabaseException{   
        
        byte[] systemvdb = RuntimeMetadataCatalog.getSystemVDBArchive();                   

        // don't load metadata, only models
        MetadataCache mc = new MetadataCache();
        
        mc.initSystemVDB(CoreConstants.SYSTEM_VDB, "1", systemvdb); //$NON-NLS-1$

        systemModels = mc;
        
        LogManager.logTrace(LogRuntimeMetadataConstants.CTX_RUNTIME_METADATA,"Creating MetadataCache for systemVDB"); //$NON-NLS-1$
    }

     private static void processEvent(RuntimeMetadataEvent event) {
         if (event.getSource() == null) {            
             if(event.refreshModels()){
                 VirtualDatabaseID vdbID= event.getVirtualDatabaseID();                            
                 refreshCache(vdbID);
             } else if(event.deleteVDB()){
                 removeFromCache(event.getVirtualDatabaseID());
             } else if(event.clearCacheForVDB()){
                 removeFromMetadataCache(event.getVirtualDatabaseID());
             }
         }
     }

    //############################################################################################################################
    //# Constructors                                                                                                             #
    //############################################################################################################################

    /*
     * Prevent construction.
     */
    private RuntimeMetadataCatalog(){}
    
    private static class RuntimeMetadataSource implements java.io.Serializable {
    }    

}