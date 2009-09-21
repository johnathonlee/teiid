/*
 * Copyright (c) 2000-2007 MetaMatrix, Inc.
 * All rights reserved.
 */
package org.teiid.test.testcases;

import org.teiid.test.framework.ConfigPropertyNames;
import org.teiid.test.framework.connection.ConnectionStrategy;


/** 
 * Local Transaction Test
 * 
 * Settings:
 * 
 * -	Using Driver
 * -	Autocommit = True
 * -	TxnAutoWrap = Off
 */
public class LocalTransactionDriverTrueOffTest extends TwoSourceTransactionScenarios {
          
    public LocalTransactionDriverTrueOffTest(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
    	
    	System.setProperty(ConfigPropertyNames.CONNECTION_TYPE, ConfigPropertyNames.CONNECTION_TYPES.DRIVER_CONNECTION);
    	System.setProperty(ConnectionStrategy.AUTOCOMMIT, "true");
    	System.setProperty(ConnectionStrategy.TXN_AUTO_WRAP, "off");
    	
		
	}      
      
}
