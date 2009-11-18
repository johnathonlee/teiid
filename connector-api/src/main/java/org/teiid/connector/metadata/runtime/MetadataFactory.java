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

package org.teiid.connector.metadata.runtime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.teiid.connector.DataPlugin;
import org.teiid.connector.api.ConnectorException;
import org.teiid.connector.api.TypeFacility;

import com.metamatrix.core.id.UUIDFactory;

/**
 * Allows connectors to build metadata for use by the engine.
 * 
 * TODO: add support for datatype import
 * TODO: add support for unique constraints
 */
public class MetadataFactory {
	
	private Schema model;
	private UUIDFactory factory = new UUIDFactory();
	private Map<String, Datatype> dataTypes;
	private Properties importProperties;
	private MetadataStore store = new MetadataStore();
	private Set<String> uniqueNames = new HashSet<String>();
	
	public MetadataFactory(String modelName, Map<String, Datatype> dataTypes, Properties importProperties) {
		this.dataTypes = dataTypes;
		this.importProperties = importProperties;
		model = new Schema();
		model.setName(modelName);
		model.setPhysical(true);
		model.setPrimaryMetamodelUri("http://www.metamatrix.com/metamodels/Relational"); //$NON-NLS-1$
		setUUID(model);	
		store.addSchema(model);
	}
	
	public MetadataStore getMetadataStore() {
		return store;
	}
	
	public Properties getImportProperties() {
		return importProperties;
	}
	
	private void setUUID(AbstractMetadataRecord record) {
		record.setUUID(factory.create().toString());
	}

	private void setValuesUsingParent(String name,
			AbstractMetadataRecord parent, AbstractMetadataRecord child, String recordType) throws ConnectorException {
		child.setFullName(parent.getFullName() + AbstractMetadataRecord.NAME_DELIM_CHAR + name);
		child.setName(name);
		if (!uniqueNames.add(recordType + "/" + child.getFullName())) { //$NON-NLS-1$
			throw new ConnectorException(DataPlugin.Util.getString("MetadataFactory.duplicate_name", child)); //$NON-NLS-1$
		}
	}
	
	/**
	 * Add a table with the given name to the model.  
	 * @param name
	 * @return
	 * @throws ConnectorException 
	 */
	public Table addTable(String name) throws ConnectorException {
		Table table = new Table();
		table.setTableType(Table.Type.Table);
		setValuesUsingParent(name, model, table, "table"); //$NON-NLS-1$
		table.setColumns(new LinkedList<Column>());
		table.setAccessPatterns(new LinkedList<KeyRecord>());
		table.setIndexes(new LinkedList<KeyRecord>());
		table.setForiegnKeys(new LinkedList<ForeignKey>());
		table.setUniqueKeys(new LinkedList<KeyRecord>());
		setUUID(table);
		this.model.addTable(table);
		return table;
	}
	
	/**
	 * Adds a column to the table with the given name and type.
	 * @param name
	 * @param type should be one of {@link TypeFacility.RUNTIME_NAMES}
	 * @param table
	 * @return
	 * @throws ConnectorException
	 */
	public Column addColumn(String name, String type, ColumnSet table) throws ConnectorException {
		Column column = new Column();
		setValuesUsingParent(name, table, column, "column"); //$NON-NLS-1$
		table.getColumns().add(column);
		column.setPosition(table.getColumns().size()); //1 based indexing
		Datatype datatype = setColumnType(type, column);
		column.setCaseSensitive(datatype.isCaseSensitive());
		column.setAutoIncrementable(datatype.isAutoIncrement());
		column.setSigned(datatype.isSigned());		
		setUUID(column);
		return column;
	}

	private Datatype setColumnType(String type,
			BaseColumn column) throws ConnectorException {
		Datatype datatype = dataTypes.get(type);
		if (datatype == null) {
			throw new ConnectorException(DataPlugin.Util.getString("MetadataFactory.unknown_datatype", type)); //$NON-NLS-1$
		}
		column.setDatatype(datatype);
		column.setDatatypeUUID(datatype.getUUID());
		column.setLength(datatype.getLength());
		column.setPrecision(datatype.getPrecisionLength());
		column.setRadix(datatype.getRadix());
		column.setRuntimeType(datatype.getRuntimeTypeName());
		return datatype;
	}
	
	/**
	 * Adds a primary key to the given table.  The column names should be in key order.
	 * @param name
	 * @param columnNames
	 * @param table
	 * @return
	 * @throws ConnectorException
	 */
	public ColumnSet addPrimaryKey(String name, List<String> columnNames, Table table) throws ConnectorException {
		KeyRecord primaryKey = new KeyRecord(KeyRecord.Type.Primary);
		primaryKey.setTable(table);
		primaryKey.setColumns(new ArrayList<Column>(columnNames.size()));
		setValuesUsingParent(name, table, primaryKey, "pk"); //$NON-NLS-1$
		setUUID(primaryKey);
		assignColumns(columnNames, table, primaryKey);
		table.setPrimaryKey(primaryKey);
		return primaryKey;
	}
	
	/**
	 * Adds an access pattern to the given table.
	 * @param name
	 * @param columnNames
	 * @param table
	 * @return
	 * @throws ConnectorException
	 */
	public KeyRecord addAccessPattern(String name, List<String> columnNames, Table table) throws ConnectorException {
		KeyRecord ap = new KeyRecord(KeyRecord.Type.AccessPattern);
		ap.setTable(table);
		ap.setColumns(new ArrayList<Column>(columnNames.size()));
		setValuesUsingParent(name, table, ap, "index"); //$NON-NLS-1$
		setUUID(ap);
		assignColumns(columnNames, table, ap);
		table.getAccessPatterns().add(ap);
		return ap;
	}	
	
	/**
	 * Adds an index to the given table.
	 * @param name
	 * @param nonUnique true indicates that an index is being added.
	 * @param columnNames
	 * @param table
	 * @return
	 * @throws ConnectorException
	 */
	public KeyRecord addIndex(String name, boolean nonUnique, List<String> columnNames, Table table) throws ConnectorException {
		KeyRecord index = new KeyRecord(nonUnique?KeyRecord.Type.NonUnique:KeyRecord.Type.Index);
		index.setTable(table);
		index.setColumns(new ArrayList<Column>(columnNames.size()));
		setValuesUsingParent(name, table, index, "index"); //$NON-NLS-1$
		setUUID(index);
		assignColumns(columnNames, table, index);
		table.getIndexes().add(index);
		return index;
	}
		
	/**
	 * Adds a foreign key to the given table.  The column names should be in key order.
	 * @param name
	 * @param columnNames
	 * @param pkTable
	 * @param table
	 * @return
	 * @throws ConnectorException
	 */
	public ForeignKey addForiegnKey(String name, List<String> columnNames, Table pkTable, Table table) throws ConnectorException {
		ForeignKey foreignKey = new ForeignKey();
		foreignKey.setTable(table);
		foreignKey.setColumns(new ArrayList<Column>(columnNames.size()));
		setValuesUsingParent(name, table, foreignKey, "fk"); //$NON-NLS-1$
		setUUID(foreignKey);
		if (pkTable.getPrimaryKey() == null) {
			throw new ConnectorException("No primary key defined for table " + pkTable); //$NON-NLS-1$
		}
		foreignKey.setPrimaryKey(pkTable.getPrimaryKey());
		foreignKey.setUniqueKeyID(pkTable.getPrimaryKey().getUUID());
		assignColumns(columnNames, table, foreignKey);
		table.getForeignKeys().add(foreignKey);
		return foreignKey;
	}
	
	/**
	 * Add a procedure with the given name to the model.  
	 * @param name
	 * @return
	 * @throws ConnectorException 
	 */
	public ProcedureRecordImpl addProcedure(String name) throws ConnectorException {
		ProcedureRecordImpl procedure = new ProcedureRecordImpl();
		setValuesUsingParent(name, this.model, procedure, "proc"); //$NON-NLS-1$
		setUUID(procedure);
		procedure.setParameters(new LinkedList<ProcedureParameter>());
		this.model.addProcedure(procedure);
		return procedure;
	}
	
	/**
	 * Add a procedure parameter.
	 * @param name
	 * @param type should be one of {@link TypeFacility.RUNTIME_NAMES}
	 * @param parameterType should be one of {@link ProcedureParameter.Type}
	 * @param procedure
	 * @return
	 * @throws ConnectorException 
	 */
	public ProcedureParameter addProcedureParameter(String name, String type, ProcedureParameter.Type parameterType, ProcedureRecordImpl procedure) throws ConnectorException {
		ProcedureParameter param = new ProcedureParameter();
		setValuesUsingParent(name, procedure, param, "param"); //$NON-NLS-1$
		setUUID(param);
		param.setType(parameterType);
		setColumnType(type, param);
		procedure.getParameters().add(param);
		param.setPosition(procedure.getParameters().size()); //1 based indexing
		return param;
	}
	
	/**
	 * Add a procedure resultset column to the given procedure.
	 * @param name
	 * @param type should be one of {@link TypeFacility.RUNTIME_NAMES}
	 * @param procedure
	 * @return
	 * @throws ConnectorException 
	 */
	public Column addProcedureResultSetColumn(String name, String type, ProcedureRecordImpl procedure) throws ConnectorException {
		if (procedure.getResultSet() == null) {
			ColumnSet resultSet = new ColumnSet();
			setValuesUsingParent("RESULT_SET", procedure, resultSet, "rs"); //$NON-NLS-1$ //$NON-NLS-2$
			setUUID(resultSet);
			procedure.setResultSet(resultSet);
			procedure.setResultSetID(resultSet.getUUID());
		}
		return addColumn(name, type, procedure.getResultSet());
	}

	private void assignColumns(List<String> columnNames, Table table,
			ColumnSet columns) throws ConnectorException {
		for (String columnName : columnNames) {
			boolean match = false;
			for (Column column : table.getColumns()) {
				if (column.getName().equals(columnName)) {
					match = true;
					columns.getColumns().add(column);
					break;
				}
			}
			if (!match) {
				throw new ConnectorException(DataPlugin.Util.getString("MetadataFactory.no_column_found", columnName)); //$NON-NLS-1$
			}
		}
	}
		
}
