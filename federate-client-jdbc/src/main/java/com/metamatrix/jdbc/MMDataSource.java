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

package com.metamatrix.jdbc;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import com.metamatrix.common.api.MMURL;
import com.metamatrix.common.api.MMURL_Properties;
import com.metamatrix.core.MetaMatrixCoreException;
import com.metamatrix.core.log.Logger;
import com.metamatrix.core.log.MessageLevel;
import com.metamatrix.core.log.NullLogger;
import com.metamatrix.jdbc.api.ConnectionProperties;
import com.metamatrix.jdbc.util.MMJDBCURL;

/**
 * The MetaMatrix JDBC DataSource implementation class of {@link javax.sql.DataSource} and
 * {@link javax.sql.XADataSource}.
 * <p>
 * The {@link javax.sql.DataSource} interface follows the JavaBean design pattern,
 * meaning the implementation class has <i>properties</i> that are accessed with getter methods
 * and set using setter methods, and where the getter and setter methods follow the JavaBean
 * naming convention (e.g., <code>get</code><i>PropertyName</i><code>() : </code><i>PropertyType</i>
 * and <code>set</code><i>PropertyName</i><code>(</code><i>PropertyType</i><code>) : void</code>).
 * </p>
 * The {@link javax.sql.XADataSource} interface is almost identical to the {@link javax.sql.DataSource}
 * interface, but rather than returning {@link java.sql.Connection} instances, there are methods that
 * return {@link javax.sql.XAConnection} instances that can be used with distributed transactions.
 * <p>
 * The following are the properties for this DataSource:
 * <table cellspacing="0" cellpadding="0" border="1" width="100%">
 *   <tr><td><b>Property Name</b></td><td><b>Type</b></td><td><b>Description</b></td></tr>
 *   <tr><td>portNumber       </td><td><code>int   </code></td><td>The port number where a MetaMatrix Server is listening
 *                                                                 for requests.</td></tr>
 *   <tr><td>serverName       </td><td><code>String</code></td><td>The hostname or IP address of the MetaMatrix Server.</td></tr>
 * <table>
 * </p>
 */
public class MMDataSource extends BaseDataSource implements javax.sql.XADataSource {

    
    static{
        try {
            Class.forName("com.metamatrix.jdbc.MMDriver"); //$NON-NLS-1$
        } catch(ClassNotFoundException e) {
            String msg = JDBCPlugin.Util.getString("MMDataSource.Cant_load_driver"); //$NON-NLS-1$
            DriverManager.println(msg);
        }
    }

    /**
     * The port number where a MetaMatrix Server is listening for requests.
     * This property name is one of the standard property names defined by the JDBC 2.0 specification,
     * and is <i>optional</i>.
     */
    private int portNumber;

    /**
     * The name of the host where the MetaMatrix Server is running.
     * This property name is one of the standard property names defined by the JDBC 2.0 specification,
     * and is <i>required</i>.
     */
    private String serverName;
     
    /**
     * Specify a set of data source credentials to pass to the connectors as defined in 
     * {@link com.metamatrix.jdbc.api.ConnectionProperties#PROP_CREDENTIALS}.  This
     * property will be parsed and used to create a {@link com.metamatrix.data.pool.CredentialMap}
     * which will be set as the session token.
     */
    private String credentials; 
    
    /**
     * Specify whether to make a secure (SSL, mms:) connection or a normal non-SSL mm: connection.
     * the default is to use a non-secure connection.
     * @since 5.0.2
     */
    private boolean secure = false;

    /**
     * Holds a comma delimited list of alternate MetaMatrix Server(s):Port(s) that can 
     * be used for connection fail-over.
     * @since 5.5
     */
    private String alternateServers = null;
    
    private String autoFailover;
    
    private transient Logger logger;

    /**
     * Constructor for MMDataSource.
     */
    public MMDataSource() {
    }

    // --------------------------------------------------------------------------------------------
    //                             H E L P E R   M E T H O D S
    // --------------------------------------------------------------------------------------------

    protected Properties buildProperties(final String userName, final String password) {               
        Properties props = super.buildProperties(userName, password);
        
        props.setProperty(MMURL_Properties.SERVER.SERVER_URL,this.buildServerURL());
        
        if (this.getAutoFailover() != null) {
            props.setProperty(CONNECTION.AUTO_FAILOVER, this.getAutoFailover());
        }
        
        if (this.getCredentials() != null) {
            props.setProperty(ConnectionProperties.PROP_CREDENTIALS, this.getCredentials());
        }

        return props;
    }

    protected String buildServerURL() {
    	if ( this.alternateServers == null ) {
    		// Format:  "mm://server:port"
    		return new MMURL(this.serverName, this.portNumber, this.secure).getAppServerURL();
    	} 

    	// Format: "mm://server1:port,server2:port,..."
		String serverURL = ""; //$NON-NLS-1$
		
		serverURL = "" + ( this.secure ? MMURL.SECURE_PROTOCOL : MMURL.DEFAULT_PROTOCOL ); //$NON-NLS-1$
		serverURL += "" + this.serverName; //$NON-NLS-1$
		if ( this.portNumber != 0 ) 
			serverURL += MMURL.COLON_DELIMITER  + this.portNumber;
		if ( this.alternateServers.length() > 0 ) {
        	String[] as = this.alternateServers.split( MMURL.COMMA_DELIMITER);
        	
        	for ( int i = 0; i < as.length; i++ ) {
        		String[] server = as[i].split( MMURL.COLON_DELIMITER );

        		if ( server.length > 0 ) {
        			serverURL += MMURL.COMMA_DELIMITER + server[0];
        			if ( server.length > 1 ) {
        				serverURL += MMURL.COLON_DELIMITER + server[1];
        			} else {
        				serverURL += MMURL.COLON_DELIMITER + this.portNumber;
        			}
        		}
        	}
		}
		
		return new MMURL(serverURL).getAppServerURL();
    }

    protected String buildURL() {
        return new MMJDBCURL(this.getDatabaseName(), buildServerURL(), buildProperties(getUser(), getPassword())).getJDBCURL();
    }

    protected void validateProperties( final String userName, final String password) throws java.sql.SQLException {
        super.validateProperties(userName, password);
        
        String reason = reasonWhyInvalidPortNumber(this.portNumber);
        if ( reason != null ) {
            throw createConnectionError(reason);
        }

        reason = reasonWhyInvalidServerName(this.serverName);
        if ( reason != null ) {
            throw createConnectionError(reason);
        }

        reason = reasonWhyInvalidAlternateServers(this.alternateServers);
        if ( reason != null) {
        	throw createConnectionError(reason);
        }
    }
    
    private MMSQLException createConnectionError(String reason) {
        String msg = JDBCPlugin.Util.getString("MMDataSource.Err_connecting", reason); //$NON-NLS-1$
        return new MMSQLException(msg);        
    }

    // --------------------------------------------------------------------------------------------
    //                        D A T A S O U R C E   M E T H O D S
    // --------------------------------------------------------------------------------------------

    /**
     * Attempt to establish a database connection.
     * @return a Connection to the database
     * @throws java.sql.SQLException if a database-access error occurs
     * @see javax.sql.DataSource#getConnection()
     */
    public Connection getConnection() throws java.sql.SQLException {
        return getConnection(null,null);
    }

    /**
     * Attempt to establish a database connection.
     * @param userName the database user on whose behalf the Connection is being made
     * @param password the user's password
     * @return a Connection to the database
     * @throws java.sql.SQLException if a database-access error occurs
     * @see javax.sql.DataSource#getConnection(java.lang.String, java.lang.String)
     */
    public Connection getConnection(String userName, String password) throws java.sql.SQLException {
        try {
            validateProperties(userName,password);
            final Properties props = buildProperties(userName, password);
            final MMDriver driver = new MMDriver();
            return driver.createMMConnection(buildURL(), props);
        } catch (MetaMatrixCoreException e) {
            getLogger().log(MessageLevel.CRITICAL, e, e.getMessage());
            throw MMSQLException.create(e, e.getMessage());
        }
    }

   /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return buildURL().substring(16);    // URL without the "jdbc:metamatrix:" at the front 
    }

    // --------------------------------------------------------------------------------------------
    //                        P R O P E R T Y   M E T H O D S
    // --------------------------------------------------------------------------------------------

    /**
     * Returns the port number.
     * @return the port number
     */
    public int getPortNumber() {
        return portNumber;
    }

    /**
     * Returns the name of the MetaMatrix Server.
     * @return the name of the MetaMatrix Server
     */
    public String getServerName() {
        return serverName;
    }
    
    /**
     * Returns the credentials string defining credentials to use with connectors for per-user logon.
     * @since 4.3.2
     */
    public String getCredentials() {
        return credentials;
    }
    
    /**
     * Returns a flag indicating whether to create a secure connection or not. 
     * @return True if using secure mms: protocol, false for normal mm: protocol.
     * @since 5.0.2
     */
    public boolean isSecure() {
        return this.secure;
    }

    /**
     * Returns a string containing a comma delimited list of alternate 
     * MetaMatrix Server(s).  
     * 
     * The list will be in the form of server2[:port2][,server3[:port3]].  If no 
     * alternate servers have been defined <code>null</code> is returned. 
     * @return A comma delimited list of server:port or <code>null</code> If 
     * no alternate servers are defined.
     * @since 5.5
     */
    public String getAlternateServers() {
    	if ( this.alternateServers != null && this.alternateServers.length() < 1 )
    		return null;
        return this.alternateServers;
    }

    /**
     * Sets the portNumber.
     * @param portNumber The portNumber to set
     */
    public void setPortNumber(final int portNumber) {
        this.portNumber = portNumber;
    }

    /**
     * Sets the serverName.
     * @param serverName The serverName to set
     */
    public void setServerName(final String serverName) {
        this.serverName = serverName;
    }
    
    /**
     * Sets the credentials string defining credentials to use with connectors for per-user logon.
     * @since 4.3.2
     */
    public void setCredentials(final String credentials) {
        this.credentials = credentials;
    }
    
    /**
     * Sets the secure flag to use mms: protocol instead of the default mm: protocol. 
     * @param secure True to use mms:
     * @since 5.0.2
     */
    public void setSecure(final boolean secure) {
        this.secure = secure;
    }
    
    /**
     * Sets a list of alternate MetaMatrix Sserver(s) that can be used for 
     * connection fail-over.
     * 
     * The form of the list should be server2[:port2][,server3:[port3][,...]].  
     * 
     * If ":port" is omitted, the port defined by <code>portNumber</code> is used.
     * 
     * If <code>servers</code> is empty or <code>null</code>, the value of
     * <code>alternateServers</code> is cleared.
     * @param servers A comma delimited list of alternate MetaMatrix 
     * Server(s):Port(s) to use for connection fail-over. If blank or 
     * <code>null</code>, the list is cleared.
     * @since 5.5
     */
    public void setAlternateServers(final String servers) {
    	this.alternateServers = servers;
    	if ( this.alternateServers != null && this.alternateServers.length() < 1 )
    		this.alternateServers = null;
    }
    
    
    // --------------------------------------------------------------------------------------------
    //                  V A L I D A T I O N   M E T H O D S
    // --------------------------------------------------------------------------------------------

    /**
     * Return the reason why the supplied port number may be invalid, or null
     * if it is considered valid.
     * @param portNumber a possible value for the property
     * @return the reason why the property is invalid, or null if it is considered valid
     * @see #setPortNumber(int)
     */
    public static String reasonWhyInvalidPortNumber( final int portNumber) {
        if ( portNumber == 0 ) {
            return null;        // default is always fine
        }
        if ( portNumber < 1 ) {
            return JDBCPlugin.Util.getString("MMDataSource.Port_number_must_be_positive"); //$NON-NLS-1$
        }
        return null;
    }

    /**
     * Return the reason why the supplied server name may be invalid, or null
     * if it is considered valid.
     * @param serverName a possible value for the property
     * @return the reason why the property is invalid, or null if it is considered valid
     * @see #setServerName(String)
     * */
    public static String reasonWhyInvalidServerName( final String serverName ) {
        if ( serverName == null || serverName.trim().length() == 0 ) {
            return JDBCPlugin.Util.getString("MMDataSource.Server_name_required"); //$NON-NLS-1$
        }
        return null;
    }

    /**
     * The reason why "socketsPerVM" is invalid.
     * @param value of "socketsPerVM" property
     * @return reason
     */
    public static String reasonWhyInvalidSocketsPerVM(final String socketsPerVM) {
        if (socketsPerVM != null) {
            int value = -1;
            try {
                value = Integer.parseInt(socketsPerVM);
            } catch (Exception e) {                
            }
            
            if (value <= 0) {
                return JDBCPlugin.Util.getString("MMDataSource.Sockets_per_vm_invalid"); //$NON-NLS-1$
            }
        }
        return null;
    }
    
    
    /**
     * The reason why "stickyConnections" is invalid.
     * @param value of "stickyConnections" property
     * @return reason
     */
    public static String reasonWhyInvalidStickyConnections(final String stickyConnections) {
        if (stickyConnections != null) {
            if ((! stickyConnections.equalsIgnoreCase("true")) &&    //$NON-NLS-1$ 
                (! stickyConnections.equalsIgnoreCase("false"))) {   //$NON-NLS-1$          
                return JDBCPlugin.Util.getString("MMDataSource.Sticky_connections_invalid"); //$NON-NLS-1$
            }
        }
        return null;
    }
 
    /**
     * The reason why "alternateServers" is invalid.
     * @param value of "alternateServers" property
     * @return reason
     */
    public static String reasonWhyInvalidAlternateServers(final String alternateServers) {
    	if ( alternateServers == null || alternateServers.trim().length() < 1 )
    		return null;
    	
    	String[] as = alternateServers.split( MMURL.COMMA_DELIMITER);
    	String sReason = null;
    	String reason = ""; //$NON-NLS-1$
    	int reasonCount = 0;
    	final String newline = System.getProperty("line.separator"); //$NON-NLS-1$
    	
    	for ( int i = 0; i < as.length; i++ ) {
    		String[] server = as[i].split( MMURL.COLON_DELIMITER );

    		if ( server.length < 1 || server.length > 2 ) {
    			// ie "server:31000:an invalid value"
    			// ie "server,server:31000"
				return JDBCPlugin.Util.getString("MMDataSource.Alternate_Servers_format"); //$NON-NLS-1$
    		}

    		// check the server name portion
    		sReason = reasonWhyInvalidServerName(server[0] );
    		if ( sReason != null ) {
   				reason += (reason.length() > 0 ? newline : "" ) + sReason; //$NON-NLS-1$
   				reasonCount++;
   				sReason = null;
    		}
    		
    		if ( server.length > 1 ) {
				// check the port portion
				int port = 0;
				// parse the int from the string
				try { port = Integer.parseInt(server[1]); }
				catch ( NumberFormatException e ) { 
	    			// ie "server:invalid_port"
	   				reason += (reason.length() > 0 ? newline : "" )  //$NON-NLS-1$
						+ JDBCPlugin.Util.getString("MMDataSource.serverPort_must_be_a_number"); //$NON-NLS-1$
					reasonCount++;
				}
				sReason = reasonWhyInvalidPortNumber(port);
				if ( sReason != null ) {
	   				reason += (reason.length() > 0 ? newline : "" ) + sReason; //$NON-NLS-1$
	   				reasonCount++;
	   				sReason = null;
				}
    		}
    	}
    	if ( reasonCount < 1 ) return null;
    	return JDBCPlugin.Util.getString("MMDataSource.alternateServer_is_invalid", new String[] { "" + reasonCount, reason }); //$NON-NLS-1$ //$NON-NLS-2$
    }
 
 
    /**
     * JDBC Logger; we create a logger once we create a connection, but if we
     * fail to make connection we need a default logger based on the set paramters
     * wish we have one place to do this. 
     * @return
     */
    public Logger getLogger() {
        if (this.logger == null) {
            try {
                if (getLogFile() != null) {
                    this.logger = new DriverManagerLogger(getLogLevel(), new PrintWriter(new FileWriter(getLogFile())));
                }
                else {
                    this.logger = new DriverManagerLogger(getLogLevel(), getLogWriter());
                }
            } catch (Exception e) {
                this.logger = new NullLogger();
            }
        }
        return this.logger;
    }

    
    /** 
     * @return Returns the transparentFailover.
     */
    public String getAutoFailover() {
        return this.autoFailover;
    }

    /** 
     * @param transparentFailover The transparentFailover to set.
     */
    public void setAutoFailover(String autoFailover) {
        this.autoFailover = autoFailover;
    }

}

