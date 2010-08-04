/*
 * JBoss, Home of Professional Open Source.
 * Copyright (C) 2009 Red Hat, Inc.
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

package org.teiid.query.processor;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.teiid.common.buffer.BufferManagerFactory;
import org.teiid.core.TeiidProcessingException;
import org.teiid.dqp.internal.process.SimpleQueryProcessorFactory;
import org.teiid.query.metadata.TempMetadataAdapter;
import org.teiid.query.optimizer.capabilities.CapabilitiesFinder;
import org.teiid.query.optimizer.capabilities.DefaultCapabilitiesFinder;
import org.teiid.query.tempdata.TempTableDataManager;
import org.teiid.query.tempdata.TempTableStore;
import org.teiid.query.unittest.RealMetadataFactory;
import org.teiid.query.util.CommandContext;

@SuppressWarnings("nls")
public class TestMaterialization {
	
	private TempMetadataAdapter metadata;
	private TempTableDataManager dataManager;
	private TempTableStore tempStore;
	private TempTableStore globalStore;
	private ProcessorPlan previousPlan;
	private HardcodedDataManager hdm;
	
	@Before public void setUp() {
		tempStore = new TempTableStore("1"); //$NON-NLS-1$
		globalStore  = new TempTableStore("SYSTEM");
		metadata = new TempMetadataAdapter(RealMetadataFactory.exampleMaterializedView(), tempStore.getMetadataStore());
		hdm = new HardcodedDataManager();
		hdm.addData("SELECT matsrc.x FROM matsrc", new List[] {Arrays.asList((String)null), Arrays.asList("one"), Arrays.asList("two"), Arrays.asList("three")});
		hdm.addData("SELECT mattable.info.e1, mattable.info.e2 FROM mattable.info", new List[] {Arrays.asList("a", 1), Arrays.asList("a", 2)});
		hdm.addData("SELECT mattable.info.e2, mattable.info.e1 FROM mattable.info", new List[] {Arrays.asList(1, "a"), Arrays.asList(2, "a")});
		dataManager = new TempTableDataManager(hdm, BufferManagerFactory.getStandaloneBufferManager());
	}
	
	private void execute(String sql, List... expectedResults) throws Exception {
		CommandContext cc = TestProcessor.createCommandContext();
		cc.setTempTableStore(tempStore);
		cc.setGlobalTableStore(globalStore);
		cc.setMetadata(metadata);
		CapabilitiesFinder finder = new DefaultCapabilitiesFinder();
		previousPlan = TestProcessor.helpGetPlan(TestProcessor.helpParse(sql), metadata, finder, cc);
		cc.setQueryProcessorFactory(new SimpleQueryProcessorFactory(BufferManagerFactory.getStandaloneBufferManager(), dataManager, finder, null, metadata));
		TestProcessor.doProcess(previousPlan, dataManager, expectedResults, cc);
	}

	@Test public void testPopulate() throws Exception {
		execute("SELECT * from vgroup3 where x = 'one'", Arrays.asList("one", "zne"));
		assertEquals(1, hdm.getCommandHistory().size());
		execute("SELECT * from vgroup3 where x is null", Arrays.asList(null, null));
		assertEquals(1, hdm.getCommandHistory().size());
	}
	
    @Test(expected=TeiidProcessingException.class) public void testCodeTableResponseException() throws Exception {
    	//duplicate key
    	execute("select lookup('mattable.info', 'e2', 'e1', 'a')");
    }
    
    @Test public void testCodeTable() throws Exception {
    	execute("select lookup('mattable.info', 'e1', 'e2', 5)", Arrays.asList((String)null));
    	assertEquals(1, hdm.getCommandHistory().size());
    	execute("select lookup('mattable.info', 'e1', 'e2', 1)", Arrays.asList("a"));
    	assertEquals(1, hdm.getCommandHistory().size());
    }
    
}
