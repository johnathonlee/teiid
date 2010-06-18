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

package org.teiid.core.types;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Reader;
import java.nio.charset.Charset;
import java.sql.SQLException;

import org.teiid.core.types.InputStreamFactory.StreamFactoryReference;
import org.teiid.core.util.InputStreamReader;

public class BaseLob implements Externalizable, StreamFactoryReference {
	
	private static final long serialVersionUID = -1586959324208959519L;
	private InputStreamFactory streamFactory;
	private Charset charset;
	
	public BaseLob() {
		
	}
	
	protected BaseLob(InputStreamFactory streamFactory) {
		this.streamFactory = streamFactory;
	}
	
	public void setStreamFactory(InputStreamFactory streamFactory) {
		this.streamFactory = streamFactory;
	}

	public InputStreamFactory getStreamFactory() throws SQLException {
		if (this.streamFactory == null) {
    		throw new SQLException("Already freed"); //$NON-NLS-1$
    	}
		return streamFactory;
	}
	
	public void setEncoding(String encoding) {
		if (encoding != null) {
			this.charset = Charset.forName(encoding);
		} else {
			this.charset = null;
		}
	}
	
	public Charset getCharset() {
		return charset;
	}
	
	public void setCharset(Charset charset) {
		this.charset = charset;
	}
	
	public void free() throws SQLException {
		if (this.streamFactory != null) {
			try {
				this.streamFactory.free();
				this.streamFactory = null;
			} catch (IOException e) {
				SQLException ex = new SQLException(e.getMessage());
				ex.initCause(e);
				throw ex;
			}
		}
	}
	
    public Reader getCharacterStream() throws SQLException {
    	try {
			Reader r = this.getStreamFactory().getCharacterStream();
			if (r != null) {
				return r;
			}
		} catch (IOException e) {
			SQLException ex = new SQLException(e.getMessage());
			ex.initCause(e);
			throw ex;
		}
		Charset cs = getCharset();
		if (cs == null) {
			cs = Streamable.CHARSET;
		}
		return new InputStreamReader(getBinaryStream(), cs.newDecoder());
    }

    public InputStream getBinaryStream() throws SQLException {
    	try {
			return this.getStreamFactory().getInputStream();
		} catch (IOException e) {
			SQLException ex = new SQLException(e.getMessage());
			ex.initCause(e);
			throw ex;
		}
    }
    
    @Override
    public void readExternal(ObjectInput in) throws IOException,
    		ClassNotFoundException {
    	streamFactory = (InputStreamFactory)in.readObject();
    }
    
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
    	out.writeObject(streamFactory);
    }

}
