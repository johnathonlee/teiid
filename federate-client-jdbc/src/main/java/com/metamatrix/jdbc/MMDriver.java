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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.metamatrix.api.exception.security.LogonException;
import com.metamatrix.common.api.MMURL;
import com.metamatrix.common.comm.api.ServerConnection;
import com.metamatrix.common.comm.exception.CommunicationException;
import com.metamatrix.common.comm.exception.ConnectionException;
import com.metamatrix.common.util.ApplicationInfo;
import com.metamatrix.common.util.PropertiesUtils;
import com.metamatrix.core.MetaMatrixCoreException;
import com.metamatrix.core.log.MessageLevel;
import com.metamatrix.jdbc.api.ConnectionProperties;
import com.metamatrix.jdbc.transport.MultiTransportFactory;
import com.metamatrix.jdbc.util.MMJDBCURL;

/**
 * <p> The java.sql.DriverManager class uses this class to connect to MetaMatrix.
 * The Driver Manager maintains a pool of MMDriver objects, which it could use
 * to connect to MetaMatrix. The MMDriver class has a static initializer, which
 * is used to instantiate and register itsef with java.sql.DriverManager. The
 * DriverManager's <code>getConnection</code> method calls <code>connect</code>
 * method on available registered drivers. The first driver to recognise the given
 * url is used to obtain a connection.</p>
 */

public final class MMDriver extends BaseDriver {

    static final String JDBC = BaseDataSource.JDBC;
    static final String URL_PREFIX = JDBC + BaseDataSource.METAMATRIX_PROTOCOL; 
    static final int MAJOR_VERSION = 5;
    static final int MINOR_VERSION = 5;
    static final String DRIVER_NAME = "MetaMatrix JDBC Driver"; //$NON-NLS-1$
    /**
     *  Suports JDBC URLS of format
     *  - jdbc:metamatrix:BQT@mm://localhost:####;version=1
     *  - jdbc:metamatrix:BQT@mms://localhost:####;version=1
     *  - jdbc:metamatrix:BQT@mm(s)://host1:####,host2:####,host3:####;version=1
     */
    
    // This host/port pattern allows just a . or a - to be in the host part.
    static final String HOST_PORT_PATTERN = "[\\p{Alnum}\\.\\-]+:\\d+"; //$NON-NLS-1$
    static final String URL_PATTERN = "jdbc:metamatrix:(\\w+)@((mm[s]?://"+HOST_PORT_PATTERN+"(,"+HOST_PORT_PATTERN+")*)[;]?){1}((.*)*)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    static Pattern urlPattern = Pattern.compile(URL_PATTERN);
    
    static MultiTransportFactory CONNECTION_FACTORY = new MultiTransportFactory();

    private static MMDriver INSTANCE = new MMDriver();
        
    // Static initializer
    static {

        try {
            ApplicationInfo info = ApplicationInfo.getInstance();
            info.setMainComponent("metamatrix-jdbc.jar"); //$NON-NLS-1$
            info.markUnmodifiable();
        } catch (Exception e) {
            String logMsg = JDBCPlugin.Util.getString("MMDriver.Err_init_appinfo", e.getMessage()); //$NON-NLS-1$
            DriverManager.println(logMsg);
        }
        
        try {
            DriverManager.registerDriver(INSTANCE);
        } catch(SQLException e) {
            // Logging
            String logMsg = JDBCPlugin.Util.getString("MMDriver.Err_registering", e.getMessage()); //$NON-NLS-1$
            DriverManager.println(logMsg);
        }
    }

    public static MMDriver getInstance() {
        return INSTANCE;
    }
    
    /**
     * Should be a singleton and only constructed in {@link #getInstance}.
     */
    public MMDriver() {
        // this is not singleton, if you want singleton make this private.
    }

    /**
     * This method tries to make a metamatrix connection to the given URL. This class
     * will return a null if this is not the right driver to connect to the given URL.
     * @param The URL used to establish a connection.
     * @return Connection object created
     * @throws SQLException if it is unable to establish a connection to the MetaMatrix server.
     */
    public Connection connect(String url, Properties info) throws SQLException {

        MMConnection myConnection = null;
        // create a properties obj if it is null
        if(info == null) {
            info = new Properties();
        } else {
            info = PropertiesUtils.clone(info);
        }

        // The url provided is in the correct format.
        if (!acceptsURL(url)) {
        	return null;
        }

        try {
            // parse the URL to add it's properties to properties object
            parseURL(url, info);

            myConnection = createMMConnection(url, info);
        } catch (MetaMatrixCoreException e) {
            DriverManager.println(e.getMessage());
            throw MMSQLException.create(e, e.getMessage());
            //throw new MMSQLException(e.getMessage(), e);
        }

        // logging
        String logMsg = JDBCPlugin.Util.getString("MMDriver.Connection_sucess"); //$NON-NLS-1$
        myConnection.getLogger().log(MessageLevel.INFO, logMsg);

        return myConnection;
    }

    MMServerConnection createMMConnection(String url, Properties info)
        throws ConnectionException, CommunicationException, LogonException {

        String transport = setupTransport(info);
        ServerConnection serverConn = CONNECTION_FACTORY.establishConnection(transport, info);

        // construct a MMConnection object.
        MMServerConnection connection = MMServerConnection.newInstance(serverConn, info, url);
        return connection;
    }

    static String setupTransport(Properties info) {
        // create a connection using the transport factory
        return MultiTransportFactory.SOCKET_TRANSPORT; 
    }

    /**
     * This method parses the URL and adds properties to the the properties object.
     * These include required and any optional properties specified in the URL.
     * Expected URL format -- jdbc:metamatrix:local:VDB@server:port;version=1;user=logon;
     * password=pw;logFile=<logFile.log>;
     * logLevel=<logLevel>;txnAutoWrap=<?>;credentials=mycredentials
     * @param The URL needed to be parsed.
     * @param The properties object which is to be updated with properties in the URL.
     * @throws SQLException if the URL is not in the expected format.
     */
    void parseURL(String url, Properties info) throws SQLException {
        if(url == null) {
            String msg = JDBCPlugin.Util.getString("MMDriver.urlFormat"); //$NON-NLS-1$
            throw new MMSQLException(msg);
        }
        try {
            MMJDBCURL jdbcURL = new MMJDBCURL(url);
            info.setProperty(BaseDataSource.VDB_NAME, jdbcURL.getVDBName());
            info.setProperty(MMURL.CONNECTION.SERVER_URL, jdbcURL.getConnectionURL());
            Properties optionalParams = jdbcURL.getProperties();
            MMJDBCURL.normalizeProperties(info);
            Enumeration keys = optionalParams.keys();
            while (keys.hasMoreElements()) {
                String propName = (String)keys.nextElement();
                // Don't let the URL properties override the passed-in Properties object.
                if (!info.containsKey(propName)) {
                    info.setProperty(propName, optionalParams.getProperty(propName));
                }
            }
            // add the property only if it is new because they could have
            // already been specified either through url or otherwise.
            if(! info.containsKey(BaseDataSource.VDB_VERSION) && jdbcURL.getVDBVersion() != null) {
                info.setProperty(BaseDataSource.VDB_VERSION, jdbcURL.getVDBVersion());
            }

            if(optionalParams.containsKey(BaseDataSource.LOG_FILE)) {
                String value = optionalParams.getProperty(BaseDataSource.LOG_FILE);
                if(value != null) {
                    try {
                        File f = new File(value);
                        boolean exists = f.exists(); 
                        FileWriter fw = new FileWriter(f, true);
                        fw.close();
                        if (!exists) {
                            f.delete();
                        }
                    } catch(IOException ioe) {
                        String msg = JDBCPlugin.Util.getString("MMDriver.Invalid_log_name", value); //$NON-NLS-1$
                        throw MMSQLException.create(ioe, msg);
                        //throw new MMSQLException(msg, ioe);
                    }
                }
            }
            if(optionalParams.containsKey(BaseDataSource.LOG_LEVEL)) {
                try {
                    int loglevel = Integer.parseInt(optionalParams.getProperty(BaseDataSource.LOG_LEVEL));
                    if(loglevel < BaseDataSource.LOG_NONE || loglevel > BaseDataSource.LOG_TRACE) {
                        Object[] params = new Object[] {new Integer(BaseDataSource.LOG_NONE), new Integer(BaseDataSource.LOG_ERROR), new Integer(BaseDataSource.LOG_INFO), new Integer(BaseDataSource.LOG_TRACE)};
                        String msg = JDBCPlugin.Util.getString("MMDriver.Log_level_invalid", params);  //$NON-NLS-1$
                        throw new MMSQLException(msg);
                    }
                } catch(NumberFormatException nfe) {
                    Object[] params = new Object[] {new Integer(BaseDataSource.LOG_NONE), new Integer(BaseDataSource.LOG_ERROR), new Integer(BaseDataSource.LOG_INFO), new Integer(BaseDataSource.LOG_TRACE)};
                    String msg = JDBCPlugin.Util.getString("MMDriver.Log_level_invalid", params);  //$NON-NLS-1$
                    throw MMSQLException.create(nfe, msg);
                    //throw new MMSQLException(msg, nfe);
                }
            }
        } catch(IllegalArgumentException iae) {
            String msg = JDBCPlugin.Util.getString("MMDriver.urlFormat"); //$NON-NLS-1$
            throw new MMSQLException(msg);
        }  
    }
    
    /**
     * Returns true if the driver thinks that it can open a connection to the given URL.
     * Typically drivers will return true if they understand the subprotocol specified
     * in the URL and false if they don't.
     * Expected URL format is
     * jdbc:metamatrix:subprotocol:VDB@server:port;version=1;logFile=<logFile.log>;logLevel=<logLevel>;txnAutoWrap=<?>
     * @param The URL used to establish a connection.
     * @return A boolean value indicating whether the driver understands the subprotocol.
     * @throws SQLException, should never occur
     */
    public boolean acceptsURL(String url) throws SQLException {
        Matcher m = urlPattern.matcher(url);
        return m.matches();
    }

    /**
     * Get's the driver's major version number. Initially this should be 1.
     * @return major version number of the driver.
     */
    public int getMajorVersion() {
        return MAJOR_VERSION;
    }

    /**
     * Get's the driver's minor version number. Initially this should be 0.
     * @return major version number of the driver.
     */
    public int getMinorVersion() {
        return MINOR_VERSION;
    }

    /** 
     * @see com.metamatrix.jdbc.BaseDriver#getDriverName()
     * @since 4.3
     */
    public String getDriverName() {
        return DRIVER_NAME;
    }
    
    @Override
    List<DriverPropertyInfo> getAdditionalPropertyInfo(String url,
    		Properties info) {
    	List<DriverPropertyInfo> dpis = new LinkedList<DriverPropertyInfo>();
        DriverPropertyInfo dpi = new DriverPropertyInfo(MMURL.CONNECTION.SERVER_URL, info.getProperty(MMURL.CONNECTION.SERVER_URL));
        dpi.required = true;
        dpis.add(dpi);
        dpis.add(new DriverPropertyInfo(BaseDataSource.USER_NAME, info.getProperty(BaseDataSource.USER_NAME)));
        dpis.add(new DriverPropertyInfo(BaseDataSource.PASSWORD, info.getProperty(BaseDataSource.PASSWORD)));
        dpis.add(new DriverPropertyInfo(ConnectionProperties.PROP_CLIENT_SESSION_PAYLOAD, info.getProperty(BaseDataSource.PASSWORD)));
        return dpis;
    }
    
}

