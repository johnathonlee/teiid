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

package com.metamatrix.query.processor.xml;

import org.teiid.client.plan.PlanNode;
import org.teiid.logging.LogManager;

import com.metamatrix.api.exception.MetaMatrixComponentException;
import com.metamatrix.api.exception.MetaMatrixProcessingException;
import com.metamatrix.common.buffer.BlockedException;
import com.metamatrix.query.execution.QueryExecPlugin;
import com.metamatrix.query.util.ErrorMessageKeys;

/**
 * This instruction, intended to be reached conditionally (only under certain
 * criteria) will throw a RuntimeException and cause query processing to
 * be aborted.  The {@link #process} method automatically and always throws
 * a RuntimeException.
 */
public class AbortProcessingInstruction extends ProcessorInstruction {

    /**
     * Default message included in the RuntimeException thrown from
     * {@link #process}
     */
    public static final String DEFAULT_MESSAGE = QueryExecPlugin.Util.getString(ErrorMessageKeys.PROCESSOR_0054);

    /**
     * Constructor for AbortProcessingInstruction.
     */
    public AbortProcessingInstruction() {
        super();
    }

    /**
     * @see ProcessorInstruction#process(ProcessorEnvironment)
     * @throws MetaMatrixComponentException always
     */
    public XMLContext process(XMLProcessorEnvironment env, XMLContext context)
        throws BlockedException, MetaMatrixComponentException, MetaMatrixProcessingException{

        LogManager.logTrace(org.teiid.logging.LogConstants.CTX_XML_PLAN, "ABORT processing now."); //$NON-NLS-1$
        throw new MetaMatrixComponentException(DEFAULT_MESSAGE);
    }

    public String toString() {
        return "ABORT"; //$NON-NLS-1$
    }

    public PlanNode getDescriptionProperties() {
    	return new PlanNode("ABORT"); //$NON-NLS-1$        
    }

}
