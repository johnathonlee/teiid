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
package org.teiid.transport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Properties;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.teiid.logging.LogConstants;
import org.teiid.logging.LogManager;
import org.teiid.net.socket.ServiceInvocationStruct;
import org.teiid.odbc.ODBCServerRemote;

/**
 * Represents the messages going from PG ODBC Client --> back end Server  
 * Some parts of this code is taken from H2's implementation of ODBC
 */
@SuppressWarnings("nls")
public class PgFrontendProtocol extends FrameDecoder {

	private int maxObjectSize;
	private Byte messageType;
	private Integer dataLength;
	private boolean initialized = false;
	private String encoding = "UTF-8"; // client can override this
	private ODBCServerRemote odbcProxy;
	private ServiceInvocationStruct message;
	private String user;
	private String databaseName;
	
	public PgFrontendProtocol(int maxObjectSize) {
        
		if (maxObjectSize <= 0) {
            throw new IllegalArgumentException("maxObjectSize: " + maxObjectSize); //$NON-NLS-1$
        }
		
		if (encoding == null) {
			this.encoding = "UTF-8"; //$NON-NLS-1$ 
		}
        		
		this.maxObjectSize = maxObjectSize;
		
		// the proxy is used for generating the object based message based on ServiceInvocationStruct class.
		this.odbcProxy = (ODBCServerRemote)Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {ODBCServerRemote.class}, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				message = new ServiceInvocationStruct(args, method.getName(),ODBCServerRemote.class);
				return null;
			}
		});
	}
	
	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
		
        if (this.initialized && this.messageType == null) {
	        if (buffer.readableBytes() < 1 ) {
	            return null;
	        }
	
	        this.messageType = buffer.readByte();
	        if (this.messageType < 0 ) {
	        	this.odbcProxy.terminate();
	        	return message;
	        }
        }
        
        if (!this.initialized) {
        	this.messageType = 'I';
        }
        
        if (this.dataLength == null) {
	        if (buffer.readableBytes() < 4) {
	            return null;
	        }
	                
	        this.dataLength = buffer.readInt();
	        if (this.dataLength <= 0) {
	            throw new StreamCorruptedException("invalid data length: " + this.dataLength); //$NON-NLS-1$
	        }
	        if (this.dataLength > this.maxObjectSize) {
	            throw new StreamCorruptedException("data length too big: " + this.dataLength + " (max: " + this.maxObjectSize + ')'); //$NON-NLS-1$ //$NON-NLS-2$
	        }
        }

        if (buffer.readableBytes() < this.dataLength - 4) {
            return null;
        }

        byte[] data = createByteArray(this.dataLength - 4);
        buffer.readBytes(data);      
		Object message =  createRequestMessage(this.messageType, new NullTerminatedStringDataInputStream(new DataInputStream(new ByteArrayInputStream(data, 0, this.dataLength-4)), this.encoding));
		this.dataLength = null;
		this.messageType = null;
		return message;
	}

	private Object createRequestMessage(byte messageType, NullTerminatedStringDataInputStream data) throws IOException{
        switch(messageType) {
        case 'I': 
        	this.initialized = true;
        	return buildInitialize(data);
        case 'p':
        	return buildLogin(data);
        case 'P':
        	return buildParse(data);
        case 'B':
        	return buildBind(data);
        case 'E':
        	return buildExecute(data);
        case 'Q':
        	return buildExecuteQuery(data);
        case 'D':
        	return buildDescribe(data);
        case 'X':
        	return buildTeminate();
        case 'S':
        	return buildSync();
        case 'C':
        	return buildClose(data);
        case 'H':
        	return buildFlush();        	       
        default:
        	return buildError();
        }
	}

	private Object buildError() {
		trace("error");
		this.odbcProxy.unsupportedOperation("option not suported");
		return message;
	}

	private Object buildFlush() {
		trace("flush");
		this.odbcProxy.flush();
		return message;
	}

	private Object buildTeminate() {
		trace("terminate");
		this.odbcProxy.terminate();
		return message;
	}

	private Object buildInitialize(NullTerminatedStringDataInputStream data) throws IOException{
        trace("Init");
        Properties props = new Properties();
       
        int version = data.readInt();
        props.setProperty("version", Integer.toString(version));
        
        trace("StartupMessage");
        trace(" version " + version + " (" + (version >> 16) + "." + (version & 0xff) + ")");
        
        while (true) {
            String param =  data.readString();
            if (param.length() == 0) {
                break;
            }
            String value =  data.readString();
            props.setProperty(param, value);
            trace(" param " + param + "=" + value);
        }        
        this.user = props.getProperty("user");
        this.databaseName = props.getProperty("database");
        this.encoding = props.getProperty("client_encoding", "UTF-8");
        this.odbcProxy.initialize(props);
        return message;
	}
	
	private Object buildLogin(NullTerminatedStringDataInputStream data) throws IOException{
        trace("PasswordMessage");
        String password = data.readString();
        this.odbcProxy.logon(this.databaseName, this.user, password);
        return message;
	}	

	private Object buildParse(NullTerminatedStringDataInputStream data) throws IOException {
        trace("Parse");
        String name = data.readString();
        String sql = data.readString();
        
        //The number of parameter data types specified (can be zero). Note that this is not 
        //an indication of the number of parameters that might appear in the query string, only 
        //the number that the frontend wants to prespecify types for.
        int count = data.readShort();
        int[] paramType = new int[count];
        for (int i = 0; i < count; i++) {
            int type = data.readInt();
            paramType[i] = type;
        }
        this.odbcProxy.prepare(name, sql, paramType);
        return message;
	}	

	private Object buildBind(NullTerminatedStringDataInputStream data) throws IOException {
        trace("Bind");
        String bindName = data.readString();
        String prepName = data.readString();
        
        int formatCodeCount = data.readShort();
        int[] formatCodes = new int[formatCodeCount];
        for (int i = 0; i < formatCodeCount; i++) {
            formatCodes[i] = data.readShort();
        }
        
        int paramCount = data.readShort();
        Object[] params = new String[paramCount];
        for (int i = 0; i < paramCount; i++) {
            int paramLen = data.readInt();
            byte[] paramdata = createByteArray(paramLen);
            data.readFully(paramdata);
            
            // the params can be either text or binary
            if (formatCodeCount == 0 || (formatCodeCount == 1 && formatCodes[0] == 0) || formatCodes[i] == 0) {
            	params[i] = new String(paramdata, this.encoding);
            }
            else {
            	params[i] = paramdata;
            }
        }
        
        int resultCodeCount = data.readShort();
        int[] resultColumnFormat = new int[resultCodeCount];
        for (int i = 0; i < resultCodeCount; i++) {
            resultColumnFormat[i] = data.readShort();
        }
        this.odbcProxy.bindParameters(bindName, prepName, paramCount, params, resultCodeCount, resultColumnFormat);
        return message;
	}	

	private Object buildExecute(NullTerminatedStringDataInputStream data) throws IOException {
		String portalName = data.readString();
		trace("Execute "+ portalName);
        int maxRows = data.readShort();
        this.odbcProxy.execute(portalName, maxRows);
        return message;
	}	


	private Object buildDescribe(NullTerminatedStringDataInputStream data)  throws IOException{
        char type = (char) data.readByte();
        String name = data.readString();
        trace("Describe");
        if (type == 'S') {
        	this.odbcProxy.getParameterDescription(name);
        	return message;
        } else if (type == 'P') {
        	this.odbcProxy.getResultSetMetaDataDescription(name);
        	return message;
        } else {
            trace("expected S or P, got " + type);
            this.odbcProxy.unsupportedOperation("expected S or P");
            return message;
        }
	}
	

	private Object buildSync() {
		trace("sync");
		this.odbcProxy.sync();
		return message;
	}	

	private Object buildExecuteQuery(NullTerminatedStringDataInputStream data) throws IOException {
        String query = data.readString();
        trace("Query:"+query);
        this.odbcProxy.executeQuery(query);
        return message;
	}	
	
	private byte[] createByteArray(int length) throws StreamCorruptedException{
		try {
			return new byte[length];
		} catch(OutOfMemoryError e) {
			throw new StreamCorruptedException("data too big: " + e.getMessage()); //$NON-NLS-1$ 
		}
	}
	
	private Object buildClose(NullTerminatedStringDataInputStream data) throws IOException {
		char type = (char)data.read();
		String name = data.readString();
		if (type == 'S') {
			this.odbcProxy.closePreparedStatement(name);
		}
		else if (type == 'P') {
			this.odbcProxy.closeBoundStatement(name);
		}
		else {
			this.odbcProxy.unsupportedOperation("unknown close type specified");
		}
		return message;
	}	
	
	static class NullTerminatedStringDataInputStream extends DataInputStream{
		private String encoding;
		
		public NullTerminatedStringDataInputStream(DataInputStream in, String encoding) {
			super(in);
			this.encoding = encoding;
		}

	    public String readString() throws IOException {
	        ByteArrayOutputStream buff = new ByteArrayOutputStream();
	        while (true) {
	            int x = read();
	            if (x <= 0) {
	                break;
	            }
	            buff.write(x);
	        }
	        return new String(buff.toByteArray(), this.encoding);
	    }
	}
	
	private static void trace(String msg) {
		LogManager.logTrace(LogConstants.CTX_ODBC, msg);
	}
}
