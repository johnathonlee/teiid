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

import org.teiid.adminapi.Model;
import org.teiid.adminapi.VDB;
import org.teiid.adminapi.impl.ModelMetaData;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.dqp.internal.datamgr.impl.ConnectorManager;
import org.teiid.dqp.internal.datamgr.impl.ConnectorManagerRepository;
import org.teiid.logging.LogConstants;
import org.teiid.logging.LogManager;
import org.teiid.runtime.RuntimePlugin;


public class VDBStatusChecker {
	private VDBRepository vdbRepository;
	private ConnectorManagerRepository connectorManagerRepository;
	
	public void translatorAdded(String translatorName) {
		resourceAdded(translatorName, true);
	}
	
	public void translatorRemoved(String translatorName) {
		resourceremoved(translatorName, true);
	}
	
	public void dataSourceAdded(String dataSourceName) {
		resourceAdded(dataSourceName, false);
	}
	
	public void dataSourceRemoved(String dataSourceName) {
		resourceremoved(dataSourceName, false);
	}	
	
	public void setVDBRepository(VDBRepository repo) {
		this.vdbRepository = repo;
	}	
	
	public void setConnectorManagerRepository(ConnectorManagerRepository repo) {
		this.connectorManagerRepository = repo;
	}	
	
	public void resourceAdded(String resourceName, boolean translator) {
		for (VDBMetaData vdb:this.vdbRepository.getVDBs()) {
			if (vdb.getStatus() == VDB.Status.ACTIVE || vdb.isPreview()) {
				continue;
			}
			
			for (Model m:vdb.getModels()) {
				ModelMetaData model = (ModelMetaData)m;
				if (model.getErrors().isEmpty()) {
					continue;
				}

				String sourceName = getSourceName(resourceName, model, translator);
				if (sourceName != null) {
					ConnectorManager cm = this.connectorManagerRepository.getConnectorManager(sourceName);
					model.clearErrors();
					String status = cm.getStausMessage();
					if (status != null && status.length() > 0) {
						model.addError(ModelMetaData.ValidationError.Severity.ERROR.name(), status);
						LogManager.logInfo(LogConstants.CTX_RUNTIME, status);					
					}					
				}
			}

			boolean valid = true;
			for (Model m:vdb.getModels()) {
				ModelMetaData model = (ModelMetaData)m;
				if (!model.getErrors().isEmpty()) {
					valid = false;
					break;
				}
			}
			
			if (valid) {
				vdb.setStatus(VDB.Status.ACTIVE);
				LogManager.logInfo(LogConstants.CTX_RUNTIME, RuntimePlugin.Util.getString("vdb_activated",vdb.getName(), vdb.getVersion())); //$NON-NLS-1$
			}
		}
	}
	
	public void resourceremoved(String resourceName, boolean translator) {
		for (VDBMetaData vdb:this.vdbRepository.getVDBs()) {
			if (vdb.isPreview()) {
				continue;
			}
			
			for (Model m:vdb.getModels()) {
				ModelMetaData model = (ModelMetaData)m;
				
				String sourceName = getSourceName(resourceName, model, translator);
				if (sourceName != null) {
					vdb.setStatus(VDB.Status.INACTIVE);
					String msg = null;
					if (translator) {
						msg = RuntimePlugin.Util.getString("translator_not_found", vdb.getName(), vdb.getVersion(), model.getSourceTranslatorName(sourceName)); //$NON-NLS-1$
					}
					else {
						msg = RuntimePlugin.Util.getString("datasource_not_found", vdb.getName(), vdb.getVersion(), model.getSourceTranslatorName(sourceName)); //$NON-NLS-1$
					}
					model.addError(ModelMetaData.ValidationError.Severity.ERROR.name(), msg);
					LogManager.logInfo(LogConstants.CTX_RUNTIME, msg);					
					LogManager.logInfo(LogConstants.CTX_RUNTIME, RuntimePlugin.Util.getString("vdb_inactivated",vdb.getName(), vdb.getVersion())); //$NON-NLS-1$							
				}
			}			
		}
	}

	private String getSourceName(String translatorName, ModelMetaData model, boolean translator) {
		for (String sourceName:model.getSourceNames()) {
			if (translator && translatorName.equals(model.getSourceTranslatorName(sourceName))) {
				return sourceName;
			}
			else  if (translatorName.equals(model.getSourceConnectionJndiName(sourceName))) {
				return sourceName;
			}
		}
		return null;
	}		
}
