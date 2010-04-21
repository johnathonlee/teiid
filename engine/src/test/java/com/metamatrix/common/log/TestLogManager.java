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

package com.metamatrix.common.log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;


/**
 * This test case tests the LogManager.
 */
public class TestLogManager extends TestCase {

	String context = "SomeContext"; //$NON-NLS-1$
	
    /**
     * Constructor for TestLogManager.
     * @param name
     */
    public TestLogManager(String name) {
        super(name);
    }
    
	@Override
	protected void setUp() throws Exception {
    	ListLogger logger = new ListLogger();
    	logger.setLogLevel(context, MessageLevel.DETAIL);
    	LogManager.logListener = logger;
	}    
    
    // =========================================================================
    //                         T E S T     C A S E S
    // =========================================================================

    /*
     * Test for boolean isMessageToBeRecorded(String, int)
     */
    public void testIsMessageToBeRecordedString() {
    	assertTrue(LogManager.isMessageToBeRecorded(context, MessageLevel.CRITICAL) ); 
    	
    	ListLogger cfg = (ListLogger)LogManager.logListener;
        cfg.setLogLevel(context, MessageLevel.NONE);
        assertFalse(LogManager.isMessageToBeRecorded(context, MessageLevel.CRITICAL) );
    }

    /**
     * Test that all msgs logged are equal and output in same order.
     */
    public void testLogMessage() throws Exception {
    	ListLogger cfg = (ListLogger)LogManager.logListener;
        cfg.setLogLevel(context, MessageLevel.INFO );

        List<String> sentMsgList = new ArrayList<String>();
        sentMsgList.add("A message 1"); //$NON-NLS-1$
        sentMsgList.add("A message 2"); //$NON-NLS-1$
        sentMsgList.add("A message 3"); //$NON-NLS-1$
        sentMsgList.add("A message 4"); //$NON-NLS-1$
        sentMsgList.add("A message 5"); //$NON-NLS-1$
        sentMsgList.add("A message 6"); //$NON-NLS-1$

        for (Iterator iter = sentMsgList.iterator(); iter.hasNext();) {
            String msg = (String) iter.next();
            LogManager.logInfo(context, msg); 
        }
        
        List recevedMsgList = cfg.getLoggedMessages();
        assertEquals(sentMsgList.size(), recevedMsgList.size());
        assertEquals(sentMsgList, recevedMsgList);
    }
    
    /**
     *
     * A log listener that saves messages (IStatus)s in a
     * List for later comparison.
     */
    class ListLogger implements LogListener {
        private List<String> messages = new ArrayList<String>();
        private Map<String, Integer> contextMap = new HashMap<String, Integer>();
    	private int defaultLevel;

        public ListLogger() {
        }
        
        /* (non-Javadoc)
         * @see com.metamatrix.core.log.LogListener#logMessage(org.eclipse.core.runtime.IStatus, long, java.lang.String, java.lang.String)
         */
        public void log(int level, String context, Object msg){
            this.messages.add(msg.toString());
        }
        
		public void log(int level, String context, Throwable t, Object msg) {
            this.messages.add(msg.toString());
		}        

        /* (non-Javadoc)
         * @see com.metamatrix.core.log.LogListener#shutdown()
         */
        public void shutdown() {
            messages.clear();
            messages = null;

        }

        public int size() {
            return this.messages.size();
        }

        public List getLoggedMessages() {
            return this.messages;
        }
    	
    	public Set<String> getContexts() {
    		return this.contextMap.keySet();
    	}

    	public int getLogLevel(String context) {				
    		Integer level = this.contextMap.get(context);
    		if (level != null) {
    			return level;
    		}
    		return defaultLevel;
    	}

    	public void setLogLevel(String context, int logLevel) {
    		this.contextMap.put(context, logLevel);
    	}

    	@Override
    	public boolean isEnabled(String context, int msgLevel) {
    		int level = getLogLevel(context);
    		return level >= msgLevel;
    	}
    }

}
