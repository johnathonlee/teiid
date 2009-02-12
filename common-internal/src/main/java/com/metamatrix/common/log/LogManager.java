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

package com.metamatrix.common.log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.metamatrix.common.CommonPlugin;
import com.metamatrix.common.config.CurrentConfiguration;
import com.metamatrix.common.config.api.exceptions.ConfigurationException;
import com.metamatrix.common.log.config.BasicLogConfiguration;
import com.metamatrix.common.log.config.DefaultLogConfigurationFactory;
import com.metamatrix.common.log.config.LogConfigurationException;
import com.metamatrix.common.util.ErrorMessageKeys;
import com.metamatrix.common.util.LogCommonConstants;
import com.metamatrix.core.MetaMatrixRuntimeException;
import com.metamatrix.core.log.LogMessage;
import com.metamatrix.core.log.MessageLevel;
import com.metamatrix.internal.core.log.PlatformLog;


/**
 * This class represents the interface to a single logging framework
 * that is easily accessible by any component.  Using the LogManager, a component
 * can quickly submit a log message, and can rely upon the LogManager to determine
 * (a) whether that message is to be recorded or discarded; and (b) where
 * to send any recorded messages.  Thus, the component's code that submits
 * messages does not have to be modified to alter the logging behavior of the
 * application.
 * <p>
 * By default, all context(s) are logged by the LogManager.   The messages that
 * the LogManager actually records and sends to the destinations
 * can be controlled using two different and orthogonal parameters.
 * The first is a message <i>level</i> that filters messages based upon detail,
 * and the second is a message <i>context</i> that filters messages based upon
 * origin.  The LogManager tracks only those context(s) that should NOT be
 * logged.  Only if a message (which also is defined with these two parameters)
 * passes both filters will it be sent to the destinations.
 * <p>
 * Each message is submitted with one of the following levels (determined
 * by the particular method used to submit the message), sorted from the
 * least detailed to the greatest:
 * <li><b>Critical</b>:  This level of message is generally
 *      used to record an event or error that must be recorded (if any logging
 *      is used).  If it is used to record an error, it generally means that the
 *      system encountered a critical error which affects the integrity, accuracy,
 *      reliability and/or capability of the system.</li>
 * <li><b>Error</b>:  Error messages are generally used
 *      to record unexpected problems, or errors that are not critical in nature
 *      and from which the system can automatically recover.</li>
 * <li><b>Warning</b>:  Warning messages generally described
 *      expected errors from which the system should recover.  However, this level
 *      is used to record the fact that such an error or event did occur.</li>
 * <li><b>Information</b>:  This level of logging is the usually
 *      the normal level.  All interesting periodic events should be logged at this
 *      level so someone looking through the log can see the amount and kind of
 *      processing happening in the system.</li>
 * <li><b>Detail</b>:  Such messages are moderately detailed,
 *      and help to debug typical problems in the system.  Generally, these
 *      messages are not so detailed that the big picture gets lost.</li>
 * <li><b>Trace</b>:  A trace message is the most detailed
 *      logging level, used to trace system execution for really nasty problems.
 *      At this level, logging will be so verbose that the system performance
 *      may be affected.</li>
 * <p>
 * The context for a message is any application-specified String.  Again, only
 * those message contexts that match those in the LogManager's configuration will
 * be sent to the destinations.
 *
 */
public final class LogManager {

    /**
     * The name of the System property that contains the set of comma-separated
     * context names for messages <i>not</i> to be recorded.  A message context is simply
     * some string that identifies something about the component that generates
     * the message.  The value for the contexts is application specific.
     * <p>
     * This is an optional property that defaults to no contexts (i.e., messages
     * with any context are recorded).
     */
    public static final String SYSTEM_LOG_CONTEXT_PROPERTY_NAME = DefaultLogConfigurationFactory.LOG_CONTEXT_PROPERTY_NAME;

    /**
     * The name of the System property that contains 'true' if the log messages
     * are to be sent to System.out, or 'false' otherwise.  This is an optional
     * property that defaults to 'true'.  Note, however, that if the message
     * level for the logger is specified to be something other than NONE but
     * no file destination is specified, the value for this propery is
     * always assumed to be 'true'.
     * <p>
     * If the System.out is captured by the LogManager, the LogManager always
     * treats this property value as 'false'.
     */
    public static final String SYSTEM_LOG_CONSOLE_PROPERTY_NAME = "metamatrix.log.console"; //$NON-NLS-1$

    /**
     * The name of the System property that should be 'true' if System.out is to
     * be captured by the LogManager, or false if System.out is not be be captured.
     * This is an optional property that defaults to 'false'.
     */
    public static final String SYSTEM_LOG_CAPTURE_SYSTEM_OUT    = "metamatrix.log.captureSystemOut"; //$NON-NLS-1$

    /**
     * The name of the System property that should be 'true' if System.err is to
     * be captured by the LogManager, or false if System.err is not be be captured.
     * This is an optional property that defaults to 'false'.
     */
    public static final String SYSTEM_LOG_CAPTURE_SYSTEM_ERR    = "metamatrix.log.captureSystemErr"; //$NON-NLS-1$

    /**
     * The name of the System property that should be set to the name of the file
     * to which System.out is sent only if also captured by the LogManager.
     * This is an optional property.  If set to the same value as
     * #SYSTEM_ERR_FILENAME (case insensitive comparison), then the same file will be used for both.
     */
    public static final String SYSTEM_OUT_FILENAME              = "metamatrix.log.systemOutFilename"; //$NON-NLS-1$

    /**
     * The name of the System property that should be set to the name of the file
     * to which System.err is sent only if also captured by the LogManager.
     * This is an optional property.  If set to the same value as
     * #SYSTEM_OUT_FILENAME (case insensitive comparison), then the same file will be used for both.
     */
    public static final String SYSTEM_ERR_FILENAME              = "metamatrix.log.systemErrFilename"; //$NON-NLS-1$

    private static LogManager INSTANCE = new LogManager(PlatformLog.getInstance());

    private LogConfiguration configuration;
    private PlatformLog platformLog;
    
    protected LogManager(PlatformLog platformLog) {
    	this.platformLog = platformLog;
    	// Get the preferred LogConfiguration ...
        try {
            configuration = (LogConfiguration)CurrentConfiguration.getInstance().getConfiguration().getLogConfiguration().clone();
        } catch ( ConfigurationException e ) {

            // Get the initial LogConfiguration from the System properties ...
            try {
                configuration = BasicLogConfiguration.createLogConfiguration(System.getProperties());
            } catch ( LogConfigurationException le ) {
                throw new MetaMatrixRuntimeException(le, ErrorMessageKeys.LOG_ERR_0005, CommonPlugin.Util.getString( ErrorMessageKeys.LOG_ERR_0005) );

            }
        }
    }

    static LogManager getInstance() {
        return INSTANCE;
    }

    /**
     * Send a critical message to the log.  This level of message is generally
     * used to record an event or error that must be recorded (if any logging
     * is used).  If it is used to record an error, it generally means that the
     * system encountered a critical error which affects the integrity, accuracy,
     * reliability and/or capability of the system.
     * <p>
     * Only if the log manager is configured to send such messages to the
     * destination will the message be recorded.
     * @param context the context for this log message (for example, the component
     * that is generating this message).
     * @param message the log message; the message is
     * not logged if this parameter is null
     */
    public static void logCritical(String context, String message) {
    	LogManager.getInstance().logMessage(MessageLevel.CRITICAL, context, message);
    }

    /**
     * Send a critical message to the log.  This level of message is generally
     * used to record an event or error that must be recorded (if any logging
     * is used).  If it is used to record an error, it generally means that the
     * system encountered a critical error which affects the integrity, accuracy,
     * reliability and/or capability of the system.
     * <p>
     * Only if the log manager is configured to send such messages to the
     * destination will the message be recorded.
     * @param context the context for this log message (for example, the component
     * that is generating this message).
     * @param e the exception that is to be logged; the message is
     * not logged if this parameter is null
     * @param message the log message (may be null)
     */
    public static void logCritical(String context, Throwable e, String message) {
        LogManager.getInstance().logMessage(MessageLevel.CRITICAL,context,e,message);
    }

    /**
     * Send an error message to the log.  Error messages are generally used
     * to record unexpected problems, or errors that are not critical in nature
     * and from which the system can automatically recover.
     * <p>
     * Only if the log manager is configured to send such messages to the
     * destination will the message be recorded.
     * @param context the context for this log message (for example, the component
     * that is generating this message).
     * @param message the log message; the message is
     * not logged if this parameter is null
     */
    public static void logError(String context, String message) {
        LogManager.getInstance().logMessage(MessageLevel.ERROR, context,message);
    }

    /**
     * Send an error message to the log.  Error messages are generally used
     * to record unexpected problems, or errors that are not critical in nature
     * and from which the system can automatically recover.
     * <p>
     * Only if the log manager is configured to send such messages to the
     * destination will the message be recorded.
     * @param context the context for this log message (for example, the component
     * that is generating this message).
     * @param e the exception that is to be logged; the message is
     * not logged if this parameter is null
     * @param message the log message (may be null)
     */
    public static void logError(String context, Throwable e, String message) {
        LogManager.getInstance().logMessage(MessageLevel.ERROR,context,e,message);
    }
    
    /**
     * Send a warning message to the log.  Warning messages generally described
     * expected errors from which the system should recover.  However, this level
     * is used to record the fact that such an error or event did occur.
     * <p>
     * Only if the log manager is configured to send such messages to the
     * destination will the message be recorded.
     * @param context the context for this log message (for example, the component
     * that is generating this message).
     * @param message the log message; the message is
     * not logged if this parameter is null
     */
    public static void logWarning(String context, String message) {
        LogManager.getInstance().logMessage(MessageLevel.WARNING, context,message);
    }

    /**
     * Send a warning message to the log.  Warning messages generally described
     * expected errors from which the system should recover.  However, this level
     * is used to record the fact that such an error or event did occur.
     * <p>
     * Only if the log manager is configured to send such messages to the
     * destination will the message be recorded.
     * @param context the context for this log message (for example, the component
     * that is generating this message).
     * @param e the exception that is to be logged; the message is
     * not logged if this parameter is null
     * @param message the log message (may be null)
     */
    public static void logWarning(String context, Throwable e, String message) {
        LogManager.getInstance().logMessage(MessageLevel.WARNING,context,e,message);
    }
    
    /**
     * Send a information message to the log.  This level of logging is the usually
     * the normal level.  All interesting periodic events should be logged at this
     * level so someone looking through the log can see the amount and kind of
     * processing happening in the system.
     * <p>
     * Only if the log manager is configured to send such messages to the
     * destination will the message be recorded.
     * @param context the context for this log message (for example, the component
     * that is generating this message).
     * @param message the log message; the message is
     * not logged if this parameter is null
     */
    public static void logInfo(String context, String message) {
        LogManager.getInstance().logMessage(MessageLevel.INFO, context,message);
    }
    
    /**
     * Send a detail message to the log.  Such messages are moderately detailed,
     * and help to debug typical problems in the system.  Generally, these
     * messages are not so detailed that the big picture gets lost.
     * <p>
     * Only if the log manager is configured to send such messages to the
     * destination will the message be recorded.
     * @param context the context for this log message (for example, the component
     * that is generating this message).
     * @param msgParts the individual parts of the log message; the message is
     * not logged if this parameter is null
     */
    public static void logDetail(String context, Object ... msgParts) {
        LogManager.getInstance().logMessage(MessageLevel.DETAIL, context, msgParts);
    }

    /**
     * Send a detail message to the log.  Such messages are moderately detailed,
     * and help to debug typical problems in the system.  Generally, these
     * messages are not so detailed that the big picture gets lost.
     * <p>
     * Only if the log manager is configured to send such messages to the
     * destination will the message be recorded.
     * @param context the context for this log message (for example, the component
     * that is generating this message).
     * @param e the exception that is to be logged; the message is
     * not logged if this parameter is null
     * @param message the log message (may be null)
     */
    public static void logDetail(String context, Throwable e, String message) {
        LogManager.getInstance().logMessage(MessageLevel.DETAIL,context,e,message);
    }

    /**
     * Send a trace message to the log.  A trace message is the most detailed
     * logging level, used to trace system execution for really nasty problems.
     * At this level, logging will be so verbose that the system performance
     * may be affected.
     * <p>
     * Only if the log manager is configured to send such messages to the
     * destination will the message be recorded.
     * @param context the context for this log message (for example, the component
     * that is generating this message).
     * @param msgParts the individual parts of the log message; the message is
     * not logged if this parameter is null
     */
    public static void logTrace(String context, Object ... msgParts) {
        LogManager.getInstance().logMessage(MessageLevel.TRACE, context, msgParts);
    }

    /**
     * Send a trace message to the log.  A trace message is the most detailed
     * logging level, used to trace system execution for really nasty problems.
     * At this level, logging will be so verbose that the system performance
     * may be affected.
     * <p>
     * Only if the log manager is configured to send such messages to the
     * destination will the message be recorded.
     * @param context the context for this log message (for example, the component
     * that is generating this message).
     * @param e the exception that is to be logged; the message is
     * not logged if this parameter is null
     * @param msgParts the individual parts of the log message (may be null)
     */
    public static void logTrace(String context, Throwable e, Object ... msgParts) {
        LogManager.getInstance().logMessage(MessageLevel.TRACE,context,e,msgParts);
    }

    /**
     * Send a message of the specified level to the log.
     * <p>
     * Only if the log manager is configured to send such messages to the
     * destination will the message be recorded.
     * @param msgLevel
     * @param context the context for this log message (for example, the component
     * that is generating this message).
     * @param message the individual parts of the log message; the message is
     * not logged if this parameter is null
     */
    public static void log(int msgLevel, String context, String message) {
        LogManager.getInstance().logMessage(msgLevel, context, message);
    }

    /**
     * Send a message of the specified level to the log.
     * <p>
     * Only if the log manager is configured to send such messages to the
     * destination will the message be recorded.
     * @param context the context for this log message (for example, the component
     * that is generating this message).
     * @param e the exception that is to be logged; the message is
     * not logged if this parameter is null
     * @param message the individual parts of the log message; the message is
     * not logged if this parameter is null
     */
    public static void log(int msgLevel, String context, Throwable e, String message) {
        LogManager.getInstance().logMessage(msgLevel, context, e, message);
    }

    /**
     * Utility method to obtain the a modifiable log configuration for the LogManager.
     * <p>After modifying the log config, user must call {@link #setLogConfiguration(LogConfiguration)} to
     * affect the logging configuration.</p>
     * @return a modifiable copy of the current log configuration
     */
    public static LogConfiguration getLogConfigurationCopy() {
    	return getInstance().getConfigurationCopy();
    }

    public static void setLogConfiguration( LogConfiguration config ) {
    	getInstance().setConfiguration(config);
    }
    
    public LogConfiguration getConfigurationCopy() {
    	return (LogConfiguration)configuration.clone(); 
	}
	
	public void setConfiguration(LogConfiguration config) {
		if ( config != null ) {
        	logMessage(MessageLevel.INFO, LogCommonConstants.CTX_LOGGING, CommonPlugin.Util.getString("MSG.003.014.0015", config)); //$NON-NLS-1$
            configuration = (LogConfiguration) config.clone();
        }
	}

    /**
     * Utility method to identify whether a log message with the specified
     * context and level will be recorded in the LogManager's destinations.
     * @param context
     * @param msgLevel
     * @return true if the message would be recorded if sent to the LogManager,
     * or false if it would be discarded by the LogManager.
     */
    public static boolean isMessageToBeRecorded(String context, int msgLevel) {
    	return getInstance().isLoggingEnabled(context, msgLevel);
    }
    
    public boolean isLoggingEnabled(String context, int msgLevel) {
    	if ( context == null ) {
            return false;
        }
                
        // If the messsage's level is greater than the logging level,
        // then the message should NOT be recorded ...
        if ( configuration.getMessageLevel() == MessageLevel.NONE || msgLevel <= MessageLevel.NONE ||
        		configuration.isLevelDiscarded( msgLevel ) || configuration.isContextDiscarded( context )) {
            return false;
        }

        return true;
    }

    protected void logMessage(int level, String context, Object ... msgParts) {
    	logMessage(level, context, null, msgParts);
    }

    protected void logMessage(int level, String context, Throwable e, Object ... msgParts) {
		if (msgParts == null || msgParts.length == 0 || !isLoggingEnabled(context, level)) {
			return;
		} 

        LogMessage msg = new LogMessage( context, level, e, msgParts);
        forwardMessage(msg);
    }


    protected void forwardMessage(LogMessage msg) {
        platformLog.logMessage(msg);
    }
    
    public static Object createLoggingProxy(final String loggingContext,
                                             final Object instance,
                                             final Class[] interfaces,
                                             final int level) {
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), interfaces, new InvocationHandler() {

            public Object invoke(Object proxy,
                                 Method method,
                                 Object[] args) throws Throwable {
                boolean log = LogManager.isMessageToBeRecorded(loggingContext, level);
                if (log) {
                    StringBuffer message = new StringBuffer();
                    message.append("before "); //$NON-NLS-1$
                    message.append(method.getName());
                    message.append(":"); //$NON-NLS-1$
                    message.append(instance);
                    message.append("("); //$NON-NLS-1$
                    if (args != null) {
	                    for (int i = 0; i < args.length; i++) {
	                        if (args[i] != null) {
	                        	message.append(args[i]);
	                        } else {
	                        	message.append("null"); //$NON-NLS-1$
	                        }
	                        if (i != args.length - 1) {
	                        	message.append(","); //$NON-NLS-1$
	                        }
	                    }
                    }
                    message.append(")"); //$NON-NLS-1$
                    LogManager.log(level, loggingContext, message.toString());
                }
                try {
                    Object result = method.invoke(instance, args);
                    if (log) {
                        LogManager.log(level, loggingContext, 
                            "after " + method.getName()+ " : "+result); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    return result;
                } catch (InvocationTargetException e) {
                    throw e.getTargetException();
                }
            }
        });
    }
}
