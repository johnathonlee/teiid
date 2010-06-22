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

import org.teiid.api.exception.query.ExpressionEvaluationException;
import org.teiid.api.exception.query.QueryPlannerException;
import org.teiid.common.buffer.BlockedException;
import org.teiid.core.TeiidComponentException;
import org.teiid.query.analysis.AnalysisRecord;
import org.teiid.query.eval.Evaluator;
import org.teiid.query.metadata.QueryMetadataInterface;
import org.teiid.query.optimizer.capabilities.CapabilitiesFinder;
import org.teiid.query.optimizer.relational.OptimizerRule;
import org.teiid.query.optimizer.relational.RuleStack;
import org.teiid.query.optimizer.relational.plantree.NodeConstants;
import org.teiid.query.optimizer.relational.plantree.NodeEditor;
import org.teiid.query.optimizer.relational.plantree.PlanNode;
import org.teiid.query.sql.lang.Criteria;
import org.teiid.query.sql.visitor.EvaluatableVisitor;
import org.teiid.query.util.CommandContext;


/**
 * Removes phantom and TRUE or FALSE criteria
 */
public final class RuleCleanCriteria implements OptimizerRule {

    /**
     * @see OptimizerRule#execute(PlanNode, QueryMetadataInterface, RuleStack)
     */
    public PlanNode execute(PlanNode plan, QueryMetadataInterface metadata, CapabilitiesFinder capFinder, RuleStack rules, AnalysisRecord analysisRecord, CommandContext context)
        throws QueryPlannerException, TeiidComponentException {

        boolean pushRaiseNull = false;
        
        for (PlanNode critNode : NodeEditor.findAllNodes(plan, NodeConstants.Types.SELECT)) {
            
            if (critNode.hasBooleanProperty(NodeConstants.Info.IS_PHANTOM)) {
                NodeEditor.removeChildNode(critNode.getParent(), critNode);
                continue;
            }
            
            //TODO: remove dependent set criteria that has not been meaningfully pushed from its parent join
            
            if (critNode.hasBooleanProperty(NodeConstants.Info.IS_HAVING) || critNode.getGroups().size() != 0) {
                continue;
            }
            
            Criteria crit = (Criteria)critNode.getProperty(NodeConstants.Info.SELECT_CRITERIA);
            //if not evaluatable, just move on to the next criteria
            if (!EvaluatableVisitor.isFullyEvaluatable(crit, true)) {
                continue;
            }
            //if evaluatable
            try {
                boolean eval = Evaluator.evaluate(crit);
                if(eval) {
                    NodeEditor.removeChildNode(critNode.getParent(), critNode);
                } else {
                    FrameUtil.replaceWithNullNode(critNode);
                    pushRaiseNull = true;
                }
            //none of the following exceptions should ever occur
            } catch(BlockedException e) {
                throw new TeiidComponentException(e);
            } catch (ExpressionEvaluationException e) {
                throw new TeiidComponentException(e);
            } 
        } 
        
        if (pushRaiseNull) {
            rules.push(RuleConstants.RAISE_NULL);
        }

        return plan;        
    }
    
    public String toString() {
        return "CleanCriteria"; //$NON-NLS-1$
    }

}
