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

package org.teiid.query.processor.relational;

import static org.teiid.query.analysis.AnalysisRecord.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.teiid.api.exception.query.QueryValidatorException;
import org.teiid.client.plan.PlanNode;
import org.teiid.common.buffer.BlockedException;
import org.teiid.common.buffer.TupleBatch;
import org.teiid.common.buffer.TupleSource;
import org.teiid.core.TeiidComponentException;
import org.teiid.core.TeiidProcessingException;
import org.teiid.query.QueryPlugin;
import org.teiid.query.eval.Evaluator;
import org.teiid.query.metadata.QueryMetadataInterface;
import org.teiid.query.rewriter.QueryRewriter;
import org.teiid.query.sql.lang.Command;
import org.teiid.query.util.CommandContext;


public class AccessNode extends SubqueryAwareRelationalNode {

    private static final int MAX_CONCURRENT = 10; //TODO: this could be settable via a property
	// Initialization state
    private Command command;
    private String modelName;
    private String connectorBindingId;
    private boolean shouldEvaluate = false;

    // Processing state
	private ArrayList<TupleSource> tupleSources = new ArrayList<TupleSource>();
	private boolean isUpdate = false;
    private boolean returnedRows = false;
    protected Command nextCommand;
    
    protected AccessNode() {
		super();
	}
    
	public AccessNode(int nodeID) {
		super(nodeID);
	}

    public void reset() {
        super.reset();
        this.tupleSources.clear();
		isUpdate = false;
        returnedRows = false;
        nextCommand = null;
    }

	public void setCommand(Command command) {
		this.command = command;
	}

    public Command getCommand() {
        return this.command;
    }

	public void setModelName(String name) {
		this.modelName = name;
	}

	public String getModelName() {
		return this.modelName;
	}

    public void setShouldEvaluateExpressions(boolean shouldEvaluate) {
        this.shouldEvaluate = shouldEvaluate;
    }

	public void open()
		throws TeiidComponentException, TeiidProcessingException {

        // Copy command and resolve references if necessary
        Command atomicCommand = command;
        boolean needProcessing = true;
        
        if (shouldEvaluate) {
	        atomicCommand = initialCommand();
        } 
        do {
			atomicCommand = nextCommand();
        	if(shouldEvaluate) {
	            needProcessing = prepareNextCommand(atomicCommand);
	            nextCommand = null;
	        } else {
	            needProcessing = RelationalNodeUtil.shouldExecute(atomicCommand, true);
	        }
	        // else command will not be changed, so no reason to all this work.
	        // Removing this if block and always evaluating has a significant cost that will
	        // show up in performance tests for many simple tests that do not require it.
	        
	        isUpdate = RelationalNodeUtil.isUpdate(atomicCommand);
	        
			if(needProcessing) {
				registerRequest(atomicCommand);
			}
			//We hardcode an upper limit on currency because these commands have potentially large in-memory value sets
        } while (!processCommandsIndividually() && hasNextCommand() && this.tupleSources.size() < Math.min(MAX_CONCURRENT, this.getContext().getUserRequestSourceConcurrency()));
	}

	static void rewriteAndEvaluate(Command atomicCommand, Evaluator eval, CommandContext context, QueryMetadataInterface metadata)
			throws TeiidProcessingException, TeiidComponentException {
		try {
		    // Defect 16059 - Rewrite the command to replace references, etc. with values.
			QueryRewriter.evaluateAndRewrite(atomicCommand, eval, context, metadata);
		} catch (QueryValidatorException e) {
		    throw new TeiidProcessingException(e, QueryPlugin.Util.getString("AccessNode.rewrite_failed", atomicCommand)); //$NON-NLS-1$
		}
	}
	
	@SuppressWarnings("unused")
	protected Command initialCommand() throws TeiidProcessingException, TeiidComponentException {
		return nextCommand();
	}

	protected Command nextCommand() {
		//it's important to save the next command
		//to ensure that the subquery ids remain stable
		if (nextCommand == null) {
			nextCommand = (Command) command.clone(); 
		}
		return nextCommand; 
	}

    protected boolean prepareNextCommand(Command atomicCommand) throws TeiidComponentException, TeiidProcessingException {
		rewriteAndEvaluate(atomicCommand, getEvaluator(Collections.emptyMap()), this.getContext(), this.getContext().getMetadata());
    	return RelationalNodeUtil.shouldExecute(atomicCommand, true);
    }

	public TupleBatch nextBatchDirect()
		throws BlockedException, TeiidComponentException, TeiidProcessingException {
        
        while (!tupleSources.isEmpty() || hasNextCommand()) {
        	
        	if (tupleSources.isEmpty() && processCommandsIndividually()) {
        		registerNext();
        	}
        	
        	//drain the tuple source(s)
        	for (int i = 0; i < this.tupleSources.size(); i++) {
        		TupleSource tupleSource = tupleSources.get(i);
        		try {
	        		List<?> tuple = null;
	        		
	        		while ((tuple = tupleSource.nextTuple()) != null) {
	                    returnedRows = true;
	                    addBatchRow(tuple);
	                    
	                    if (isBatchFull()) {
	                    	return pullBatch();
	                    }
	        		}
	        		
                	//end of source
                    tupleSource.closeSource();
                    tupleSources.remove(i--);
                    if (!processCommandsIndividually()) {
                    	registerNext();
                    }
                    continue;
        		} catch (BlockedException e) {
        			if (processCommandsIndividually()) {
        				throw e;
        			}
        			continue;
        		}
			}
        	
        	if (processCommandsIndividually()) {
        		if (hasPendingRows()) {
        			return pullBatch();
        		}
        		continue;
        	}
        	
        	if (!this.tupleSources.isEmpty()) {
        		throw BlockedException.INSTANCE;
        	}
        }
        
        if(isUpdate && !returnedRows) {
			List<Integer> tuple = new ArrayList<Integer>(1);
			tuple.add(Integer.valueOf(0));
            // Add tuple to current batch
            addBatchRow(tuple);
        }
        terminateBatches();
        return pullBatch();
	}

	private void registerNext() throws TeiidComponentException,
			TeiidProcessingException {
		while (hasNextCommand()) {
		    Command atomicCommand = nextCommand();
		    if (prepareNextCommand(atomicCommand)) {
		    	nextCommand = null;
		        registerRequest(atomicCommand);
		        break;
		    }
		    nextCommand = null;
		}
	}

	private void registerRequest(Command atomicCommand)
			throws TeiidComponentException, TeiidProcessingException {
		int limit = -1;
		if (getParent() instanceof LimitNode) {
			LimitNode parent = (LimitNode)getParent();
			if (parent.getLimit() > 0) {
				limit = parent.getLimit() + parent.getOffset();
			}
		}
		tupleSources.add(getDataManager().registerRequest(getContext(), atomicCommand, modelName, connectorBindingId, getID(), limit));
	}
	
	protected boolean processCommandsIndividually() {
		return false;
	}
    
    protected boolean hasNextCommand() {
        return false;
    }
    
	public void closeDirect() {
		super.closeDirect();
        closeSources();            
	}

    private void closeSources() {
    	for (TupleSource ts : this.tupleSources) {
    		ts.closeSource();			
		}
    	this.tupleSources.clear();
	}

	protected void getNodeString(StringBuffer str) {
		super.getNodeString(str);
		str.append(command);
	}

	public Object clone(){
		AccessNode clonedNode = new AccessNode();
		this.copy(this, clonedNode);
		return clonedNode;
	}

	protected void copy(AccessNode source, AccessNode target){
		super.copy(source, target);
		target.modelName = source.modelName;
		target.connectorBindingId = source.connectorBindingId;
		target.shouldEvaluate = source.shouldEvaluate;
		target.command = source.command;
	}

    public PlanNode getDescriptionProperties() {
    	PlanNode props = super.getDescriptionProperties();
        props.addProperty(PROP_SQL, this.command.toString());
        props.addProperty(PROP_MODEL_NAME, this.modelName);
        return props;
    }

	public String getConnectorBindingId() {
		return connectorBindingId;
	}

	public void setConnectorBindingId(String connectorBindingId) {
		this.connectorBindingId = connectorBindingId;
	}
    
}