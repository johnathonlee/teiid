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

package com.metamatrix.common.types.basic;

import java.sql.Timestamp;

import com.metamatrix.common.types.Transform;
import com.metamatrix.common.types.TransformationException;
import com.metamatrix.core.CorePlugin;
import com.metamatrix.core.ErrorMessageKeys;

public class StringToTimestampTransform extends Transform {

	/**
	 * This method transforms a value of the source type into a value
	 * of the target type.
	 * @param value Incoming value of source type
	 * @return Outgoing value of target type
	 * @throws TransformationException if value is an incorrect input type or
	 * the transformation fails
	 */
	public Object transformDirect(Object value) throws TransformationException {
		value = ((String) value).trim();
		Timestamp result = null;
		try {
			result = Timestamp.valueOf( (String) value );
		} catch(Exception e) {
			throw new TransformationException(e, ErrorMessageKeys.TYPES_ERR_0024, CorePlugin.Util.getString(ErrorMessageKeys.TYPES_ERR_0024, value));
		}
		//validate everything except for fractional seconds
		if (!((String)value).startsWith(result.toString().substring(0, 19))) {
			throw new TransformationException(CorePlugin.Util.getString("transform.invalid_string_for_date", value, getTargetType().getSimpleName())); //$NON-NLS-1$
		}
		return result;
	}

	/**
	 * Type of the incoming value.
	 * @return Source type
	 */
	public Class getSourceType() {
		return String.class;
	}

	/**
	 * Type of the outgoing value.
	 * @return Target type
	 */
	public Class getTargetType() {
		return Timestamp.class;
	}
	
	@Override
	public boolean isNarrowing() {
		return true;
	}

}
