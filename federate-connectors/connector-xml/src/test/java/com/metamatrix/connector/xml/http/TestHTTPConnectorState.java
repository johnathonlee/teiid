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

package com.metamatrix.connector.xml.http;

import java.util.Properties;

import junit.framework.TestCase;

import com.metamatrix.cdk.api.EnvironmentUtility;
import com.metamatrix.cdk.api.SysLogger;
import com.metamatrix.connector.xml.base.ProxyObjectFactory;
import com.metamatrix.data.exception.ConnectorException;

/**
 * created by JChoate on Jun 27, 2005
 *
 */
public class TestHTTPConnectorState extends TestCase {

    /**
     * Constructor for HTTPConnectorStateTest.
     * @param arg0
     */
    public TestHTTPConnectorState(String arg0) {
        super(arg0);
    }

    protected void setUp() throws Exception {
		super.setUp();
	}

	public void testHTTPConnectorState() {
        HTTPConnectorState state = new HTTPConnectorState();
        assertNotNull(state);
    }

    public void testHTTPConnectorStateProperties() {
    	Properties props = ProxyObjectFactory.getDefaultXMLRequestProps();
        Properties propOut = null;
        try {
        	HTTPConnectorState state = new HTTPConnectorState();
        	state.setLogger(new SysLogger(false));
        	state.setState(EnvironmentUtility.createEnvironment(props));
            propOut = state.getState();
        } catch (ConnectorException ce) {
         ce.printStackTrace();
         fail(ce.getMessage());
        }
    	assertNotNull(propOut);
    	assertEquals(props.getProperty(HTTPConnectorState.ACCESS_METHOD), propOut.getProperty(HTTPConnectorState.ACCESS_METHOD));    	
    	assertEquals(props.getProperty(HTTPConnectorState.PARAMETER_METHOD), propOut.getProperty(HTTPConnectorState.PARAMETER_METHOD));
    	assertEquals(props.getProperty(HTTPConnectorState.PROXY_URI), propOut.getProperty(HTTPConnectorState.PROXY_URI));
    	assertEquals(props.getProperty(HTTPConnectorState.URI), propOut.getProperty(HTTPConnectorState.URI));
    	assertEquals(props.getProperty(HTTPConnectorState.REQUEST_TIMEOUT), propOut.getProperty(HTTPConnectorState.REQUEST_TIMEOUT));
    	assertEquals(props.getProperty(HTTPConnectorState.XML_PARAMETER_NAME), 
    			propOut.getProperty(HTTPConnectorState.XML_PARAMETER_NAME));	
    }
    
    public void testGetSetProperties() {
    	Properties props = ProxyObjectFactory.getDefaultXMLRequestProps();
    	HTTPConnectorState state = new HTTPConnectorState();
        Properties propOut = null;
        try {
        	state.setLogger(new SysLogger(false));
        	state.setState(EnvironmentUtility.createEnvironment(props));
        	propOut = state.getState();
        } catch (ConnectorException ce) {
         ce.printStackTrace();
         fail(ce.getMessage());
        }
       
    	assertNotNull(propOut);
    	assertEquals(props.getProperty(HTTPConnectorState.ACCESS_METHOD), propOut.getProperty(HTTPConnectorState.ACCESS_METHOD));    	
    	assertEquals(props.getProperty(HTTPConnectorState.PARAMETER_METHOD), propOut.getProperty(HTTPConnectorState.PARAMETER_METHOD));
    	assertEquals(props.getProperty(HTTPConnectorState.PROXY_URI), propOut.getProperty(HTTPConnectorState.PROXY_URI));
    	assertEquals(props.getProperty(HTTPConnectorState.URI), propOut.getProperty(HTTPConnectorState.URI));
    	assertEquals(props.getProperty(HTTPConnectorState.REQUEST_TIMEOUT), 
    			propOut.getProperty(HTTPConnectorState.REQUEST_TIMEOUT));
    	assertEquals(props.getProperty(HTTPConnectorState.XML_PARAMETER_NAME), 
    			propOut.getProperty(HTTPConnectorState.XML_PARAMETER_NAME));
    }

    public void testSetGetAccessMethod() {
    	HTTPConnectorState state = new HTTPConnectorState();
    	state.setAccessMethod(HTTPConnectorState.GET);
    	assertEquals(HTTPConnectorState.GET, state.getAccessMethod());
    }
    
    public void testSetGetParameterMethod() {
    	HTTPConnectorState state = new HTTPConnectorState();
    	state.setParameterMethod(HTTPConnectorState.PARAMETER_XML_REQUEST);
    	assertEquals(HTTPConnectorState.PARAMETER_XML_REQUEST, state.getParameterMethod());
    }
    
    public void testSetGetUri() {
    	HTTPConnectorState state = new HTTPConnectorState();
    	String uri = "http://www.metamatrix.com:80";
    	state.setUri(uri);
    	assertEquals(uri, state.getUri());
    }
    
    public void testSetGetProxyUri() {
    	HTTPConnectorState state = new HTTPConnectorState();
    	String uri = "http://www.metamatrix.com:80";
    	state.setProxyUri(uri);
    	assertEquals(uri, state.getProxyUri());
    }
    
    public void testSetGetRequestTimeout() {
    	HTTPConnectorState state = new HTTPConnectorState();
    	final int timeOut = 5000;
    	state.setRequestTimeout(timeOut);
    	assertEquals(timeOut, state.getRequestTimeout());
    }
    
    public void testGetSetXmlParameterName() {
    	HTTPConnectorState state = new HTTPConnectorState();
    	String paramName = "RequestParam";
    	state.setXmlParameterName(paramName);
    	assertEquals(paramName, state.getXmlParameterName());
    }
    
    public void testHostnameVerifier() {
        Properties props = ProxyObjectFactory.getDefaultXMLRequestProps();
        props.put("HostnameVerifier", "com.metamatrix.connector.xml.MockHostnameVerifier");
        HTTPConnectorState state = new HTTPConnectorState();
        Properties propOut = null;
        try {
           	state.setLogger(new SysLogger(false));
           	state.setState(EnvironmentUtility.createEnvironment(props));
        } catch (ConnectorException ce) {
        	fail(ce.getMessage());
        }
    }
    
    public void testBadHostnameVerifier() {
        Properties props = ProxyObjectFactory.getDefaultXMLRequestProps();
        props.put("HostnameVerifier", "com.metamatrix.connector.xml.BogusHostnameVerifier");
        HTTPConnectorState state = new HTTPConnectorState();
        Properties propOut = null;
        try {
           	state.setLogger(new SysLogger(false));
           	state.setState(EnvironmentUtility.createEnvironment(props));
        } catch (ConnectorException ce) {
        	return;
        }
    }
}
