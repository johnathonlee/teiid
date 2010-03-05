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
package com.metamatrix.connector.salesforce.execution.visitors;

import org.teiid.connector.api.ConnectorException;
import org.teiid.connector.language.Update;
import org.teiid.connector.metadata.runtime.RuntimeMetadata;


public class UpdateVisitor extends CriteriaVisitor implements IQueryProvidingVisitor {

	public UpdateVisitor(RuntimeMetadata metadata) { 
		super(metadata);
	}

	@Override
	public void visit(Update update) {
		// don't visit the changes or they will be in the query.
		visitNode(update.getTable());
        visitNode(update.getWhere());
		try {
			loadColumnMetadata(update.getTable());
		} catch (ConnectorException ce) {
			exceptions.add(ce);
		}
	}
	
	/*
	 * The SOQL SELECT command uses the following syntax: SELECT fieldList FROM
	 * objectType [WHERE The Condition Expression (WHERE Clause)] [ORDER BY]
	 * LIMIT ?
	 */

	public String getQuery() throws ConnectorException {
		if (!exceptions.isEmpty()) {
			throw ((ConnectorException) exceptions.get(0));
		}
		StringBuffer result = new StringBuffer();
		result.append(SELECT).append(SPACE);
		result.append("Id").append(SPACE);
		result.append(FROM).append(SPACE);
		result.append(table.getNameInSource()).append(SPACE);
		addCriteriaString(result);
		return result.toString();
	}

}
