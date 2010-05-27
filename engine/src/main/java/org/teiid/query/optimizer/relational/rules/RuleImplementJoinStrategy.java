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

package org.teiid.query.optimizer.relational.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.teiid.api.exception.query.QueryMetadataException;
import org.teiid.api.exception.query.QueryPlannerException;
import org.teiid.core.TeiidComponentException;
import org.teiid.query.analysis.AnalysisRecord;
import org.teiid.query.metadata.QueryMetadataInterface;
import org.teiid.query.optimizer.capabilities.CapabilitiesFinder;
import org.teiid.query.optimizer.relational.OptimizerRule;
import org.teiid.query.optimizer.relational.RuleStack;
import org.teiid.query.optimizer.relational.plantree.NodeConstants;
import org.teiid.query.optimizer.relational.plantree.NodeEditor;
import org.teiid.query.optimizer.relational.plantree.NodeFactory;
import org.teiid.query.optimizer.relational.plantree.PlanNode;
import org.teiid.query.processor.relational.JoinNode.JoinStrategyType;
import org.teiid.query.processor.relational.MergeJoinStrategy.SortOption;
import org.teiid.query.sql.lang.Command;
import org.teiid.query.sql.lang.JoinType;
import org.teiid.query.sql.lang.OrderBy;
import org.teiid.query.sql.symbol.GroupSymbol;
import org.teiid.query.sql.symbol.SingleElementSymbol;
import org.teiid.query.sql.util.SymbolMap;
import org.teiid.query.sql.visitor.GroupsUsedByElementsVisitor;
import org.teiid.query.util.CommandContext;


/**
 * Inserts sort nodes for specific join strategies.
 */
public class RuleImplementJoinStrategy implements OptimizerRule {
        
    /** 
     * @see org.teiid.query.optimizer.relational.OptimizerRule#execute(org.teiid.query.optimizer.relational.plantree.PlanNode, org.teiid.query.metadata.QueryMetadataInterface, org.teiid.query.optimizer.capabilities.CapabilitiesFinder, org.teiid.query.optimizer.relational.RuleStack, org.teiid.query.analysis.AnalysisRecord, org.teiid.query.util.CommandContext)
     */
    public PlanNode execute(PlanNode plan,
                            QueryMetadataInterface metadata,
                            CapabilitiesFinder capabilitiesFinder,
                            RuleStack rules,
                            AnalysisRecord analysisRecord,
                            CommandContext context) throws QueryPlannerException,
                                                   QueryMetadataException,
                                                   TeiidComponentException {
    	
    	for (PlanNode sourceNode : NodeEditor.findAllNodes(plan, NodeConstants.Types.SOURCE, NodeConstants.Types.ACCESS)) {
    		SymbolMap references = (SymbolMap)sourceNode.getProperty(NodeConstants.Info.CORRELATED_REFERENCES);
	        if (references != null) {
	        	Set<GroupSymbol> groups = GroupsUsedByElementsVisitor.getGroups(references.getValues());
	        	PlanNode joinNode = NodeEditor.findParent(sourceNode, NodeConstants.Types.JOIN, NodeConstants.Types.SOURCE);
	        	while (joinNode != null) {
	        		joinNode.setProperty(NodeConstants.Info.JOIN_STRATEGY, JoinStrategyType.NESTED_TABLE);
	        		if (joinNode.getProperty(NodeConstants.Info.DEPENDENT_VALUE_SOURCE) != null) {
	        			//sanity check
	        			throw new AssertionError("Cannot use a depenedent join when the join involves a correlated nested table.");  //$NON-NLS-1$
	        		}
	        		if (joinNode.getGroups().containsAll(groups)) {
	        			break;
	        		}
	        		joinNode = NodeEditor.findParent(joinNode, NodeConstants.Types.JOIN, NodeConstants.Types.SOURCE);
	        	}
	        }
    	}

        for (PlanNode joinNode : NodeEditor.findAllNodes(plan, NodeConstants.Types.JOIN, NodeConstants.Types.ACCESS)) {
            JoinStrategyType stype = (JoinStrategyType) joinNode.getProperty(NodeConstants.Info.JOIN_STRATEGY);
            if (!JoinStrategyType.MERGE.equals(stype)) {
            	continue;
            } 
            
            /**
             * Don't push sorts for unbalanced inner joins, we prefer to use partitioning 
             */
            boolean pushLeft = true;
            boolean pushRight = true;
            if (joinNode.getProperty(NodeConstants.Info.JOIN_TYPE) == JoinType.JOIN_INNER && context != null) {
            	float leftCost = NewCalculateCostUtil.computeCostForTree(joinNode.getFirstChild(), metadata);
            	float rightCost = NewCalculateCostUtil.computeCostForTree(joinNode.getLastChild(), metadata);
            	if (leftCost != NewCalculateCostUtil.UNKNOWN_VALUE && rightCost != NewCalculateCostUtil.UNKNOWN_VALUE 
            			&& (leftCost > context.getProcessorBatchSize() || rightCost > context.getProcessorBatchSize())) {
            		//we use a larger constant here to ensure that we don't unwisely prevent pushdown
            		pushLeft = leftCost < context.getProcessorBatchSize() || leftCost / rightCost < 16;
            		pushRight = rightCost < context.getProcessorBatchSize() || rightCost / leftCost < 16 || joinNode.getProperty(NodeConstants.Info.DEPENDENT_VALUE_SOURCE) != null;
            	}
            }            

            boolean pushedLeft = insertSort(joinNode.getFirstChild(), (List<SingleElementSymbol>) joinNode.getProperty(NodeConstants.Info.LEFT_EXPRESSIONS), joinNode, metadata, capabilitiesFinder, pushLeft);	
            insertSort(joinNode.getLastChild(), (List<SingleElementSymbol>) joinNode.getProperty(NodeConstants.Info.RIGHT_EXPRESSIONS), joinNode, metadata, capabilitiesFinder, pushRight);
        	
        	if (joinNode.getProperty(NodeConstants.Info.JOIN_TYPE) == JoinType.JOIN_INNER && (!pushRight || !pushedLeft)) {
        		joinNode.setProperty(NodeConstants.Info.JOIN_STRATEGY, JoinStrategyType.PARTITIONED_SORT);
        	}
        }
        
        return plan;
    }

    /**
     * Insert a sort node under the merge join node.  If necessary, also insert a project
     * node to handle function evaluation.  
     * @param expressions The expressions that need to be sorted on 
     * @param jnode The planner merge join node to attach to
     * @return returns true if a project node needs added
     * @throws TeiidComponentException 
     * @throws QueryMetadataException 
     */
    private static boolean insertSort(PlanNode childNode, List<SingleElementSymbol> expressions, PlanNode jnode, QueryMetadataInterface metadata, CapabilitiesFinder capFinder,
    		boolean attemptPush) throws QueryMetadataException, TeiidComponentException {
        Set<SingleElementSymbol> orderSymbols = new LinkedHashSet<SingleElementSymbol>(expressions); 

        PlanNode sourceNode = FrameUtil.findJoinSourceNode(childNode);
        PlanNode joinNode = childNode.getParent();

        Set<SingleElementSymbol> outputSymbols = new LinkedHashSet<SingleElementSymbol>((List<SingleElementSymbol>)childNode.getProperty(NodeConstants.Info.OUTPUT_COLS));
        
        int oldSize = outputSymbols.size();
        
        outputSymbols.addAll(expressions);
        
        boolean needsCorrection = outputSymbols.size() > oldSize;
                
        PlanNode sortNode = createSortNode(new ArrayList<SingleElementSymbol>(orderSymbols), outputSymbols);
        
        if (sourceNode.getType() == NodeConstants.Types.ACCESS) {
        	if (NewCalculateCostUtil.usesKey(sourceNode, expressions, metadata)) {
                joinNode.setProperty(joinNode.getFirstChild() == childNode ? NodeConstants.Info.IS_LEFT_DISTINCT : NodeConstants.Info.IS_RIGHT_DISTINCT, true);
        	}
	        if (attemptPush && RuleRaiseAccess.canRaiseOverSort(sourceNode, metadata, capFinder, sortNode)) {
	            sourceNode.getFirstChild().addAsParent(sortNode);
	            
	            if (needsCorrection) {
	                correctOutputElements(joinNode, outputSymbols, sortNode);
	            }
	            return true;
	        }
        }
        
        joinNode.setProperty(joinNode.getFirstChild() == childNode ? NodeConstants.Info.SORT_LEFT : NodeConstants.Info.SORT_RIGHT, SortOption.SORT);
        
        if (needsCorrection) {
            PlanNode projectNode = NodeFactory.getNewNode(NodeConstants.Types.PROJECT);
            projectNode.setProperty(NodeConstants.Info.PROJECT_COLS, new ArrayList(outputSymbols));
            childNode.addAsParent(projectNode);
            correctOutputElements(joinNode, outputSymbols, projectNode);
        }        
        return false;
    }

    private static PlanNode createSortNode(List<SingleElementSymbol> orderSymbols,
                                           Collection outputElements) {
        PlanNode sortNode = NodeFactory.getNewNode(NodeConstants.Types.SORT);
        sortNode.setProperty(NodeConstants.Info.SORT_ORDER, new OrderBy(orderSymbols));
        sortNode.setProperty(NodeConstants.Info.OUTPUT_COLS, new ArrayList(outputElements));
        return sortNode;
    }

    private static void correctOutputElements(PlanNode endNode,
                                              Collection outputElements,
                                              PlanNode startNode) {
        while (startNode != endNode) {
            startNode.setProperty(NodeConstants.Info.OUTPUT_COLS, new ArrayList(outputElements));
            startNode = startNode.getParent();
        }
    }
            
    /** 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "ImplementJoinStrategy"; //$NON-NLS-1$
    }

}
