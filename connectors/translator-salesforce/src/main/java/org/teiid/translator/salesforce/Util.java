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
package org.teiid.translator.salesforce;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.teiid.translator.TranslatorException;


public class Util {

	public static String stripQutes(String id) {
		if((id.startsWith("'") && id.endsWith("'"))) {
			id = id.substring(1,id.length()-1);
		} else if ((id.startsWith("\"") && id.endsWith("\""))) {
			id = id.substring(1,id.length()-1);
		}
		return id;
	}
	
	public static String addSingleQuotes(String text) {
		StringBuffer result = new StringBuffer();
		if(!text.startsWith("'")) {
			result.append('\'');
		}
		result.append(text);
		if(!text.endsWith("'")) {
			result.append('\'');
		} 
		return result.toString();
	}
	
	public static void validateQueryLength(StringBuffer query) throws TranslatorException {
		if(query.length() >= 10000) {
			throw new TranslatorException(Messages.getString("Util.query.exceeds.max.length"));
		}
	}

	public static SimpleDateFormat getSalesforceDateTimeFormat() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	}
	
	public static SimpleDateFormat getTimeZoneOffsetFormat() {
		return new SimpleDateFormat("Z");
	}

	public static DateFormat getSalesforceDateFormat() {
		return new SimpleDateFormat("yyyy-MM-dd");
	}
	
}
