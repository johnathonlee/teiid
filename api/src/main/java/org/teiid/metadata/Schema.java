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

package org.teiid.metadata;

import java.util.LinkedHashMap;
import java.util.Map;

import org.teiid.connector.DataPlugin;

public class Schema extends AbstractMetadataRecord {

	private static final long serialVersionUID = -5113742472848113008L;

	private boolean physical = true;
    private boolean isVisible = true;
    private String primaryMetamodelUri = "http://www.metamatrix.com/metamodels/Relational"; //$NON-NLS-1$
    
    private Map<String, Table> tables = new LinkedHashMap<String, Table>();
	private Map<String, Procedure> procedures = new LinkedHashMap<String, Procedure>();
	private Map<String, FunctionMethod> functions = new LinkedHashMap<String, FunctionMethod>();
	
	public void addTable(Table table) {
		table.setParent(this);
		if (this.tables.put(table.getCanonicalName(), table) != null) {
			throw new DuplicateRecordException(DataPlugin.Util.getString("Schema.duplicate_table", table.getName())); //$NON-NLS-1$
		}
	}
	
	public void addProcedure(Procedure procedure) {
		procedure.setParent(this);
		if (this.procedures.put(procedure.getCanonicalName(), procedure) != null) {
			throw new DuplicateRecordException(DataPlugin.Util.getString("Schema.duplicate_procedure", procedure.getName())); //$NON-NLS-1$
		}
	}
	
	public void addFunction(FunctionMethod function) {
		function.setParent(this);
		if (this.functions.put(function.getCanonicalName(), function) != null) {
			throw new DuplicateRecordException(DataPlugin.Util.getString("Schema.duplicate_function", function.getName())); //$NON-NLS-1$
		}
	}	

	/**
	 * Get the tables defined in this schema
	 * @return
	 */
	public Map<String, Table> getTables() {
		return tables;
	}
	
	/**
	 * Get the procedures defined in this schema
	 * @return
	 */
	public Map<String, Procedure> getProcedures() {
		return procedures;
	}
	
	/**
	 * Get the functions defined in this schema
	 * @return
	 */
	public Map<String, FunctionMethod> getFunctions() {
		return functions;
	}
	
    public String getPrimaryMetamodelUri() {
        return primaryMetamodelUri;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public boolean isPhysical() {
        return physical;
    }

    /**
     * @param string
     */
    public void setPrimaryMetamodelUri(String string) {
        primaryMetamodelUri = string;
    }

    /**
     * @param b
     */
    public void setVisible(boolean b) {
        isVisible = b;
    }
    
    public void setPhysical(boolean physical) {
		this.physical = physical;
	}
    
}
