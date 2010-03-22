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

package com.metamatrix.query.processor.relational;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.metamatrix.api.exception.MetaMatrixComponentException;
import com.metamatrix.api.exception.MetaMatrixProcessingException;
import com.metamatrix.common.buffer.BlockedException;
import com.metamatrix.common.buffer.BufferManager;
import com.metamatrix.common.buffer.TupleBatch;
import com.metamatrix.query.processor.DescribableUtil;
import com.metamatrix.query.processor.ProcessorDataManager;
import com.metamatrix.query.processor.ProcessorPlan;
import com.metamatrix.query.sql.lang.QueryCommand;
import com.metamatrix.query.util.CommandContext;

/**
 */
public class RelationalPlan extends ProcessorPlan {

	// Initialize state - don't reset
	private RelationalNode root;
	private List outputCols;

    /**
     * Constructor for RelationalPlan.
     */
    public RelationalPlan(RelationalNode node) {
        this.root = node;
    }

    public RelationalNode getRootNode() {
        return this.root;
    }
    
    public void setRootNode(RelationalNode root) {
        this.root = root;
    }

    /**
     * @see ProcessorPlan#connectDataManager(ProcessorDataManager)
     */
    public void initialize(CommandContext context, ProcessorDataManager dataMgr, BufferManager bufferMgr) {
        setContext(context);        
		connectExternal(this.root, context, dataMgr, bufferMgr);	
    }        

	private void connectExternal(RelationalNode node, CommandContext context, ProcessorDataManager dataMgr, BufferManager bufferMgr) {		
                                    
        node.initialize(context, bufferMgr, dataMgr);

        RelationalNode[] children = node.getChildren();  
        for(int i=0; i<children.length; i++) {
            if(children[i] != null) {
                connectExternal(children[i], context, dataMgr, bufferMgr);                
            } else {
                break;
            }
        }
    }
    
    /**
     * Get list of resolved elements describing output columns for this plan.
     * @return List of SingleElementSymbol
     */
    public List getOutputElements() {
        return this.outputCols;
    }

    public void open()
        throws MetaMatrixComponentException, MetaMatrixProcessingException {
            
        this.root.open();
    }

    /**
     * @see ProcessorPlan#nextBatch()
     */
    public TupleBatch nextBatch()
        throws BlockedException, MetaMatrixComponentException, MetaMatrixProcessingException {

        return this.root.nextBatch();
    }

    public void close()
        throws MetaMatrixComponentException {
            
        this.root.close();
    }

    /**
     * @see com.metamatrix.query.processor.ProcessorPlan#reset()
     */
    public void reset() {
        super.reset();
        
        this.root.reset();
    }

	public String toString() {
		return this.root.toString();    
	}
    
	public RelationalPlan clone(){
		RelationalPlan plan = new RelationalPlan((RelationalNode)root.clone());
		plan.setOutputElements(new ArrayList(( outputCols != null ? outputCols : Collections.EMPTY_LIST )));
		return plan;
	}
	
    /* 
     * @see com.metamatrix.query.processor.Describable#getDescriptionProperties()
     */
    public Map getDescriptionProperties() {
        Map props = new HashMap();
        props.put(PROP_TYPE, "Relational Plan"); //$NON-NLS-1$
        List children = new ArrayList();
        Map childProps = getRootNode().getDescriptionProperties();
        children.add(childProps);
        props.put(PROP_CHILDREN, children);
        props.put(PROP_OUTPUT_COLS, DescribableUtil.getOutputColumnProperties(getOutputElements()));
        return props;
    }
    
    /** 
     * @param outputCols The outputCols to set.
     */
    public void setOutputElements(List outputCols) {
        this.outputCols = outputCols;
    }
    
    @Override
    public boolean requiresTransaction(boolean transactionalReads) {
    	return requiresTransaction(transactionalReads, root);
    }
    
    /**
     * Currently does not detect procedures in non-inline view subqueries
     */
    boolean requiresTransaction(boolean transactionalReads, RelationalNode node) {
    	if (node instanceof DependentAccessNode) {
			if (transactionalReads || !(((DependentAccessNode)node).getCommand() instanceof QueryCommand)) {
				return true;
			}
			return false;
		}
    	if (node instanceof AccessNode) {
			return false;
		}
		if (transactionalReads) {
			return true;
		}
		if (node instanceof PlanExecutionNode) {
			ProcessorPlan plan = ((PlanExecutionNode)node).getProcessorPlan();
			return plan.requiresTransaction(transactionalReads);
		}
		for (RelationalNode child : node.getChildren()) {
			if (child != null && requiresTransaction(transactionalReads, child)) {
				return true;
			}
		}
		return false;
    }
	
}
