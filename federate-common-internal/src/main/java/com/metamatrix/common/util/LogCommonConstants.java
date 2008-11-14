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

package com.metamatrix.common.util;

public interface LogCommonConstants {

    // **********************************************************************
    // PLEASE NOTE:!!!!!!!!!!!!!!!!!
    // All constants defined here should also be defined in
    // com.metamatrix.common.util.LogContextsUtil
    // **********************************************************************

    // Contexts
    public static final String CTX_DBIDGEN = "DBIDGEN"; //$NON-NLS-1$

    public static final String CTX_LOGON = "LOGON"; //$NON-NLS-1$
    public static final String CTX_SERVICE = "SERVICE"; //$NON-NLS-1$
    public static final String CTX_PROXY = "PROXY"; //$NON-NLS-1$
    public static final String CTX_CONTROLLER = "CONTROLLER"; //$NON-NLS-1$
    public static final String CTX_CONFIG = "CONFIG"; //$NON-NLS-1$
    public static final String CTX_LOGGING = "LOG"; //$NON-NLS-1$
    public static final String CTX_MESSAGE_BUS = "MESSAGE_BUS"; //$NON-NLS-1$
    public static final String CTX_STANDARD_OUT = "STDOUT"; //$NON-NLS-1$
    public static final String CTX_STANDARD_ERR = "STDERR"; //$NON-NLS-1$
    public static final String CTX_DISTRIB_CACHE = "DISTRIB_CACHE"; //$NON-NLS-1$
    public static final String CTX_POOLING = "RESOURCE_POOLING"; //$NON-NLS-1$
    public static final String CTX_BUFFER_MGR = "BUFFER_MGR"; //$NON-NLS-1$
    public static final String CTX_STORAGE_MGR = "STORAGE_MGR"; //$NON-NLS-1$
    public static final String CTX_XA_TXN = "XA_TXN"; //$NON-NLS-1$
    public static final String CTX_TXN_LOG = "TXN_LOG"; //$NON-NLS-1$
    public static final String CTX_EXTENSION_SOURCE = "EXTENSION_MODULE"; //$NON-NLS-1$
    public static final String CTX_EXTENSION_SOURCE_JDBC = "JDBC_EXT_MODULE_TRANSACTION"; //$NON-NLS-1$

    // Types
    public static final String TYPE_INFO = "INFO"; //$NON-NLS-1$
    public static final String TYPE_TRACE = "TRACE"; //$NON-NLS-1$
    public static final String TYPE_ERROR = "ERROR"; //$NON-NLS-1$
    public static final String TYPE_DEBUG = "DEBUG"; //$NON-NLS-1$
    public static final String TYPE_EXCEPTION = "EXCEPTION"; //$NON-NLS-1$
    public static final String TYPE_WARNING = "WARNING"; //$NON-NLS-1$
}
