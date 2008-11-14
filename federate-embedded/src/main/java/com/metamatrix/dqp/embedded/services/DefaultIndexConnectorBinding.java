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

import java.util.Date;
import java.util.Properties;

import com.metamatrix.common.config.api.ComponentTypeID;
import com.metamatrix.common.config.api.ConfigurationID;
import com.metamatrix.common.config.api.ConnectorBinding;
import com.metamatrix.common.config.model.ConfigurationVisitor;
import com.metamatrix.common.namedobject.BaseID;
import com.metamatrix.dqp.internal.datamgr.ConnectorPropertyNames;


/** 
 * This is default class which will used by the VDBs as ConnectorBinding for the 
 * CoreConstants.SYSTEM_PHYSICAL_MODEL_NAME. This is same connector in all the
 * VDBs, however this is not included in the VDB.
 * 
 * @since 4.3
 */
class DefaultIndexConnectorBinding implements ConnectorBinding {
        
    private static final String INDEX_CONNECTOR_NAME = "Index_Connector"; //$NON-NLS-1$
    private static final String INDEX_CONNECTOR_CLASS_NAME = "com.metamatrix.connector.metadata.IndexConnector"; //$NON-NLS-1$
    private static final String INDEX_CONNECTOR_MAX_ROWS = "0"; //$NON-NLS-1$
    
    private Properties props = new Properties();
    private Date creationTime = new Date(System.currentTimeMillis());
    
    /**
     * ctor  
     * @since 4.3
     */
    public DefaultIndexConnectorBinding() {
        this.props.setProperty(ConnectorPropertyNames.CONNECTOR_CLASS, INDEX_CONNECTOR_CLASS_NAME );
        this.props.setProperty(ConnectorPropertyNames.MAX_RESULT_ROWS, INDEX_CONNECTOR_MAX_ROWS ); 
    }
    
    /** 
     * @see com.metamatrix.common.config.api.ConnectorBinding#getDeployedName()
     */
    public String getDeployedName() {
        return INDEX_CONNECTOR_NAME;
    }
    
    /** 
     * @see com.metamatrix.common.config.api.ServiceComponentDefn#isQueuedService()
     * @since 4.3
     */
    public boolean isQueuedService() {
        return false;
    }

    /** 
     * @see com.metamatrix.common.config.api.ServiceComponentDefn#accept(com.metamatrix.common.config.model.ConfigurationVisitor)
     * @since 4.3
     */
    public void accept(ConfigurationVisitor visitor) {         
        // no configuratio needed
    }

    /** 
     * @see com.metamatrix.common.config.api.ServiceComponentDefn#getRoutingUUID()
     * @since 4.3
     */
    public String getRoutingUUID() {
        return INDEX_CONNECTOR_NAME;
    }

    /** 
     * @see com.metamatrix.common.config.api.ComponentDefn#getConfigurationID()
     * @since 4.3
     */
    public ConfigurationID getConfigurationID() {
        return null;
    }

    /** 
     * @see com.metamatrix.common.config.api.ComponentDefn#isEnabled()
     * @since 4.3
     */
    public boolean isEnabled() {
        return true;
    }

    /** 
     * @see com.metamatrix.common.config.api.ComponentObject#getName()
     * @since 4.3
     */
    public String getName() {
        return INDEX_CONNECTOR_NAME;
    }

    /** 
     * @see com.metamatrix.common.config.api.ComponentObject#getProperties()
     * @since 4.3
     */
    public Properties getProperties() {
        return props;
    }

    /** 
     * @see com.metamatrix.common.config.api.ComponentObject#getProperty(java.lang.String)
     * @since 4.3
     */
    public String getProperty(String name) {
        return props.getProperty(name);
    }

    /** 
     * @see com.metamatrix.common.config.api.ComponentObject#getComponentTypeID()
     * @since 4.3
     */
    public ComponentTypeID getComponentTypeID() {
        return new ComponentTypeID(INDEX_CONNECTOR_NAME);
    }

    /** 
     * @see com.metamatrix.common.config.api.ComponentObject#getDescription()
     * @since 4.3
     */
    public String getDescription() {
        return INDEX_CONNECTOR_NAME;
    }

    /** 
     * @see com.metamatrix.common.config.api.ComponentObject#getCreatedBy()
     * @since 4.3
     */
    public String getCreatedBy() {
        return "system.runtime"; //$NON-NLS-1$
    }

    /** 
     * @see com.metamatrix.common.config.api.ComponentObject#getCreatedDate()
     * @since 4.3
     */
    public Date getCreatedDate() {
        return creationTime;
    }

    /** 
     * @see com.metamatrix.common.config.api.ComponentObject#getLastChangedBy()
     * @since 4.3
     */
    public String getLastChangedBy() {
        return "none"; //$NON-NLS-1$
    }

    /** 
     * @see com.metamatrix.common.config.api.ComponentObject#getLastChangedDate()
     * @since 4.3
     */
    public Date getLastChangedDate() {
        return creationTime;
    }

    /** 
     * @see com.metamatrix.common.config.api.ComponentObject#isDependentUpon(com.metamatrix.common.namedobject.BaseID)
     * @since 4.3
     */
    public boolean isDependentUpon(BaseID componentObjectId) {
        return false;
    }

    /** 
     * @see com.metamatrix.common.namedobject.BaseObject#getID()
     * @since 4.3
     */
    public BaseID getID() {
        return null;
    }

    /** 
     * @see com.metamatrix.common.namedobject.BaseObject#getFullName()
     * @since 4.3
     */
    public String getFullName() {
        return INDEX_CONNECTOR_NAME;
    }

    /** 
     * @see com.metamatrix.common.namedobject.BaseObject#compareTo(java.lang.Object)
     * @since 4.3
     */
    public int compareTo(Object obj) {
        return 0;
    }
    /**
     * Return a deep cloned instance of this object.  Subclasses must override
     * this method.
     * @return the object that is the clone of this instance.
     * @throws CloneNotSupportedException if this object cannot be cloned (i.e., only objects in
     * {@link com.metamatrix.metadata.api.Defaults Defaults} cannot be cloned).
     */
    public Object clone() throws CloneNotSupportedException{
        throw new CloneNotSupportedException();
    }

    /** 
     * @see com.metamatrix.common.config.api.ConnectorBinding#getConnectorClass()
     * @since 4.3
     */
    public String getConnectorClass() {
        return INDEX_CONNECTOR_CLASS_NAME;
    }

    /** 
     * @see com.metamatrix.common.config.api.ComponentDefn#isEssential()
     */
    public boolean isEssential() {
        return false;
    }
}
