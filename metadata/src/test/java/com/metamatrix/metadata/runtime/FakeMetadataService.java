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

package com.metamatrix.metadata.runtime;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import org.teiid.connector.metadata.runtime.Datatype;
import org.teiid.metadata.CompositeMetadataStore;
import org.teiid.metadata.TransformationMetadata;
import org.teiid.metadata.index.IndexMetadataFactory;

import com.metamatrix.api.exception.MetaMatrixComponentException;
import com.metamatrix.common.application.ApplicationEnvironment;
import com.metamatrix.common.application.ApplicationService;
import com.metamatrix.common.application.exception.ApplicationInitializationException;
import com.metamatrix.common.application.exception.ApplicationLifecycleException;
import com.metamatrix.common.vdb.api.VDBArchive;
import com.metamatrix.core.util.TempDirectoryMonitor;
import com.metamatrix.dqp.service.MetadataService;
import com.metamatrix.metadata.runtime.api.MetadataSource;
import com.metamatrix.query.metadata.QueryMetadataInterface;


public class FakeMetadataService implements ApplicationService, MetadataService {
    private CompositeMetadataStore compositeMetadataStore;
    
    public FakeMetadataService(URL vdbFile) throws IOException, MetaMatrixComponentException {
        TempDirectoryMonitor.turnOn();
    	MetadataSource source = new VDBArchive(vdbFile.openStream());
    	compositeMetadataStore = new CompositeMetadataStore(Arrays.asList(new IndexMetadataFactory(source).getMetadataStore()), source);
    }

    public void clear() {
        TempDirectoryMonitor.removeAll();
    }

    public void initialize(Properties props) throws ApplicationInitializationException {

    }

    public void start(ApplicationEnvironment environment) throws ApplicationLifecycleException {

    }

    public void stop() throws ApplicationLifecycleException {

    }

	public QueryMetadataInterface lookupMetadata(String vdbName,String vdbVersion) throws MetaMatrixComponentException {
		return new TransformationMetadata(compositeMetadataStore);
	}

	public CompositeMetadataStore getMetadataObjectSource(String vdbName,String vdbVersion) throws MetaMatrixComponentException {
		return compositeMetadataStore;
	}
	
	@Override
	public Map<String, Datatype> getBuiltinDatatypes()
			throws MetaMatrixComponentException {
		return null;
	}
}
