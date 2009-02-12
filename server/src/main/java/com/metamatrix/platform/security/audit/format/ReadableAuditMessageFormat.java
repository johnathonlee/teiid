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

package com.metamatrix.platform.security.audit.format;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.metamatrix.platform.security.audit.AuditMessage;

public class ReadableAuditMessageFormat implements AuditMessageFormat {

    public static final String TIMESTAMP_FORMAT = "MMM dd, yyyy HH:mm:ss.SSS"; //$NON-NLS-1$

	// Cache date formatter which is expensive to create
	private static DateFormat DATE_FORMATTER = new SimpleDateFormat(TIMESTAMP_FORMAT);
    private static final String DEFAULT_FORMATTED_MESSAGE = ""; //$NON-NLS-1$

	/**
	 * Format the specified message and return the String representation.
     * @param message the log message to be formated.
     * @return the String representation of the log message.
	 */
	public String formatMessage( AuditMessage message ) {
        if ( message == null ) {
            return DEFAULT_FORMATTED_MESSAGE;
        }
        StringBuffer msg = new StringBuffer();
        msg.append( DATE_FORMATTER.format( new Date(message.getTimestamp()) ) );
        msg.append(" ["); //$NON-NLS-1$
        msg.append( message.getHostName() );
        msg.append( '|' );
        msg.append( message.getVMName() );
        msg.append( '|' );
        msg.append( message.getPrincipal() );
        msg.append("] <"); //$NON-NLS-1$
        msg.append( message.getContext() );
        msg.append('.');
        msg.append( message.getActivity() );
        msg.append("> "); //$NON-NLS-1$
        msg.append( message.getText() );

        return msg.toString();
	}

}
