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

import com.metamatrix.common.types.AbstractTransform;
import com.metamatrix.common.types.TransformationException;
import com.metamatrix.core.CorePlugin;

public class FloatToBooleanTransform extends AbstractTransform {

	private static final Float FALSE = new Float(0);
	private static final Float TRUE = new Float(1);
	
	/**
	 * This method transforms a value of the source type into a value
	 * of the target type.
	 * @param value Incoming value of source type
	 * @return Outgoing value of target type
	 * @throws TransformationException if value is an incorrect input type or
	 * the transformation fails
	 */
	public Object transform(Object value) throws TransformationException {
		if(value == null) {
			return value;
		}

		if(value.equals(FALSE)) {
			return Boolean.FALSE;
		} else if(value.equals(TRUE)) {
			return Boolean.TRUE;
		} else {
			throw new TransformationException(CorePlugin.Util.getString("FloatToBooleanTransform.Failed_transform"));				 //$NON-NLS-1$
		}	
	}

	/**
	 * Type of the incoming value.
	 * @return Source type
	 */
	public Class getSourceType() {
		return Float.class;
	}

	/**
	 * Type of the outgoing value.
	 * @return Target type
	 */
	public Class getTargetType() {
		return Boolean.class;
	}

	/**
	 * Flag if the transformation from source to target is 
	 * a narrowing transformation that may lose information.
	 * @return True - this transformation is narrowing
	 */
	public boolean isNarrowing() {
		return true;
	}
}
