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

package com.metamatrix.common.log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;

import com.metamatrix.common.CommonPlugin;
import com.metamatrix.common.config.JDBCConnectionPoolHelper;
import com.metamatrix.common.pooling.api.ResourceContainer;
import com.metamatrix.common.pooling.api.exception.ResourcePoolException;
import com.metamatrix.common.pooling.impl.BaseResource;
import com.metamatrix.common.util.ErrorMessageKeys;
import com.metamatrix.common.util.LogCommonConstants;
import com.metamatrix.core.log.LogMessage;
import com.metamatrix.core.util.DateUtil;
import com.metamatrix.core.util.StringUtil;

/**
 *  - flag for turning off logging after # times unsuccessful writes (2) or can't connect (1 retry)
 * -  timestamp when turned off - determine period to reset flags
 * -  on msg write - check flag or if period for resume retry
 * -  any messages sent during the down time will not written out by this logger
 * -  add System.err messages when restarting and stopping the logging
 * 
 */
public class DbLogWriter implements DbWriter {

    /**
     * Static String to use as the user name when checking out connections from the pool
     */
    static final String LOGGING = "LOGGING";//$NON-NLS-1$

	/**
	 * The name of the System property that contains the name of the LogMessageFormat
	 * class that is used to format messages sent to the file destination.
	 * This is an optional property; if not specified and the file destination
	 * is used, then the {@link com.metamatrix.common.logging.format.DelimitedLogMessageFormat DelimitedLogMessageFormat}
	 * is used.
	 */
	static final String PROPERTY_PREFIX    = "metamatrix.log."; //$NON-NLS-1$  


	/**
	 * The name of the property that contains the name
	 * JDBC database to which log messages are to be recorded.
	 * This is a required property that has no default.
	 */
	public static final String DATABASE_PROPERTY_NAME    = PROPERTY_PREFIX + "jdbcDatabase"; //$NON-NLS-1$

	/**
	 * The name of the property that contains the protocol that should
	 * be used to connect to the JDBC database to which log messages are to be recorded.
	 * This is a required property that has no default.
	 */
	public static final String PROTOCOL_PROPERTY_NAME    = PROPERTY_PREFIX + "jdbcProtocol"; //$NON-NLS-1$

	/**
	 * The name of the property that contains the JDBC driver of the
	 * JDBC database to which log messages are to be recorded.
	 * This is a required property that has no default.
	 */
	public static final String DRIVER_PROPERTY_NAME    = PROPERTY_PREFIX + "jdbcDriver"; //$NON-NLS-1$

	/**
	 * The name of the property that contains the principal of the
	 * JDBC database to which log messages are to be recorded.
	 * This is a required property that has no default.
	 */
	public static final String PRINCIPAL_PROPERTY_NAME    = PROPERTY_PREFIX + "jdbcUsername"; //$NON-NLS-1$

	/**
	 * The name of the property that contains the password of the
	 * JDBC database to which log messages are to be recorded.
	 * This is a required property that has no default.
	 */
	public static final String PASSWORD_PROPERTY_NAME    = PROPERTY_PREFIX + "jdbcPassword"; //$NON-NLS-1$

	/**
	 * The name of the property that contains the name of the table
	 * to which log messages are to be recorded.
	 * This is an optional property that defaults to "log".
	 */
	public static final String TABLE_PROPERTY_NAME    = PROPERTY_PREFIX + "jdbcTable"; //$NON-NLS-1$

	/**
	 * The name of the property that contains the maximum length allowed
	 * for the column that contains the message portion.
	 * This is an optional property that defaults to "2000"; if supplied
	 * value is 0 then the length is not checked for each message prior to insertion.
	 */
	public static final String MAX_MESSAGE_LENGTH_PROPERTY_NAME    = PROPERTY_PREFIX + "jdbcMaxMsgLength"; //$NON-NLS-1$

	/**
	 * The name of the property that contains the maximum length allowed
	 * for the general column (exception message and exception).
	 * This is an optional property that defaults to "64"; if supplied
	 * value is 0 then the length is not checked for each message prior to insertion.
	 */
	public static final String MAX_GENERAL_LENGTH_PROPERTY_NAME    = PROPERTY_PREFIX + "jdbcMaxContextLength"; //$NON-NLS-1$

	/**
	 * The name of the property that contains the name of the table
	 * to which log messages are to be recorded.
	 * This is an optional property that defaults to "4000"; if supplied
	 * value is 0 then the length is not checked for each message prior to insertion.
	 */
	public static final String MAX_EXCEPTION_LENGTH_PROPERTY_NAME    = PROPERTY_PREFIX + "jdbcMaxExceptionLength"; //$NON-NLS-1$

	public static final String DEFAULT_TABLE_NAME = "LOGENTRIES";  //$NON-NLS-1$
	public static final int DEFAULT_MAX_GENERAL_LENGTH = 64;
	public static final int DEFAULT_MAX_EXCEPTION_LENGTH = 4000;
	public static final int DEFAULT_MAX_MSG_LENGTH = 2000;
    
	
	public static final String PLUGIN_PREFIX = "com.metamatrix.";  //$NON-NLS-1$

	public static final class ColumnName {
		public static final String TIMESTAMP        = "TIMESTAMP"; //$NON-NLS-1$
		public static final String SEQUENCE_NUMBER  = "VMSEQNUM"; //$NON-NLS-1$
		public static final String CONTEXT          = "CONTEXT"; //$NON-NLS-1$
		public static final String LEVEL            = "MSGLEVEL"; //$NON-NLS-1$
		public static final String EXCEPTION        = "EXCEPTION"; //$NON-NLS-1$
		public static final String MESSAGE          = "MESSAGE"; //$NON-NLS-1$
		public static final String HOST             = "HOSTNAME"; //$NON-NLS-1$
		public static final String VM               = "VMID"; //$NON-NLS-1$
		public static final String THREAD           = "THREADNAME"; //$NON-NLS-1$
	}
    
    private static final int WAIT_TIME = 60 * 1000; // 60 secs or 1 min
    private static final int RETRY_TIME = 5 * 1000; // 5 sec
    
    private static final int WRITE_RETRIES = 5; // # of retries before stop writing
    private static final int RESUME_LOGGING_AFTER_TIME =  300 * 1000; // 5 mins 
    
    private boolean isLogSuspended=false;
    private long resumeTime=-1;
    

	// Maximum number of consecutive exceptions allowed before removing this
	// destination from the list.
	private int consecutiveExceptions = 0;
    
	private short sequenceNumber;
	private long lastSequenceStart;
	private int maxMsgLength        = DEFAULT_MAX_MSG_LENGTH;
	private int maxGeneralLength    = DEFAULT_MAX_GENERAL_LENGTH;
	private int maxExceptionLength  = DEFAULT_MAX_EXCEPTION_LENGTH;

	private Connection con;
	private Properties connProps;
	private PreparedStatement stmt;
	private StringBuffer insertStr;
       
    private boolean shutdown = false;


	public DbLogWriter(Properties properties) {
		connProps = properties;
	}


	/* (non-Javadoc)
	 * @see com.metamatrix.core.log.LogListener#shutdown()
	 */
    
    public synchronized void shutdown() {
        shutdown = true;
        cleanup();
    }
    
    // cleanup is used internally to close and cleanup connections after
    // the connection has failed.
	private void cleanup() {
		try {
			if( stmt != null ) {
				stmt.close();
			}
		} catch(SQLException ex) {
			System.err.println(CommonPlugin.Util.getString(ErrorMessageKeys.LOG_ERR_0027) + ex.getMessage());
		}
        
        stmt = null;

		try {
			if( con != null ) {     
				con.close();                
			}
		} catch(SQLException ex) {
			System.err.println(CommonPlugin.Util.getString(ErrorMessageKeys.LOG_ERR_0027) + ex.getMessage());
		}	
        con = null;

	}
    
    private void startup() throws DbWriterException {
        // initialize the connection
        con = getConnection();
        
    }
	
	public synchronized Connection getConnection() throws DbWriterException {
        SQLException firstException = null;
        Connection connection = null;
        
        long endTime = System.currentTimeMillis() + WAIT_TIME;
        
        
		// Establish connection and prepare statement
        while(true) {
    		try {
               
                           
        		  try {
        		      connection = JDBCConnectionPoolHelper.getConnection(connProps, LOGGING);
        		  } catch (ResourcePoolException err) {
                      // throw this as a SQLException so that it will be 
                      // wrapped above and set as the nextException
                      throw new SQLException(err.getMessage());
        		  }
                                  
        		  getStatement(connection);
                                
        		  return connection;
            } catch(SQLException sqle) {
                  cleanup();
                  if (firstException == null) {
                      firstException = sqle;
                  }
               
                  if (System.currentTimeMillis() > endTime) {
                        System.err.println(LogCommonConstants.CTX_LOGGING + " " + CommonPlugin.Util.getString(ErrorMessageKeys.LOG_ERR_0028, firstException.getMessage())); //$NON-NLS-1$
    
                        SQLException se = new SQLException(
                            CommonPlugin.Util.getString(
                                ErrorMessageKeys.LOG_ERR_0028,
                                firstException.getMessage()));
                       se.setNextException(firstException);
                    }
    
                    try {
                        Thread.sleep(RETRY_TIME); // retry every 5 seconds.
    
                    } catch (InterruptedException ie) {
                        // ignore it
                    }
              
            } 
        }
        
    }        
              
              
	private void getStatement(Connection connection) throws SQLException {
		  stmt = connection.prepareStatement(insertStr.toString());
	}
	
	public String getTableName(Properties props) {
		String tableName = props.getProperty(TABLE_PROPERTY_NAME, DEFAULT_TABLE_NAME);
		return tableName;
	}
	
	private static final String INSERT_INTO = "INSERT INTO "; //$NON-NLS-1$
	private static final String LEFT_PAREN = " ("; //$NON-NLS-1$
	private static final String COMMA = ","; //$NON-NLS-1$
	private static final String VALUES = ") VALUES (?,?,?,?,?,?,?,?,?)"; //$NON-NLS-1$
	/**
	 * Initialize this destination with the specified properties.
	 * @param props the properties that this destination should use to initialize
	 * itself.
	 * @throws LogDestinationInitFailedException if there was an error during initialization.
	 */
	public void initialize() throws DbWriterException {


		sequenceNumber = 0;
		lastSequenceStart = 0;

		try {
			int max = Integer.parseInt(connProps.getProperty(MAX_MESSAGE_LENGTH_PROPERTY_NAME));
			if( max > 0 ) {
				maxMsgLength = max;
			}
		} catch(Exception e) {
			// ignore and use default
		}
		try {
			
			int max = Integer.parseInt(connProps.getProperty(MAX_GENERAL_LENGTH_PROPERTY_NAME));
			if( max > 0 ) {
				maxGeneralLength = max;
			}
		} catch(Exception e) {
			// ignore and use default
		}
		try {
			int max = Integer.parseInt(connProps.getProperty(MAX_EXCEPTION_LENGTH_PROPERTY_NAME));
			if( max > 0 ) {
				maxExceptionLength = max;
			}
		} catch(Exception e) {
			// ignore and use default
		}

		// construct the insert string
		insertStr = new StringBuffer(INSERT_INTO);
		insertStr.append(getTableName(connProps));
		insertStr.append(LEFT_PAREN);
		insertStr.append( ColumnName.TIMESTAMP );
		insertStr.append(COMMA);
		insertStr.append( ColumnName.SEQUENCE_NUMBER );
		insertStr.append(COMMA);
		insertStr.append( ColumnName.CONTEXT );
		insertStr.append(COMMA);
		insertStr.append( ColumnName.LEVEL );
		insertStr.append(COMMA);
		insertStr.append( ColumnName.MESSAGE );
		insertStr.append(COMMA);
		insertStr.append( ColumnName.HOST );
		insertStr.append(COMMA);
		insertStr.append( ColumnName.VM );
		insertStr.append(COMMA);
		insertStr.append( ColumnName.THREAD );
		insertStr.append(COMMA);
		insertStr.append( '"'+ ColumnName.EXCEPTION +'"' );
		insertStr.append(VALUES);
        
        startup();
	}
	

	public void logMessage(LogMessage msg) {
		write(msg);	
	}
			
	private void write(LogMessage message) {
		// put this in a while to so that as long a 
        int retrycnt = 0;
        if (isLogSuspended) {
            if (System.currentTimeMillis() > resumeTime) {             
                resumeLogging();
            }
        }
		while (!isLogSuspended && !shutdown) {
			try {
				printMsg(message);
				return;

			} catch (SQLException ex) {

                if (retrycnt >= WRITE_RETRIES) {
                    suspendLogging();
                } else {
                    reconnect();
                }
                ++retrycnt;                

			} catch (Throwable t) {
			    // used to catch the NPE when the logger is 
                // shutdown while a write is in progress
                if (retrycnt >= WRITE_RETRIES) {
                    suspendLogging();
                } else {
                    reconnect();
                }
                ++retrycnt;                

            }
		}
	}
	
	private synchronized boolean reconnect()  {
		if (!shutdown) {
			try {
                if(con instanceof BaseResource) {
                    final BaseResource rsrc = (BaseResource)con;
                    //If the resource is checked out by the logger or is not checked out at all
                    //close the container so that we can get a good connection on the next retry.
                    if(LOGGING.equals(rsrc.getCheckedOutBy() ) || rsrc.getCheckedOutBy() == null){
                        ResourceContainer container = rsrc.getContainer();
                        container.shutDown();
                    }
                }
                
                //reset the statement and connection variables
                cleanup();                
                
                // get new connection
				con = getConnection();
                return true;
			} catch (Exception e) {
				System.err.println(LogCommonConstants.CTX_LOGGING + " " + CommonPlugin.Util.getString(ErrorMessageKeys.LOG_ERR_0028, e.getMessage())); //$NON-NLS-1$
				suspendLogging();
			}
		}
        return false;
	}

	
	
	private static final String NULL = "Null";    //$NON-NLS-1$
	private void printMsg(LogMessage message) throws SQLException {
        if (this.shutdown) {
            return;
        }
        
		long msgTimeStamp = message.getTimestamp();
		if (lastSequenceStart != msgTimeStamp) {
			lastSequenceStart = msgTimeStamp;
			sequenceNumber = 0;
		}
                        
			// Add values to Prepared statement
            
			// Timestamp column
			stmt.setString(1, DateUtil.getDateAsString(new Timestamp(msgTimeStamp)));

			// VM Sequence number
			stmt.setShort(2, sequenceNumber);

			// Message context column
			stmt.setString(3, StringUtil.truncString(message.getContext(), maxGeneralLength));

			// Message type column
			stmt.setInt(4, message.getLevel());

			// Message text column
			stmt.setString(5, StringUtil.truncString(message.getText(), maxMsgLength));

			// Message hostname column
			stmt.setString(6, StringUtil.truncString(message.getHostName(), maxGeneralLength));

			// Message VM ID column
			stmt.setString(7, StringUtil.truncString(message.getVMName(), maxGeneralLength));

			// Message thread name column
			stmt.setString(8, StringUtil.truncString(message.getThreadName(), maxGeneralLength));

			// Exception column
			if(message.getException() != null) {
				String eMsg = message.getException().getMessage();
				if ( eMsg == null ) {
					eMsg = NULL;
				} else {
					eMsg = StringUtil.truncString(eMsg, maxExceptionLength);
				}
				stmt.setString(9, eMsg);
			} else {
				stmt.setString(9, NULL);
			}


			// Insert the row into the table
			stmt.executeUpdate();
           

			// Execute was successful. Wipe out the outstanding exception cnt, if any
			if ( this.consecutiveExceptions > 0 ) {
				this.consecutiveExceptions = 0;
			}

			// Increment VM sequence number
			++sequenceNumber;
            
	}
    
    private synchronized void suspendLogging() {
        // suspend logging until the resumeTime has passed
        isLogSuspended=true;
        resumeTime = System.currentTimeMillis() + RESUME_LOGGING_AFTER_TIME;
        Date rd = new Date(resumeTime);
        String stringDate = DateUtil.getDateAsString(rd);
        System.err.println(CommonPlugin.Util.getString("DBLogWriter.Database_Logging_has_been_suspended", stringDate)); //$NON-NLS-1$
        
    }
    
    private synchronized void resumeLogging() {
        if (reconnect()) {
            // if the resume time has passed, then set the suspended flag to false
            // so that logging will resume
            isLogSuspended=false;
            resumeTime=-1;
            
            Date rd = new Date(System.currentTimeMillis());
            String stringDate = DateUtil.getDateAsString(rd);
            
            System.err.println(CommonPlugin.Util.getString("DBLogWriter.Database_Logging_has_been_resumed", stringDate)); //$NON-NLS-1$
        } 
       
    }

}
