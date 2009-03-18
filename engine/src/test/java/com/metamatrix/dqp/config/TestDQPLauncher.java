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

package com.metamatrix.dqp.config;

import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.mockito.Mockito;
import org.teiid.dqp.internal.process.DQPCore;

import com.metamatrix.common.application.ApplicationService;
import com.metamatrix.common.application.DQPConfigSource;
import com.metamatrix.core.log.LogListener;
import com.metamatrix.dqp.service.AutoGenDataService;
import com.metamatrix.dqp.service.DQPServiceNames;
import com.metamatrix.dqp.service.FakeAbstractService;
import com.metamatrix.dqp.service.FakeBufferService;
import com.metamatrix.dqp.service.FakeMetadataService;
import com.metamatrix.internal.core.log.PlatformLog;

/**
 */
public class TestDQPLauncher extends TestCase {

    /**
     * Constructor for TestDQPLauncher.
     * @param name
     */
    public TestDQPLauncher(String name) {
        super(name);
    }
    
    public void testLaunch() throws Exception {
        DQPConfigSource configSource = Mockito.mock(DQPConfigSource.class);
        Mockito.stub(configSource.getProperties()).toReturn(new Properties());
        HashMap<String, Class<? extends ApplicationService>> defaults = new HashMap<String, Class<? extends ApplicationService>>();
        Mockito.stub(configSource.getDefaultServiceClasses()).toReturn(defaults);
        String[] services = new String[] {DQPServiceNames.BUFFER_SERVICE, DQPServiceNames.METADATA_SERVICE, DQPServiceNames.DATA_SERVICE};
        defaults.put(DQPServiceNames.BUFFER_SERVICE, FakeBufferService.class);
        defaults.put(DQPServiceNames.METADATA_SERVICE, FakeMetadataService.class);
        defaults.put(DQPServiceNames.DATA_SERVICE, AutoGenDataService.class);
        
        DQPCore dqpCore = new DQPCore();
        dqpCore.start(configSource);
    	
        PlatformLog log = new PlatformLog();
    	List<LogListener> list = log.getLogListeners();
    	for(LogListener l: list) {
    		log.removeListener(l);
    	}
                
        DQPCore dqp = new DQPCore();
        dqp.start(configSource);
        assertNotNull("DQP should not be null", dqp); //$NON-NLS-1$
        
        // Check that bootstrapping occurred
        for(int i=0; i<services.length; i++) {
            FakeAbstractService svc = (FakeAbstractService)dqpCore.getEnvironment().findService(services[i]);
            assertEquals("service " + svc.getClass().getName() + " not init'ed correct # of times ", 1, svc.getInitializeCount()); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("service " + svc.getClass().getName() + " not start'ed correct # of times ", 1, svc.getStartCount()); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("service " + svc.getClass().getName() + " not stop'ed correct # of times ", 0, svc.getStopCount()); //$NON-NLS-1$ //$NON-NLS-2$
        }

    }
    
}
