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
package org.teiid.adminapi.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.jboss.managed.api.annotation.ManagementComponent;
import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementObjectID;
import org.jboss.managed.api.annotation.ManagementProperties;
import org.jboss.managed.api.annotation.ManagementProperty;
import org.teiid.adminapi.DataPolicy;
import org.teiid.adminapi.Model;
import org.teiid.adminapi.VDB;
import org.teiid.adminapi.impl.ModelMetaData.ValidationError;


@ManagementObject(componentType=@ManagementComponent(type="teiid",subtype="vdb"), properties=ManagementProperties.EXPLICIT)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "", propOrder = {
    "description",
    "JAXBProperties",
    "models",
    "dataPolicies"
})
@XmlRootElement(name = "vdb")
public class VDBMetaData extends AdminObjectImpl implements VDB {

	private static final long serialVersionUID = -4723595252013356436L;
	
	/**
	 * This simulating a list over a map. JAXB requires a list and performance recommends
	 * map and we would like to keep one variable to represent both. 
	 */
	@XmlElement(name = "model", required = true, type = ModelMetaData.class)
	protected ListOverMap<ModelMetaData> models = new ListOverMap<ModelMetaData>(new KeyBuilder<ModelMetaData>() {
		@Override
		public String getKey(ModelMetaData entry) {
			return entry.getName();
		}
	});
	
	@XmlElement(name = "data-policy", required = true, type = DataPolicyMetadata.class)
	protected ListOverMap<DataPolicyMetadata> dataPolicies = new ListOverMap<DataPolicyMetadata>(new KeyBuilder<DataPolicyMetadata>() {
		@Override
		public String getKey(DataPolicyMetadata entry) {
			return entry.getName();
		}
	});	
	
	@XmlAttribute(name = "version", required = true)
	private int version = 1;
	
	@XmlElement(name = "description")
	protected String description;
	
	private String fileUrl = null;
	private boolean dynamic = false;
	private VDB.Status status = VDB.Status.INACTIVE;
	

	@ManagementProperty(description="Name of the VDB")
	@ManagementObjectID(type="vdb")
	@XmlAttribute(name = "name", required = true)
	public String getName() {
		return super.getName();
	}
	
	// This needed by JAXB marshaling
	public void setName(String name) {
		super.setName(name);
	} 
	
	@Override
	@ManagementProperty(description="VDB Status")
	public Status getStatus() {
		return this.status;
	}
	
	public void setStatus(Status s) {
		this.status = s;
	}
	
	@Override
	@ManagementProperty(description="VDB version")
	public int getVersion() {
		return this.version;
	}
	
	public void setVersion(int version) {
		this.version = version;
	}	
		
	@Override
	@ManagementProperty(description = "The VDB file url")
	public String getUrl() {
		return this.fileUrl;
	}
	
	public void setUrl(String url) {
		this.fileUrl = url;
	}

	@Override
	@ManagementProperty(description="Models in a VDB", managed=true)
	public List<Model> getModels(){
		return new ArrayList<Model>(this.models.getMap().values());
	}
	
	/**
	 * This method required to make the JNDI assignment on the model work; if not persistent Management framework
	 * treating "models" as ReadOnly property. The actual assignment is done in the VDBMetaDataClassInstancefactory
	 * @param models
	 */
	public void setModels(List<Model> models) {
		for (Model obj : models) {
			ModelMetaData model = (ModelMetaData) obj;
			addModel(model);
		}
	}
	
	public void addModel(ModelMetaData m) {
		this.models.getMap().put(m.getName(), m);
	}	
	
	@Override
	@ManagementProperty(description = "Description")	
	public String getDescription() {
		return this.description;
	}
	
	public void setDescription(String desc) {
		this.description = desc;
	}

	@Override
	@ManagementProperty(description = "VDB validity errors", readOnly=true)		
	public List<String> getValidityErrors(){
		List<String> allErrors = new ArrayList<String>();
		for (ModelMetaData model:this.models.getMap().values()) {
			List<ValidationError> errors = model.getErrors();
			if (errors != null && !errors.isEmpty()) {
				for (ValidationError m:errors) {
					if (ValidationError.Severity.valueOf(m.getSeverity()).equals(ValidationError.Severity.ERROR)) {
						allErrors.add(m.getValue());
					}
				}
			}
		}
		return allErrors; 
	}

	@Override
	@ManagementProperty(description = "Is VDB Valid", readOnly=true)
    public boolean isValid() {
        if (!getValidityErrors().isEmpty()) {
            return false;
        }
                
        if (getModels().isEmpty()) {
            return false;        	
        }
    	for(ModelMetaData m: this.models.getMap().values()) {
    		if (m.isSource()) {
    			List<String> resourceNames = m.getSourceNames();
    			if (resourceNames.isEmpty()) {
    				return false;
    			}
    			for (String sourceName:resourceNames) {
    				if (m.getSourceJndiName(sourceName) == null) {
    					return false;
    				}
    			}
    		}
    	}
        return true;
    } 	
    
	public String toString() {
		return getName()+"."+getVersion()+ models.getMap().values(); //$NON-NLS-1$
	}

	public ModelMetaData getModel(String modelName) {
		return this.models.getMap().get(modelName);
	}
	
	public Set<String> getMultiSourceModelNames(){
		Set<String> list = new HashSet<String>();
		for(ModelMetaData m: models.getMap().values()) {
			if (m.isSupportsMultiSourceBindings()) {
				list.add(m.getName());
			}
		}
		return list;
	}
	
	// This one manages the JAXB binding
	@Override
	@XmlElement(name = "property", type = PropertyMetadata.class)
	@ManagementProperty(description = "VDB Properties", managed=true)
	public List<PropertyMetadata> getJAXBProperties(){
		return super.getJAXBProperties();
	}
	
	@ManagementProperty(description="Is this a Dynamic VDB")
	public boolean isDynamic() {
		return dynamic;
	}

	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}	
	
	@Override
	@ManagementProperty(description="Data Policies in a VDB", managed=true)
	public List<DataPolicy> getDataPolicies(){
		return new ArrayList<DataPolicy>(this.dataPolicies.getMap().values());
	}	
	
	/**
	 * This method is required by the Management framework to write the mappings to the persistent form. The actual assignment is done
	 * in the VDBMetaDataClassInstancefactory
	 * @param policies
	 */
	public void setDataPolicies(List<DataPolicy> policies){
		this.dataPolicies.getMap().clear();
		for (DataPolicy policy:policies) {
			this.dataPolicies.getMap().put(policy.getName(), (DataPolicyMetadata)policy);
		}
	}	
	
	public void addDataPolicy(DataPolicyMetadata policy){
		this.dataPolicies.getMap().put(policy.getName(), policy);
	}
	
	public DataPolicyMetadata getDataPolicy(String policyName) {
		return this.dataPolicies.getMap().get(policyName);
	}
	
	public boolean isPreview() {
		return Boolean.valueOf(getPropertyValue("preview")); //$NON-NLS-1$
	}
}
