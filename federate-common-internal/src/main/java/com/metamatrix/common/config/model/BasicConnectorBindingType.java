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

package com.metamatrix.common.config.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

import com.metamatrix.common.CommonPlugin;
import com.metamatrix.common.config.api.ComponentTypeID;
import com.metamatrix.common.config.api.ConnectorBindingType;
import com.metamatrix.common.util.ErrorMessageKeys;
import com.metamatrix.core.util.Assertion;
import com.metamatrix.core.util.MetaMatrixProductVersion;

public class BasicConnectorBindingType extends BasicComponentType implements ConnectorBindingType {
    
    public static final long serialVersionUID = 1592753260156781311L;
    
    private static final String XA_CONNECTOR = "XA Connector";//$NON-NLS-1$

    public BasicConnectorBindingType(ComponentTypeID id, ComponentTypeID parentID, ComponentTypeID superID, boolean deployable, boolean deprecated, boolean monitored) {
        super(id, parentID, superID, deployable, deprecated, monitored);
        
        if(parentID == null){
            Assertion.isNotNull(parentID, CommonPlugin.Util.getString(ErrorMessageKeys.CONFIG_ERR_0075, id));
        }
        if(superID == null){
            Assertion.isNotNull(superID, CommonPlugin.Util.getString(ErrorMessageKeys.CONFIG_ERR_0076, id));
        }
    }

    BasicConnectorBindingType(BasicConnectorBindingType copy) {
        super(copy);
    }
    
    public boolean isOfConnectorProductType() {
        if (getParentComponentTypeID().getFullName().equalsIgnoreCase(MetaMatrixProductVersion.CONNECTOR_PRODUCT_TYPE_NAME)) {
            return true;
        }
        return false;
    }    

    public boolean isOfTypeConnector() {
        return true;
    }
    
    public boolean isOfTypeXAConnector() {
        if (this.getFullName().endsWith(XA_CONNECTOR)) {
            return true;
        }

        return false;
    }    


    public synchronized Object clone() {
        BasicComponentType result = null;
	    result = new BasicConnectorBindingType(this);

        Collection defns = this.getComponentTypeDefinitions();
        result.setComponentTypeDefinitions(defns);

        return result;
    }

    /**
     * Get list of extension modules needed by this connector type. 
     * @see com.metamatrix.common.config.api.ConnectorBindingType#getExtensionModules()
     * @since 4.3.2
     */
    public String[] getExtensionModules() {
        ArrayList modules = new ArrayList();
        String classPath = getDefaultValue(Attributes.CONNECTOR_CLASSPATH);
        if (classPath != null) {
            StringTokenizer st = new StringTokenizer(classPath, ";"); //$NON-NLS-1$
            while (st.hasMoreTokens()) {
                String path = st.nextToken();
                int idx = path.indexOf(Attributes.MM_JAR_PROTOCOL);
                if (idx != -1) {
                    String jarFile = path.substring(idx + Attributes.MM_JAR_PROTOCOL.length() + 1);
                    modules.add(jarFile);
                }                                        
            }
        }
        return (String[])modules.toArray(new String[modules.size()]);
    }    

}
