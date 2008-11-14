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

package com.metamatrix.cdk.api;

import java.io.IOException;
import java.net.URL;

import com.metamatrix.cdk.CommandBuilder;
import com.metamatrix.data.language.ICommand;
import com.metamatrix.data.metadata.runtime.RuntimeMetadata;
import com.metamatrix.dqp.internal.datamgr.metadata.MetadataFactory;
import com.metamatrix.dqp.internal.datamgr.metadata.RuntimeMetadataImpl;
import com.metamatrix.metadata.runtime.VDBMetadataFactory;
import com.metamatrix.query.metadata.QueryMetadataInterface;

/**
 * <p>This translation utility can be used to translate sql strings into 
 * Connector API language interfaces for testing purposes.  The utility
 * requires a metadata .vdb file in order to resolve references in 
 * the SQL.</p>  
 * 
 * <p>This utility class can also be used to obtain a RuntimeMetadata
 * implementation based on the VDB file, which is sometimes handy when writing 
 * unit tests.</p>
 */
public class TranslationUtility {
    
    private QueryMetadataInterface metadata;

    /**
     * Construct a utility instance with a given vdb file.  
     * @param vdbFile The .vdb file name representing metadata for the connector
     */
    public TranslationUtility(String vdbFile) {
        metadata = VDBMetadataFactory.getVDBMetadata(vdbFile);     
    }
    
    public TranslationUtility(URL url) {
        try {
			metadata = VDBMetadataFactory.getVDBMetadata(url);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}     
    }
    
    public TranslationUtility(QueryMetadataInterface metadata) {
    	this.metadata = metadata;
    }
    
    public ICommand parseCommand(String sql, boolean generateAliases, boolean supportsGroupAliases) {
        CommandBuilder commandBuilder = new CommandBuilder(metadata);
        return commandBuilder.getCommand(sql, generateAliases, supportsGroupAliases);
    }
    
    /**
     * Parse a SQL command and return an ICommand object.
     * @param sql
     * @return Command using the language interfaces
     */
    public ICommand parseCommand(String sql) {
        CommandBuilder commandBuilder = new CommandBuilder(metadata);
        return commandBuilder.getCommand(sql);
    }
    
    /**
     * Create a RuntimeMetadata that can be used for testing a connector.
     * This RuntimeMetadata instance will be backed by the metadata from the vdbFile
     * this translation utility instance was created with.
     * @return RuntimeMetadata for testing
     */
    public RuntimeMetadata createRuntimeMetadata() {
        return new RuntimeMetadataImpl(new MetadataFactory(metadata));
    }
}
