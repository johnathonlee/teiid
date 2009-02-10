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
package com.metamatrix.connector.salesforce.execution;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.axis.message.MessageElement;

import com.metamatrix.connector.api.ConnectorEnvironment;
import com.metamatrix.connector.api.ConnectorLogger;
import com.metamatrix.connector.api.DataNotAvailableException;
import com.metamatrix.connector.api.ExecutionContext;
import com.metamatrix.connector.api.ResultSetExecution;
import com.metamatrix.connector.exception.ConnectorException;
import com.metamatrix.connector.language.IQueryCommand;
import com.metamatrix.connector.metadata.runtime.Element;
import com.metamatrix.connector.metadata.runtime.RuntimeMetadata;
import com.metamatrix.connector.salesforce.Messages;
import com.metamatrix.connector.salesforce.Util;
import com.metamatrix.connector.salesforce.connection.SalesforceConnection;
import com.metamatrix.connector.salesforce.execution.visitors.SelectVisitor;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.sobject.SObject;

public class QueryExecutionImpl implements ResultSetExecution {

	private SalesforceConnection connection;

	private RuntimeMetadata metadata;

	private ExecutionContext context;

	private ConnectorEnvironment connectorEnv;
	
	private SelectVisitor visitor;
	
	private QueryResult results;

	// Identifying values
	private String connectionIdentifier;

	private String connectorIdentifier;

	private String requestIdentifier;

	private String partIdentifier;

	private String logPreamble;
	
	private Map<String,Integer> fieldMap;
	
	private IQueryCommand query;
	
	private int i;

	public QueryExecutionImpl(IQueryCommand command, SalesforceConnection connection,
			RuntimeMetadata metadata, ExecutionContext context,
			ConnectorEnvironment connectorEnv) {
		this.connection = connection;
		this.metadata = metadata;
		this.context = context;
		this.connectorEnv = connectorEnv;
		this.query = command;

		connectionIdentifier = context.getConnectionIdentifier();
		connectorIdentifier = context.getConnectorIdentifier();
		requestIdentifier = context.getRequestIdentifier();
		partIdentifier = context.getPartIdentifier();
	}

	public void cancel() throws ConnectorException {
		connectorEnv.getLogger().logInfo(Messages.getString("SalesforceQueryExecutionImpl.cancel"));
	}

	public void close() throws ConnectorException {
		connectorEnv.getLogger().logInfo(Messages.getString("SalesforceQueryExecutionImpl.close"));
	}

	@Override
	public void execute() throws ConnectorException {
		connectorEnv.getLogger().logInfo(
				getLogPreamble() + "Incoming Query: " + query.toString());
		visitor = new SelectVisitor(metadata);
		visitor.visitNode(query);
		String finalQuery;
		finalQuery = visitor.getQuery().trim();
		connectorEnv.getLogger().logInfo(
				getLogPreamble() + "Executing Query: " + finalQuery);
		
		results = connection.query(finalQuery, this.context.getBatchSize());
	}
	
	@Override
	public List next() throws ConnectorException, DataNotAvailableException {
		while (results != null) {
			if (i < results.getSize()) {
				SObject sObject = results.getRecords(i);
				org.apache.axis.message.MessageElement[] fields = sObject.get_any();
				if (null == fieldMap) {
					logAndMapFields(fields);
				}
				return extractRowFromFields(sObject, fields);
			}
	
			if (results.isDone()) { // no more batches on sf.
				results = null;
				break;
			} 
			results = connection.queryMore(results.getQueryLocator());
		}
		return null;
	}

	private List<Object> extractRowFromFields(SObject sObject, MessageElement[] fields) throws ConnectorException {
		List<Object> row = new ArrayList<Object>();
		for (int j = 0; j < visitor.getSelectSymbolCount(); j++) {
			Element element = visitor.getSelectSymbolMetadata(j);
			Integer index = fieldMap.get(element.getNameInSource());
			// id gets dropped from the result if it is not the
			// first field in the querystring. Add it back in.
			if (null == index) {
				if (element.getNameInSource().equalsIgnoreCase("id")) {
					row.add(sObject.getId());
				} else {
					throw new ConnectorException("SalesforceQueryExecutionImpl.missing.field"
									+ element.getNameInSource());
				}
			} else {
				Object cell;
				cell = getCellDatum(element, fields[index]);
				row.add(cell);
			}
		}
		return row;
	}

	private void logAndMapFields(org.apache.axis.message.MessageElement[] fields) {
		logFields(fields);
		fieldMap = new HashMap<String, Integer>();
		for (int x = 0; x < fields.length; x++) {
			fieldMap.put(fields[x].getLocalName(), x);
		}
	}

	private void logFields(MessageElement[] fields) {
		ConnectorLogger logger = connectorEnv.getLogger();
		logger.logDetail("FieldCount = " + fields.length);
		for(int i = 0; i < fields.length; i++) {
			logger.logDetail("Field # " + i + " is " + fields[i].getLocalName());
		}
		
	}

	private Object getCellDatum(Element element, MessageElement me)
			throws ConnectorException {
		if(!element.getNameInSource().equals(me.getLocalName())) {
			throw new ConnectorException("SalesforceQueryExecutionImpl.column.mismatch1" + element.getNameInSource() +
					"SalesforceQueryExecutionImpl.column.mismatch2" + me.getLocalName());
		}
		String value = me.getValue();
		Object result = null;
		Class type = element.getJavaType();
		
		if(type.equals(String.class)) {
			result = value;
		}
		else if (type.equals(Boolean.class)) {
			result = Boolean.valueOf(value);
		} else if (type.equals(Double.class)) {
			if (null != value) {
				result = Double.valueOf(value);
			}
		} else if (type.equals(Integer.class)) {
			if (null != value) {
				result = Integer.valueOf(value);
			}
		} else if (type.equals(java.sql.Date.class)) {
			if (null != value) {
				result = java.sql.Date.valueOf(value);
			}
		} else if (type.equals(java.sql.Timestamp.class)) {
			if (null != value) {
				try {
					Date date = Util.getSalesforceDateTimeFormat().parse(value);
					result = new Timestamp(date.getTime());
				} catch (ParseException e) {
					throw new ConnectorException(e, "SalesforceQueryExecutionImpl.datatime.parse" + value);
				}
			}
		} else {
			result = value;
		}
		return result;
	}


	private String getLogPreamble() {
		if (null == logPreamble) {
			StringBuffer preamble = new StringBuffer();
			preamble.append(connectorIdentifier);
			preamble.append('.');
			preamble.append(connectionIdentifier);
			preamble.append('.');
			preamble.append(requestIdentifier);
			preamble.append('.');
			preamble.append(partIdentifier);
			preamble.append(": ");
			logPreamble = preamble.toString();
		}
		return logPreamble;
	}
}
