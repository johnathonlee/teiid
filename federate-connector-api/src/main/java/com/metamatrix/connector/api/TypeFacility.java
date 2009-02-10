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

package com.metamatrix.connector.api;

import com.metamatrix.common.types.DataTypeManager;
import com.metamatrix.common.types.MMJDBCSQLTypeInfo;
import com.metamatrix.connector.exception.ConnectorException;

/**
 */
public abstract class TypeFacility {

    public interface RUNTIME_TYPES {
        public static final Class STRING        = DataTypeManager.DefaultDataClasses.STRING;
        public static final Class BOOLEAN       = DataTypeManager.DefaultDataClasses.BOOLEAN;
        public static final Class BYTE          = DataTypeManager.DefaultDataClasses.BYTE;
        public static final Class SHORT         = DataTypeManager.DefaultDataClasses.SHORT;
        public static final Class CHAR          = DataTypeManager.DefaultDataClasses.CHAR;
        public static final Class INTEGER       = DataTypeManager.DefaultDataClasses.INTEGER;
        public static final Class LONG          = DataTypeManager.DefaultDataClasses.LONG;
        public static final Class BIG_INTEGER   = DataTypeManager.DefaultDataClasses.BIG_INTEGER;
        public static final Class FLOAT         = DataTypeManager.DefaultDataClasses.FLOAT;
        public static final Class DOUBLE        = DataTypeManager.DefaultDataClasses.DOUBLE;
        public static final Class BIG_DECIMAL   = DataTypeManager.DefaultDataClasses.BIG_DECIMAL;
        public static final Class DATE          = DataTypeManager.DefaultDataClasses.DATE;
        public static final Class TIME          = DataTypeManager.DefaultDataClasses.TIME;
        public static final Class TIMESTAMP     = DataTypeManager.DefaultDataClasses.TIMESTAMP;
        public static final Class OBJECT        = DataTypeManager.DefaultDataClasses.OBJECT;
        public static final Class BLOB          = DataTypeManager.DefaultDataClasses.BLOB;
        public static final Class CLOB          = DataTypeManager.DefaultDataClasses.CLOB;
        public static final Class XML           = DataTypeManager.DefaultDataClasses.XML;
    }
    
    public static final class RUNTIME_NAMES {
        public static final String STRING       = DataTypeManager.DefaultDataTypes.STRING;
        public static final String BOOLEAN      = DataTypeManager.DefaultDataTypes.BOOLEAN;
        public static final String BYTE         = DataTypeManager.DefaultDataTypes.BYTE;
        public static final String SHORT        = DataTypeManager.DefaultDataTypes.SHORT;
        public static final String CHAR         = DataTypeManager.DefaultDataTypes.CHAR;
        public static final String INTEGER      = DataTypeManager.DefaultDataTypes.INTEGER;
        public static final String LONG         = DataTypeManager.DefaultDataTypes.LONG;
        public static final String BIG_INTEGER  = DataTypeManager.DefaultDataTypes.BIG_INTEGER;
        public static final String FLOAT        = DataTypeManager.DefaultDataTypes.FLOAT;
        public static final String DOUBLE       = DataTypeManager.DefaultDataTypes.DOUBLE;
        public static final String BIG_DECIMAL  = DataTypeManager.DefaultDataTypes.BIG_DECIMAL;
        public static final String DATE         = DataTypeManager.DefaultDataTypes.DATE;
        public static final String TIME         = DataTypeManager.DefaultDataTypes.TIME;
        public static final String TIMESTAMP    = DataTypeManager.DefaultDataTypes.TIMESTAMP;
        public static final String OBJECT       = DataTypeManager.DefaultDataTypes.OBJECT;
        public static final String NULL         = DataTypeManager.DefaultDataTypes.NULL;
        public static final String BLOB         = DataTypeManager.DefaultDataTypes.BLOB;
        public static final String CLOB         = DataTypeManager.DefaultDataTypes.CLOB;
        public static final String XML         	= DataTypeManager.DefaultDataTypes.XML;
    }
    
    public static Class getDataTypeClass(String type) {
    	return DataTypeManager.getDataTypeClass(type);    	
    }
    
    public static final int getSQLTypeFromRuntimeType(Class type) {
        return MMJDBCSQLTypeInfo.getSQLTypeFromRuntimeType(type);
    }    
    
    public abstract boolean hasTransformation(Class sourceClass, Class targetClass);
    
    public abstract <T> T transformValue(Object value, Class sourceClass, Class<T> targetClass) throws ConnectorException;
    
    public abstract Object convertToRuntimeType(Object value);

}
