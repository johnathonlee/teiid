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
package org.teiid.deployers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.concurrent.ConcurrentSkipListMap;

import org.jboss.deployers.spi.DeploymentException;
import org.teiid.adminapi.AdminException;
import org.teiid.adminapi.AdminProcessingException;
import org.teiid.adminapi.Model;
import org.teiid.adminapi.impl.ModelMetaData;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.core.CoreConstants;
import org.teiid.core.types.DataTypeManager;
import org.teiid.dqp.internal.datamgr.ConnectorManagerRepository;
import org.teiid.logging.LogConstants;
import org.teiid.logging.LogManager;
import org.teiid.metadata.Datatype;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.MetadataStore;
import org.teiid.query.metadata.TransformationMetadata.Resource;
import org.teiid.runtime.RuntimePlugin;
import org.teiid.translator.TranslatorException;
import org.teiid.vdb.runtime.VDBKey;


/**
 * Repository for VDBs
 */
public class VDBRepository implements Serializable{
	private static final long serialVersionUID = 312177538191772674L;
	
	private NavigableMap<VDBKey, CompositeVDB> vdbRepo = new ConcurrentSkipListMap<VDBKey, CompositeVDB>();
	private MetadataStore systemStore;
	private MetadataStore odbcStore;
	private boolean odbcEnabled = false;
	
	public void addVDB(VDBMetaData vdb, MetadataStoreGroup stores, LinkedHashMap<String, Resource> visibilityMap, UDFMetaData udf, ConnectorManagerRepository cmr) throws DeploymentException {
		if (getVDB(vdb.getName(), vdb.getVersion()) != null) {
			throw new DeploymentException(RuntimePlugin.Util.getString("duplicate_vdb", vdb.getName(), vdb.getVersion())); //$NON-NLS-1$
		}
		
		// get the system VDB metadata store
		if (this.systemStore == null) {
			throw new DeploymentException(RuntimePlugin.Util.getString("system_vdb_load_error")); //$NON-NLS-1$
		}	
		
		if (this.odbcEnabled && odbcStore == null) {
			this.odbcStore = getODBCMetadataStore();
		}
		
		if (this.odbcStore == null) {
			addSystemModel(vdb);
			this.vdbRepo.put(vdbId(vdb), new CompositeVDB(vdb, stores, visibilityMap, udf, cmr, this.systemStore));
		}
		else {
			addSystemModel(vdb);
			addODBCModel(vdb);
			this.vdbRepo.put(vdbId(vdb), new CompositeVDB(vdb, stores, visibilityMap, udf, cmr, this.systemStore, odbcStore));
		}
	}

	private void addODBCModel(VDBMetaData vdb) {
		// add the ODBC model
		ModelMetaData odbcSystem = new ModelMetaData();
		odbcSystem.setName(CoreConstants.ODBC_MODEL);
		odbcSystem.setVisible(true);
		odbcSystem.setModelType(Model.Type.VIRTUAL);
		vdb.addModel(odbcSystem);
	}
	
	private void addSystemModel(VDBMetaData vdb) {
		// Add system model to the deployed VDB
		ModelMetaData system = new ModelMetaData();
		system.setName(CoreConstants.SYSTEM_MODEL);
		system.setVisible(true);
		system.setModelType(Model.Type.PHYSICAL);
		system.addSourceMapping(CoreConstants.SYSTEM_MODEL, CoreConstants.SYSTEM_MODEL, CoreConstants.SYSTEM_MODEL); 
		system.setSupportsMultiSourceBindings(false);
		vdb.addModel(system);		
	}
	
	public VDBMetaData getVDB(String name, int version) {
		CompositeVDB v = this.vdbRepo.get(new VDBKey(name, version));
		if (v != null) {
			return v.getVDB();
		}
		return null;
	}
	
	public List<VDBMetaData> getVDBs(){
		ArrayList<VDBMetaData> vdbs = new ArrayList<VDBMetaData>();
		for(CompositeVDB cVDB:this.vdbRepo.values()) {
			vdbs.add(cVDB.getVDB());
		}
		return vdbs;
	}

    protected VDBKey vdbId(VDBMetaData vdb) {
        return new VDBKey(vdb.getName(), vdb.getVersion());
    } 	
		
	public VDBMetaData getVDB(String vdbName) throws VirtualDatabaseException {
    	int latestVersion = 0;
        for (VDBKey key:this.vdbRepo.tailMap(new VDBKey(vdbName, 0)).keySet()) {
            if(!key.getName().equalsIgnoreCase(vdbName)) {
            	break;
            }
        	VDBMetaData vdb = this.vdbRepo.get(key).getVDB();
        	switch (vdb.getConnectionType()) {
        	case ANY:
        		latestVersion = Math.max(vdb.getVersion(), latestVersion);
        		break;
        	case BY_VERSION:
                if (latestVersion == 0) {
            		latestVersion = vdb.getVersion();
                }            	
                break;
        	}
        }
        if(latestVersion == 0) {
            throw new VirtualDatabaseException(RuntimePlugin.Util.getString("VDBService.VDB_does_not_exist._2", vdbName, "latest")); //$NON-NLS-1$ //$NON-NLS-2$ 
        }

        return getVDB(vdbName, latestVersion);
	}
	
	public MetadataStore getSystemStore() {
		return systemStore;
	}
	
	public MetadataStore getODBCStore() {
		return this.odbcStore;
	}	
	
	public void setSystemStore(MetadataStore store) {
		this.systemStore = store;
	}

	private MetadataStore getODBCMetadataStore() {
		MetadataFactory factory = new MetadataFactory(CoreConstants.ODBC_MODEL, getBuiltinDatatypes(), new Properties()); 
		try {
			PgCatalogMetadataStore pg = new PgCatalogMetadataStore(factory);
			return  pg.getMetadataStore();
		} catch (TranslatorException e) {
			LogManager.logError(LogConstants.CTX_DQP, RuntimePlugin.Util.getString("failed_to_load_odbc_metadata")); //$NON-NLS-1$
		}
		return null;
	}
	
	public void odbcEnabled() {
		this.odbcEnabled = true;
	}
	
	public synchronized boolean removeVDB(String vdbName, int vdbVersion) {
		VDBKey key = new VDBKey(vdbName, vdbVersion);
		CompositeVDB removed = this.vdbRepo.remove(key);
		if (removed != null) {
			// if this VDB was part of another VDB; then remove them.
			for (CompositeVDB other:this.vdbRepo.values()) {
				other.removeChild(key);
			}
			return true;
		}
		return false;
	}	
	
	public Map<String, Datatype> getBuiltinDatatypes() {
		Collection<Datatype> datatypes = this.systemStore.getDatatypes();
		Map<String, Datatype> datatypeMap = new HashMap<String, Datatype>();
		for (Class<?> typeClass : DataTypeManager.getAllDataTypeClasses()) {
			for (Datatype datatypeRecordImpl : datatypes) {
				if (datatypeRecordImpl.getJavaClassName().equals(typeClass.getName())) {
					datatypeMap.put(DataTypeManager.getDataTypeName(typeClass), datatypeRecordImpl);
					break;
				}
			}
		}
		return datatypeMap;
	}
	
	public void mergeVDBs(String sourceVDBName, int sourceVDBVersion, String targetVDBName, int targetVDBVersion) throws AdminException{
		CompositeVDB source = this.vdbRepo.get(new VDBKey(sourceVDBName, sourceVDBVersion));
		if (source == null) {
			throw new AdminProcessingException(RuntimePlugin.Util.getString("vdb_not_found", sourceVDBName, sourceVDBVersion)); //$NON-NLS-1$
		}
		
		CompositeVDB target = this.vdbRepo.get(new VDBKey(targetVDBName, targetVDBVersion));
		if (target == null) {
			throw new AdminProcessingException(RuntimePlugin.Util.getString("vdb_not_found", sourceVDBName, sourceVDBVersion)); //$NON-NLS-1$
		}		
		
		// merge them
		target.addChild(source);
	}
	
	// this is called by mc
	public void start() {
		if (this.odbcEnabled) {
			this.odbcStore = getODBCMetadataStore();
		}
	}
	
	public void updateVDB(String name, int version) {
		CompositeVDB v = this.vdbRepo.get(new VDBKey(name, version));
		if (v!= null) {
			v.update(v.getVDB());
		}
	}
}
