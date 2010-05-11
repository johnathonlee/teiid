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

package com.metamatrix.cdk.unittest;

import org.teiid.query.unittest.FakeMetadataFactory;

import com.metamatrix.cdk.api.TranslationUtility;

public class FakeTranslationFactory {
	
	private static FakeTranslationFactory instance = new FakeTranslationFactory();
	
	public static FakeTranslationFactory getInstance() {
		return instance;
	}

	public TranslationUtility getBQTTranslationUtility() {
		return new TranslationUtility(FakeMetadataFactory.exampleBQTCached());
	}
	
	public TranslationUtility getYahooTranslationUtility() {
		return new TranslationUtility(FakeMetadataFactory.exampleYahoo());
	}
	
	public TranslationUtility getExampleTranslationUtility() {
		return new TranslationUtility(FakeMetadataFactory.example1Cached());
	}
	
}
