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

package com.metamatrix.connector.jdbc.oracle.spatial;

import java.util.ArrayList;
import java.util.List;

import com.metamatrix.connector.language.IExpression;
import com.metamatrix.connector.language.IFunction;

public class RelateFunctionModifier extends OracleSpatialFunctionModifier {

    /**
     * Implement this method to modify the function before it gets translated
     */
    public IExpression modify(IFunction function) {
        function.setName("SDO_RELATE"); //$NON-NLS-1$
        return function;
    }

    /**
     * Implement this method to change how the function is translated into the SQL string In this case, if either of the first two
     * parameters are a Literal String, then we need to put the literal itself in the SQL to be passed to Oracle, without the tick
     * marks
     */
    public List translate(IFunction function) {
        List objs = new ArrayList();
        objs.add("SDO_RELATE"); // recast name from sdoRelate to SDO_RELATE //$NON-NLS-1$
        objs.add("("); //$NON-NLS-1$
        IExpression[] params = function.getParameters();
        //if it doesn't have 3 parms, it is not a version of SDO_RELATE which
        // we are prepared to translate
        if (params.length == 3) {
            addParamWithConversion(objs, params[0]);
            objs.add(", "); //comma between parms //$NON-NLS-1$

            addParamWithConversion(objs, params[1]);
            objs.add(", "); //$NON-NLS-1$
            addParamWithConversion(objs, params[2]);
        } else {
            return super.translate(function);
        }
        objs.add(")"); //$NON-NLS-1$

        return objs;
    }

}