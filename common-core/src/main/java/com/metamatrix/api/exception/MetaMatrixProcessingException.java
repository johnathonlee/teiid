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

package com.metamatrix.api.exception;

/**
 * This exception is a superclass for exceptions that are thrown during 
 * processing as a result of user input.  This exception is the result
 * of handling a user request, not the result of an internal error.
 */
public class MetaMatrixProcessingException extends MetaMatrixException {

	private static final long serialVersionUID = -4013536109023540872L;

	/**
     * No-arg Constructor
     */
    public MetaMatrixProcessingException() {
        super();
    }

    /**
     * Construct an instance with the message specified.
     *
     * @param message A message describing the exception
     */
    public MetaMatrixProcessingException( String message ) {
        super( message );
    }

    /**
     * Construct an instance with a linked exception specified.
     *
     * @param e An exception to chain to this exception
     */
    public MetaMatrixProcessingException( Throwable e ) {
		super( e );
    }

    /**
     * Construct an instance with the message and error code specified.
     *
     * @param message A message describing the exception
     * @param code The error code
     */
    public MetaMatrixProcessingException( String code, String message ) {
        super( code, message );
    }

    /**
     * Construct an instance from a message and an exception to chain to this one.
     *
     * @param message A message describing the exception
     * @param e An exception to nest within this one
     */
    public MetaMatrixProcessingException( Throwable e, String message ) {
        super( e, message );
    }

    /**
     * Construct an instance from a message and a code and an exception to
     * chain to this one.
     *
     * @param e An exception to nest within this one
     * @param message A message describing the exception
     * @param code A code denoting the exception
     */
    public MetaMatrixProcessingException( Throwable e, String code, String message ) {
        super( e, code, message );
    }

} // END CLASS

