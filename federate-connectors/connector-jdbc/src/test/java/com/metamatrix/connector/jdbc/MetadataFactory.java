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

package com.metamatrix.connector.jdbc;

import junit.framework.Assert;

import com.metamatrix.cdk.api.TranslationUtility;
import com.metamatrix.cdk.unittest.FakeTranslationFactory;
import com.metamatrix.connector.language.ICommand;

public class MetadataFactory {
	
    public static final String PARTS_VDB = "/PartsSupplier.vdb"; //$NON-NLS-1$
    public static final String BQT_VDB = "/bqt.vdb"; //$NON-NLS-1$

    public static ICommand helpTranslate(String vdbFileName, String sql) {
    	TranslationUtility util = null;
    	if (PARTS_VDB.equals(vdbFileName)) {
    		util = new TranslationUtility(MetadataFactory.class.getResource(vdbFileName));
    	} else if (BQT_VDB.equals(vdbFileName)){
    		util = FakeTranslationFactory.getInstance().getBQTTranslationUtility();
    	} else {
    		Assert.fail("unknown vdb"); //$NON-NLS-1$
    	}
        return util.parseCommand(sql);        
    }

}
