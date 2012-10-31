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

package org.teiid.query.function;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import org.junit.Test;
import org.teiid.language.SQLConstants.NonReserved;
import org.teiid.query.unittest.TimestampUtil;
import org.teiid.query.function.FunctionMethods;

@SuppressWarnings("nls")
public class TestFunctionMethods {
	
	@Test public void testUnescape() {
		assertEquals("a\t\n\n%6", FunctionMethods.unescape("a\\t\\n\\012\\456"));
	}
	
	@Test public void testUnescape1() {
		assertEquals("a\u45AA'", FunctionMethods.unescape("a\\u45Aa\'"));
	}
	
	@Test public void testTimestampDiffTimeStamp_ErrorUsingEndDate2304() throws Exception {
		assertEquals(Long.valueOf(106752), FunctionMethods.timestampDiff(NonReserved.SQL_TSI_DAY, 
				new Timestamp(TimestampUtil.createDate(112, 0, 1).getTime()),
				new Timestamp(TimestampUtil.createDate(404, 3, 13).getTime())));
	}

}
