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

import java.util.Collection;
import java.util.LinkedHashMap;

import org.teiid.adminapi.DataPolicy;
import org.teiid.adminapi.Model;
import org.teiid.adminapi.impl.DataPolicyMetadata;
import org.teiid.adminapi.impl.ModelMetaData;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.dqp.internal.datamgr.ConnectorManagerRepository;
import org.teiid.metadata.MetadataStore;
import org.teiid.query.function.metadata.FunctionMethod;
import org.teiid.query.metadata.CompositeMetadataStore;
import org.teiid.query.metadata.QueryMetadataInterface;
import org.teiid.query.metadata.TransformationMetadata;
import org.teiid.query.metadata.TransformationMetadata.Resource;
import org.teiid.query.tempdata.TempTableStore;
import org.teiid.vdb.runtime.VDBKey;



public class CompositeVDB {
	private VDBMetaData vdb;
	private MetadataStoreGroup stores;
	private LinkedHashMap<String, Resource> visibilityMap;
	private UDFMetaData udf;
	private LinkedHashMap<VDBKey, CompositeVDB> children;
	private MetadataStore[] additionalStores;
	private ConnectorManagerRepository cmr;
	
	// used as cached item to avoid rebuilding
	private VDBMetaData mergedVDB;
	
	public CompositeVDB(VDBMetaData vdb, MetadataStoreGroup stores, LinkedHashMap<String, Resource> visibilityMap, UDFMetaData udf, ConnectorManagerRepository cmr, MetadataStore... additionalStores) {
		this.vdb = vdb;
		this.stores = stores;
		this.visibilityMap = visibilityMap;
		this.udf = udf;
		this.cmr = cmr;
		this.additionalStores = additionalStores;
		this.vdb.addAttchment(ConnectorManagerRepository.class, cmr);
		update(this.vdb);
	}
	
	public void addChild(CompositeVDB child) {
		if (this.children == null) {
			this.children = new LinkedHashMap<VDBKey, CompositeVDB>();
		}
		VDBMetaData childVDB = child.getVDB();
		this.children.put(new VDBKey(childVDB.getName(), childVDB.getVersion()), child);
		this.mergedVDB = null;
	}
	
	public void removeChild(VDBKey child) {
		if (this.children != null) {
			this.children.remove(child);
		}
		this.mergedVDB = null;
	}	
	
	void update(VDBMetaData vdbMetadata) {
		TransformationMetadata metadata = buildTransformationMetaData(vdbMetadata, getVisibilityMap(), getMetadataStores(), getUDF());
		vdbMetadata.addAttchment(QueryMetadataInterface.class, metadata);
		vdbMetadata.addAttchment(TransformationMetadata.class, metadata);	
		TempTableStore globalTables = new TempTableStore("SYSTEM"); //$NON-NLS-1$
		vdbMetadata.addAttchment(TempTableStore.class, globalTables); 
	}
	
	private TransformationMetadata buildTransformationMetaData(VDBMetaData vdb, LinkedHashMap<String, Resource> visibilityMap, MetadataStoreGroup stores, UDFMetaData udf) {
		Collection <FunctionMethod> methods = null;
		if (udf != null) {
			methods = udf.getFunctions();
		}
		
		CompositeMetadataStore compositeStore = new CompositeMetadataStore(stores.getStores());
		for (MetadataStore s:this.additionalStores) {
			compositeStore.addMetadataStore(s);
		}
		
		TransformationMetadata metadata =  new TransformationMetadata(vdb, compositeStore, visibilityMap, methods);
				
		return metadata;
	}	
	
	public VDBMetaData getVDB() {
		if (this.children == null || this.children.isEmpty()) {
			return vdb;
		}
		if (this.mergedVDB == null) {
			this.mergedVDB = buildVDB();
			update(mergedVDB);
		}
		return this.mergedVDB;
	}
	
	
	private VDBMetaData buildVDB() {
		VDBMetaData mergedVDB = new VDBMetaData();
		mergedVDB.setName(this.vdb.getName());
		mergedVDB.setVersion(this.vdb.getVersion());
		mergedVDB.setModels(this.vdb.getModels());
		mergedVDB.setDataPolicies(this.vdb.getDataPolicies());
		mergedVDB.setDescription(this.vdb.getDescription());
		mergedVDB.setStatus(this.vdb.getStatus());
		mergedVDB.setJAXBProperties(this.vdb.getJAXBProperties());
		mergedVDB.setConnectionType(this.vdb.getConnectionType());
		ConnectorManagerRepository mergedRepo = new ConnectorManagerRepository();
		mergedRepo.getConnectorManagers().putAll(this.cmr.getConnectorManagers());
		for (CompositeVDB child:this.children.values()) {
			
			// add models
			for (Model m:child.getVDB().getModels()) {
				mergedVDB.addModel((ModelMetaData)m);
			}
			
			for (DataPolicy p:child.getVDB().getDataPolicies()) {
				mergedVDB.addDataPolicy((DataPolicyMetadata)p);
			}
			mergedRepo.getConnectorManagers().putAll(child.cmr.getConnectorManagers());
		}
		mergedVDB.addAttchment(ConnectorManagerRepository.class, mergedRepo);
		return mergedVDB;
	}
	
	private UDFMetaData getUDF() {
		if (this.children == null || this.children.isEmpty()) {
			return this.udf;
		}
		
		UDFMetaData mergedUDF = new UDFMetaData();
		if (this.udf != null) {
			mergedUDF.addFunctions(this.udf.getFunctions());
		}
		for (CompositeVDB child:this.children.values()) {
			UDFMetaData funcs = child.getUDF();
			if (funcs != null) {
				mergedUDF.addFunctions(funcs.getFunctions());
			}
		}		
		return mergedUDF;
	}
	
	private LinkedHashMap<String, Resource> getVisibilityMap() {
		if (this.children == null || this.children.isEmpty()) {
			return this.visibilityMap;
		}
		
		LinkedHashMap<String, Resource> mergedvisibilityMap = new LinkedHashMap<String, Resource>();
		if (this.visibilityMap != null) {
			mergedvisibilityMap.putAll(this.visibilityMap);
		}
		for (CompositeVDB child:this.children.values()) {
			LinkedHashMap<String, Resource> vm = child.getVisibilityMap();
			if ( vm != null) {
				mergedvisibilityMap.putAll(vm);
			}
		}		
		return mergedvisibilityMap;
	}
	
	private MetadataStoreGroup getMetadataStores() {
		if (this.children == null || this.children.isEmpty()) {
			return this.stores;
		}		
		
		MetadataStoreGroup mergedStores = new MetadataStoreGroup();
		if (this.stores != null) {
			mergedStores.addStores(this.stores.getStores());
		}
		for (CompositeVDB child:this.children.values()) {
			MetadataStoreGroup stores = child.getMetadataStores();
			if ( stores != null) {
				mergedStores.addStores(stores.getStores());
			}
		}		
		return mergedStores;
	}
}
