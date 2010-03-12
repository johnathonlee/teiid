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

import java.util.Date;

import org.jboss.managed.api.annotation.ManagementProperty;
import org.jboss.metatype.api.annotations.MetaMapping;
import org.teiid.adminapi.Transaction;

import com.metamatrix.admin.AdminPlugin;

@MetaMapping(TransactionMetadataMapper.class)
public class TransactionMetadata extends AdminObjectImpl implements Transaction {

	private static final long serialVersionUID = -8588785315218789068L;
	private long associatedSession;
	private String scope;
	private String id;
	private long createdTime;

	@Override
	@ManagementProperty(description="Session ID", readOnly=true)
	public long getAssociatedSession() {
		return associatedSession;
	}

	public void setAssociatedSession(long associatedSession) {
		this.associatedSession = associatedSession;
	}

	@Override
	@ManagementProperty(description="Scope", readOnly=true)
	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	@Override
	@ManagementProperty(description="ID", readOnly=true)
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	@Override
	@ManagementProperty(description="Transaction created time", readOnly=true)
	public long getCreatedTime() {
		return createdTime;
	}
	
	public void setCreatedTime(long time) {
		this.createdTime = time;
	}

	@Override
	public String toString() {
        StringBuffer result = new StringBuffer();
        result.append(AdminPlugin.Util.getString("TransactionImpl.associatedSession")).append(associatedSession); //$NON-NLS-1$
        result.append(AdminPlugin.Util.getString("TransactionImpl.scope")).append(scope); //$NON-NLS-1$
        result.append(AdminPlugin.Util.getString("TransactionImpl.id")).append(id); //$NON-NLS-1$
        result.append(AdminPlugin.Util.getString("TransactionImpl.createdTime")).append(new Date(createdTime)); //$NON-NLS-1$
        return result.toString();
	}

}
