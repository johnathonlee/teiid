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

package org.teiid.core.types.basic;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;

import org.teiid.core.CorePlugin;
import org.teiid.core.types.DataTypeManager;
import org.teiid.core.types.SQLXMLImpl;
import org.teiid.core.types.Transform;
import org.teiid.core.types.TransformationException;
import org.teiid.core.types.XMLType;
import org.teiid.core.types.XMLType.Type;


public class StringToSQLXMLTransform extends Transform {

	/**
	 * This method transforms a value of the source type into a value
	 * of the target type.
	 * @param value Incoming value of source type
	 * @return Outgoing value of target type
	 * @throws TransformationException if value is an incorrect input type or
	 * the transformation fails
	 */
	public Object transformDirect(Object value) throws TransformationException {
        String xml = (String)value;
        Reader reader = new StringReader(xml);
        Type type = isXml(reader);
        XMLType result = new XMLType(new SQLXMLImpl(xml));
        result.setType(type);
        return result;
	}

	public static Type isXml(Reader reader) throws TransformationException {
		Type type = Type.ELEMENT;
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        try{        
             XMLEventReader xmlReader = inputFactory.createXMLEventReader(reader);
             while (xmlReader.hasNext()) {
            	 XMLEvent event = xmlReader.nextEvent();
            	 if  (event.isStartDocument() && event.getLocation().getColumnNumber() != 1) {
            		 type = Type.DOCUMENT;
            	 }
             }
        } catch (Exception e){
            throw new TransformationException(e, CorePlugin.Util.getString("invalid_string")); //$NON-NLS-1$
        } finally {
        	try {
				reader.close();
			} catch (IOException e) {
			}
        }
        return type;
	}

	/**
	 * Type of the incoming value.
	 * @return Source type
	 */
	public Class<?> getSourceType() {
		return DataTypeManager.DefaultDataClasses.STRING;
	}

	/**
	 * Type of the outgoing value.
	 * @return Target type
	 */
	public Class<?> getTargetType() {
		return DataTypeManager.DefaultDataClasses.XML;
	}
	
	@Override
	public boolean isExplicit() {
		return true;
	}
	
}
