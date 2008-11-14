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

package com.metamatrix.common.protocol;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import com.metamatrix.common.protocol.classpath.ClasspathURLConnection;
import com.metamatrix.common.protocol.mmfile.MMFileURLConnection;
import com.metamatrix.common.protocol.mmrofile.MMROFileURLConnection;


/** 
 * A factory class for registering the "classpath:" and "mmfile:" protocol based URL.
 * since we can not register in app servers currently this only used for testing.
 * @since 4.4
 */
public class MetaMatrixURLStreamHandlerFactory implements URLStreamHandlerFactory {
    private static final String DEFAULT_HANDLER_PKG = "sun.net.www.protocol"; //$NON-NLS-1$
    
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if (protocol.equals(ClasspathURLConnection.PROTOCOL)) {
            return new com.metamatrix.common.protocol.classpath.Handler();
        }
        else if (protocol.equals(MMFileURLConnection.PROTOCOL)) {
            return new com.metamatrix.common.protocol.mmfile.Handler();
        }
        else if (protocol.equals(MMROFileURLConnection.PROTOCOL)) {
            return new com.metamatrix.common.protocol.mmrofile.Handler();
        }        
        else {
            String handlerName = DEFAULT_HANDLER_PKG+"."+protocol+".Handler"; //$NON-NLS-1$ //$NON-NLS-2$
            try {
                Class handlerClass = Class.forName(handlerName);
                return (URLStreamHandler)handlerClass.newInstance();
            } catch (Exception e) {    
                // eat it and return null
            }
        }
        return null;
    }
}
