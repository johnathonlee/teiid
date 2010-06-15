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

/**
 * 
 */
package org.teiid.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import org.teiid.core.types.DataTypeManager;

public class ReaderInputStream extends InputStream {
	
	private static final int DEFAULT_BUFFER_SIZE = DataTypeManager.MAX_LOB_MEMORY_BYTES;
	
	private final Reader reader;
	private final Charset charSet;
	private char[] charBuffer;
	
	private boolean hasMore = true;
	private ByteBuffer currentBuffer;
	private int prefixBytes;
	private boolean needsPrefix = true;
	
	public ReaderInputStream(Reader reader, Charset charSet) {
		this(reader, charSet, DEFAULT_BUFFER_SIZE);
	}

	public ReaderInputStream(Reader reader, Charset charSet, int bufferSize) {
		this.reader = reader;
		this.charSet = charSet;
		this.charBuffer = new char[bufferSize];
		if (charSet.displayName().equalsIgnoreCase("UTF-16")) { //$NON-NLS-1$
			prefixBytes = 2;
		}
	}

	@Override
	public int read() throws IOException {
		if (currentBuffer == null || !currentBuffer.hasRemaining()) {
			if (!hasMore) {
				return -1;
			}
			int charsRead = reader.read(charBuffer);
			if (charsRead == -1) {
	            hasMore = false;
				return -1;
			}
			currentBuffer = charSet.encode(CharBuffer.wrap(charBuffer, 0, charsRead));
			if (!needsPrefix) {
				currentBuffer.position(prefixBytes);
			}
			needsPrefix = false;
		}
		return currentBuffer.get() & 0xff;
	}
	
	@Override
	public void close() throws IOException {
		this.reader.close();
	}
}