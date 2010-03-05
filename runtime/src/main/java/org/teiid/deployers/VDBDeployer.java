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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.helpers.AbstractSimpleRealDeployer;
import org.jboss.deployers.spi.deployer.managed.ManagedObjectCreator;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.managed.api.ManagedObject;
import org.jboss.managed.api.factory.ManagedObjectFactory;
import org.jboss.virtual.VirtualFile;
import org.teiid.adminapi.VDB;
import org.teiid.adminapi.impl.ModelMetaData;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.connector.metadata.runtime.MetadataStore;
import org.teiid.dqp.internal.cache.DQPContextCache;
import org.teiid.metadata.CompositeMetadataStore;
import org.teiid.metadata.TransformationMetadata;
import org.teiid.metadata.index.IndexMetadataFactory;

import com.metamatrix.core.CoreConstants;
import com.metamatrix.query.function.metadata.FunctionMethod;
import com.metamatrix.query.metadata.QueryMetadataInterface;

public class VDBDeployer extends AbstractSimpleRealDeployer<VDBMetaData> implements ManagedObjectCreator {
	protected Logger log = Logger.getLogger(getClass());
	private ManagedObjectFactory mof;
	private VDBRepository vdbRepository;
	private DQPContextCache contextCache;
	private ObjectSerializer serializer;
	
	public VDBDeployer() {
		super(VDBMetaData.class);
		setInput(VDBMetaData.class);
		setOutput(VDBMetaData.class);
	}

	@Override
	public void deploy(DeploymentUnit unit, VDBMetaData deployment) throws DeploymentException {
		if (this.vdbRepository.getVDB(deployment.getName(), deployment.getVersion()) != null) {
			this.vdbRepository.removeVDB(deployment.getName(), deployment.getVersion());
			log.info("Re-deploying VDB = "+deployment);
		}
		
		List<String> errors = deployment.getValidityErrors();
		if (errors != null && !errors.isEmpty()) {
			throw new DeploymentException("VDB has validaity errors; failed to deploy");
		}
		
		this.vdbRepository.addVDB(deployment);
		
		TransformationMetadata metadata = null;
		
		// get the metadata store of the VDB (this is build in parse stage)
		CompositeMetadataStore store = unit.getAttachment(CompositeMetadataStore.class);
		this.vdbRepository.addMetadataStore(deployment, store);
		
		// check if this is a VDB with index files, if there are then build the TransformationMetadata
		IndexMetadataFactory indexFactory = unit.getAttachment(IndexMetadataFactory.class);
		UDFMetaData udf = unit.getAttachment(UDFMetaData.class);
		if (indexFactory != null) {
			Map<VirtualFile, Boolean> visibilityMap = indexFactory.getEntriesPlusVisibilities();
			metadata = buildTransformationMetaData(deployment, visibilityMap, store, udf);
		}
		else {
			// this dynamic VDB
			metadata = buildTransformationMetaData(deployment, null, store, udf);
		}
				
		// add the metadata objects as attachments
		deployment.removeAttachment(IndexMetadataFactory.class);
		deployment.removeAttachment(UDFMetaData.class);
		deployment.addAttchment(QueryMetadataInterface.class, metadata);
		deployment.addAttchment(CompositeMetadataStore.class, metadata.getMetadataStore());
		
		// add transformation metadata to the repository.
		this.vdbRepository.addMetadata(deployment, metadata);
				
		boolean valid = true;
		for(ModelMetaData model:deployment.getModels()) {
			for (String sourceName:model.getSourceNames()) {
				String jndiName = model.getSourceJndiName(sourceName);
				try {
					InitialContext ic = new InitialContext();
					ic.lookup(jndiName);
				} catch (NamingException e) {
					valid = false;
					String msg = "Jndi resource = "+ jndiName + " not found for Source Name = "+sourceName;
					model.addError(ModelMetaData.ValidationError.Severity.ERROR.name(), msg);
					log.info(msg);
				}
			}
		}
		
		// Check if the VDB is fully configured.
		if (valid) {
			deployment.setStatus(VDB.Status.ACTIVE);
		}
		log.info("VDB = "+deployment + " deployed");
	}


	// does this need to be synchronized? 
	private TransformationMetadata buildTransformationMetaData(VDBMetaData vdb, Map<VirtualFile, Boolean> visibilityMap, CompositeMetadataStore store, UDFMetaData udf) throws DeploymentException {
		
		// get the system VDB metadata store
		MetadataStore systemStore = this.vdbRepository.getMetadataStore(CoreConstants.SYSTEM_VDB, 1);
		if (systemStore == null) {
			throw new DeploymentException("System.vdb needs to be loaded before any other VDBs.");
		}
		
		store.addMetadataStore(systemStore);
		
		Collection <FunctionMethod> methods = null;
		if (udf != null) {
			methods = udf.getMethods();
		}
		
		TransformationMetadata metadata =  new TransformationMetadata(vdb, store, visibilityMap, methods);
				
		return metadata;
	}	
	
	

	@Override
	public void build(DeploymentUnit unit, Set<String> attachmentNames, Map<String, ManagedObject> managedObjects)
		throws DeploymentException {
	          
		ManagedObject vdbMO = managedObjects.get(VDBMetaData.class.getName());
		if (vdbMO != null) {
			VDBMetaData vdb = (VDBMetaData) vdbMO.getAttachment();
			for (ModelMetaData m : vdb.getModels()) {
				if (m.getName().equals(CoreConstants.SYSTEM_MODEL)) {
					continue;
				}
				ManagedObject mo = this.mof.initManagedObject(m, ModelMetaData.class, m.getName(),m.getName());
				if (mo == null) {
					throw new DeploymentException("could not create managed object");
				}
				managedObjects.put(mo.getName(), mo);
			}
		}
	}
	
	public void setManagedObjectFactory(ManagedObjectFactory mof) {
		this.mof = mof;
	}
	
	public void setVDBRepository(VDBRepository repo) {
		this.vdbRepository = repo;
	}
	
	@Override
	public void undeploy(DeploymentUnit unit, VDBMetaData deployment) {
		super.undeploy(unit, deployment);
		
		if (this.vdbRepository != null) {
			this.vdbRepository.removeVDB(deployment.getName(), deployment.getVersion());
		}
		
		if (this.contextCache != null) {
			// remove any vdb specific context cache
			this.contextCache.removeVDBScopedCache(deployment.getName(), deployment.getVersion());			
		}

		try {
			if (((VFSDeploymentUnit)unit).getRoot().exists()) {
				File cacheFileName = this.serializer.getAttachmentPath((VFSDeploymentUnit)unit, deployment.getName()+"_"+deployment.getVersion());
				if (cacheFileName.exists()) {
					cacheFileName.delete();
				}
			}
		} catch (IOException e) {
			log.warn("failed to delete the cached metadata files due to:" + e.getMessage());
		}

		log.info("VDB = "+deployment + " undeployed");
	}

	public void setContextCache(DQPContextCache cache) {
		this.contextCache = cache;
	}
	
	public void setObjectSerializer(ObjectSerializer serializer) {
		this.serializer = serializer;
	}		
	
}
