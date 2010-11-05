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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.teiid.api.exception.query.QueryMetadataException;
import org.teiid.core.TeiidComponentException;
import org.teiid.query.function.metadata.FunctionMethod;
import org.teiid.query.metadata.QueryMetadataInterface;
import org.teiid.query.optimizer.relational.RelationalPlanner;
import org.teiid.query.optimizer.relational.plantree.NodeConstants;
import org.teiid.query.optimizer.relational.plantree.PlanNode;
import org.teiid.query.resolver.util.AccessPattern;
import org.teiid.query.sql.lang.CompareCriteria;
import org.teiid.query.sql.lang.CompoundCriteria;
import org.teiid.query.sql.lang.Criteria;
import org.teiid.query.sql.symbol.ElementSymbol;
import org.teiid.query.sql.symbol.Expression;
import org.teiid.query.sql.symbol.Function;
import org.teiid.query.sql.symbol.GroupSymbol;
import org.teiid.query.sql.visitor.ElementCollectorVisitor;
import org.teiid.query.sql.visitor.FunctionCollectorVisitor;
import org.teiid.query.sql.visitor.GroupsUsedByElementsVisitor;


/**
 *  A join region is a set of cross and inner joins whose ordering is completely interchangeable.
 *  
 *  It can be conceptually thought of as:
 *     Criteria node some combination of groups A, B, C
 *     Criteria node some combination of groups A, B, C
 *     ...
 *     Join
 *       JoinSourceA
 *       JoinSourceB
 *       JoinSourceC
 *  
 *  A full binary join tree is then constructed out of this join region such that all of the
 *  criteria is pushed to its lowest point.
 *  
 */
class JoinRegion {
    
    private PlanNode joinRoot;
    
    public static final int UNKNOWN_TUPLE_EST = 100000;
    
    private LinkedHashMap<PlanNode, PlanNode> dependentJoinSourceNodes = new LinkedHashMap<PlanNode, PlanNode>();
    private LinkedHashMap<PlanNode, PlanNode> joinSourceNodes = new LinkedHashMap<PlanNode, PlanNode>();
        
    private List<PlanNode> dependentCritieraNodes = new ArrayList<PlanNode>();
    private List<PlanNode> criteriaNodes = new ArrayList<PlanNode>();
    
    private List<Collection<AccessPattern>> unsatisfiedAccessPatterns = new LinkedList<Collection<AccessPattern>>();
    private boolean containsNestedTable;
    
    private Map<ElementSymbol, Set<Collection<GroupSymbol>>> dependentCriteriaElements;
    private Map<PlanNode, Set<PlanNode>> critieriaToSourceMap;
    
    public PlanNode getJoinRoot() {
        return joinRoot;
    }
    
    public void setContainsNestedTable(boolean containsNestedTable) {
		this.containsNestedTable = containsNestedTable;
	}
    
    public boolean containsNestedTable() {
		return containsNestedTable;
	}
    
    public List<Collection<AccessPattern>> getUnsatisfiedAccessPatterns() {
        return unsatisfiedAccessPatterns;
    }
    
    public Map<PlanNode, PlanNode> getJoinSourceNodes() {
        return joinSourceNodes;
    }

    public Map<PlanNode, PlanNode> getDependentJoinSourceNodes() {
        return dependentJoinSourceNodes;
    }
    
    public List<PlanNode> getCriteriaNodes() {
        return criteriaNodes;
    }
    
    public List<PlanNode> getDependentCriteriaNodes() {
        return dependentCritieraNodes;
    }
    
    public Map<ElementSymbol, Set<Collection<GroupSymbol>>> getDependentCriteriaElements() {
        return this.dependentCriteriaElements;
    }

    public Map<PlanNode, Set<PlanNode>> getCritieriaToSourceMap() {
        return this.critieriaToSourceMap;
    }

    public void addJoinSourceNode(PlanNode sourceNode) {
        PlanNode root = sourceNode;
        while (root.getParent() != null && root.getParent().getType() == NodeConstants.Types.SELECT) {
            root = root.getParent();
        }
        if (sourceNode.hasCollectionProperty(NodeConstants.Info.ACCESS_PATTERNS)) {
            Collection<AccessPattern> aps = (Collection<AccessPattern>)sourceNode.getProperty(NodeConstants.Info.ACCESS_PATTERNS);
            unsatisfiedAccessPatterns.add(aps);
            dependentJoinSourceNodes.put(sourceNode, root);
        } else {
            joinSourceNodes.put(sourceNode, root);
        }
        
        if (joinRoot == null) {
            joinRoot = root;
        }
    }

    public void addParentCriteria(PlanNode sourceNode) {
        PlanNode parent = sourceNode.getParent();
        while (parent != null && parent.getType() == NodeConstants.Types.SELECT) {
            criteriaNodes.add(parent);
            sourceNode = parent;
            parent = parent.getParent();
        }
        if (joinRoot == null) {
            joinRoot = sourceNode;
        }
    }
                    
    public void addJoinCriteriaList(List<? extends Criteria> joinCriteria) {
        if (joinCriteria == null || joinCriteria.isEmpty()) {
            return;
        }
        for (Criteria crit : joinCriteria) {
            criteriaNodes.add(RelationalPlanner.createSelectNode(crit, false));
        }
    }
    
    /**
     * This will rebuild the join tree starting at the join root.
     * 
     * A left linear tree will be constructed out of the ordering of the 
     * join sources.
     * 
     * Criteria nodes are simply placed at the top of the join region in order
     * to be pushed by rule PushSelectSriteria.
     * 
     */
    public void reconstructJoinRegoin() {
        LinkedHashMap<PlanNode, PlanNode> combined = new LinkedHashMap<PlanNode, PlanNode>(joinSourceNodes);
        combined.putAll(dependentJoinSourceNodes);
        
        PlanNode root = null;
        
        if (combined.size() < 2) {
            root = combined.values().iterator().next();
        } else {
            root = RulePlanJoins.createJoinNode();
        
            for (Map.Entry<PlanNode, PlanNode> entry : combined.entrySet()) {
                PlanNode joinSourceRoot = entry.getValue();
                if (root.getChildCount() == 2) {
                    PlanNode parentJoin = RulePlanJoins.createJoinNode();
                    parentJoin.addFirstChild(root);
                    parentJoin.addGroups(root.getGroups());
                    root = parentJoin;
                }
                root.addLastChild(joinSourceRoot);
                root.addGroups(entry.getKey().getGroups());
            }
        }
        LinkedList<PlanNode> criteria = new LinkedList<PlanNode>(dependentCritieraNodes);
        criteria.addAll(criteriaNodes);

        PlanNode parent = this.joinRoot.getParent();
        
        boolean isLeftChild = parent.getFirstChild() == this.joinRoot;

        parent.removeChild(joinRoot);
        
        for (PlanNode critNode : criteria) {
            critNode.removeFromParent();
            critNode.removeAllChildren();
            critNode.addFirstChild(root);
            root = critNode;
            critNode.removeProperty(NodeConstants.Info.IS_COPIED);
            critNode.removeProperty(NodeConstants.Info.EST_CARDINALITY);
        }

        if (isLeftChild) {
            parent.addFirstChild(root);
        } else {
            parent.addLastChild(root);
        }
        this.joinRoot = root;
    }
    
    /**
     * Will provide an estimate of cost by summing the estimated tuples flowing through
     * each intermediate join. 
     *  
     * @param joinOrder
     * @param metadata
     * @return
     * @throws TeiidComponentException 
     * @throws QueryMetadataException 
     */
    public double scoreRegion(Object[] joinOrder, QueryMetadataInterface metadata) throws QueryMetadataException, TeiidComponentException {
        List<Map.Entry<PlanNode, PlanNode>> joinSourceEntries = new ArrayList<Map.Entry<PlanNode, PlanNode>>(joinSourceNodes.entrySet());
        double totalIntermediatCost = 0;
        double cost = 1;
        
        HashSet<PlanNode> criteria = new HashSet<PlanNode>(this.criteriaNodes);
        HashSet<GroupSymbol> groups = new HashSet<GroupSymbol>(this.joinSourceNodes.size());
        
        for (int i = 0; i < joinOrder.length; i++) {
            Integer source = (Integer)joinOrder[i];
            
            Map.Entry<PlanNode, PlanNode> entry = joinSourceEntries.get(source.intValue());
            PlanNode joinSourceRoot = entry.getValue();
            
            //check to make sure that this group ordering satisfies the access patterns
            if (!this.unsatisfiedAccessPatterns.isEmpty() || this.containsNestedTable) {
                PlanNode joinSource = entry.getKey();
                
                Collection<GroupSymbol> requiredGroups = (Collection<GroupSymbol>)joinSource.getProperty(NodeConstants.Info.REQUIRED_ACCESS_PATTERN_GROUPS);
                
                if (requiredGroups != null && !groups.containsAll(requiredGroups)) {
                    return Double.MAX_VALUE;
                }
            }
            
            groups.addAll(joinSourceRoot.getGroups());
            
            float sourceCost = ((Float)joinSourceRoot.getProperty(NodeConstants.Info.EST_CARDINALITY)).floatValue();
            
            List<PlanNode> applicableCriteria = null;
            
            if (!criteria.isEmpty() && i > 0) {
                applicableCriteria = getJoinCriteriaForGroups(groups, criteria);
            }
            
        	if (sourceCost == NewCalculateCostUtil.UNKNOWN_VALUE) {
        		sourceCost = UNKNOWN_TUPLE_EST;
                if (applicableCriteria != null && !applicableCriteria.isEmpty()) {
                	CompoundCriteria cc = new CompoundCriteria();
                	for (PlanNode planNode : applicableCriteria) {
    					cc.addCriteria((Criteria) planNode.getProperty(NodeConstants.Info.SELECT_CRITERIA));
    				}
                	sourceCost = (float)cost;
                	criteria.removeAll(applicableCriteria);
	            	applicableCriteria = null;
            		if (NewCalculateCostUtil.usesKey(cc, metadata)) {
    	            	sourceCost = Math.min(UNKNOWN_TUPLE_EST, sourceCost * Math.min(NewCalculateCostUtil.UNKNOWN_JOIN_SCALING, sourceCost));
            		} else {
    	            	sourceCost = Math.min(UNKNOWN_TUPLE_EST, sourceCost * Math.min(NewCalculateCostUtil.UNKNOWN_JOIN_SCALING * 2, sourceCost));
            		}
                }
            } else if (Double.isInfinite(sourceCost) || Double.isNaN(sourceCost)) {
            	return Double.MAX_VALUE;
            }
        
            cost *= sourceCost;
            
            if (applicableCriteria != null) {
                for (PlanNode criteriaNode : applicableCriteria) {
                    float filter = ((Float)criteriaNode.getProperty(NodeConstants.Info.EST_SELECTIVITY)).floatValue();
                    
                    cost *= filter;
                }
                
                criteria.removeAll(applicableCriteria);
            }
            totalIntermediatCost += cost;
        }
        
        return totalIntermediatCost;
    }
    
    /**
     *  Returns true if every element in an unsatisfied access pattern can be satisfied by the current join criteria
     *  This does not necessarily mean that a join tree will be successfully created
     */
    public boolean isSatisfiable() {
    	for (Collection<AccessPattern> accessPatterns : getUnsatisfiedAccessPatterns()) {
            boolean matchedAll = false;
            for (AccessPattern ap : accessPatterns) {
                if (dependentCriteriaElements.keySet().containsAll(ap.getUnsatisfied())) {
                    matchedAll = true;
                    break;
                }
            }
            if (!matchedAll) {
                return false;
            }
        }
        
        return true;
    }

    public void initializeCostingInformation(QueryMetadataInterface metadata) throws QueryMetadataException, TeiidComponentException {
    	for (PlanNode node : joinSourceNodes.values()) {
            NewCalculateCostUtil.computeCostForTree(node, metadata);
        }
        
        estimateCriteriaSelectivity(metadata);        
    }

    /** 
     * @param metadata
     * @throws QueryMetadataException
     * @throws TeiidComponentException
     */
    private void estimateCriteriaSelectivity(QueryMetadataInterface metadata) throws QueryMetadataException,
                                                                             TeiidComponentException {
        for (PlanNode node : criteriaNodes) {
            Criteria crit = (Criteria)node.getProperty(NodeConstants.Info.SELECT_CRITERIA);
            
            float[] baseCosts = new float[] {100, 10000, 1000000};
            
            float filterValue = 0;
            
            for (int j = 0; j < baseCosts.length; j++) {
                float filter = NewCalculateCostUtil.recursiveEstimateCostOfCriteria(baseCosts[j], node, crit, metadata);
                
                filterValue += filter/baseCosts[j];
            }
            
            filterValue /= baseCosts.length;
   
            node.setProperty(NodeConstants.Info.EST_SELECTIVITY, new Float(filterValue));
        }
    }
    
    /**
     *  Initializes information on the joinRegion about dependency information, etc.
     *  
     *  TODO: assumptions are made here about how dependent criteria must look that are a little restrictive
     */
    public void initializeJoinInformation() {
        critieriaToSourceMap = new HashMap<PlanNode, Set<PlanNode>>();
                
        LinkedList<PlanNode> crits = new LinkedList<PlanNode>(criteriaNodes);
        crits.addAll(dependentCritieraNodes);
        
        LinkedHashMap<PlanNode, PlanNode> source = new LinkedHashMap<PlanNode, PlanNode>(joinSourceNodes);
        source.putAll(dependentJoinSourceNodes);
        
        for (PlanNode critNode : crits) {
        	for (GroupSymbol group : critNode.getGroups()) {
        		for (PlanNode node : source.keySet()) {
                    if (node.getGroups().contains(group)) {
                        Set<PlanNode> sources = critieriaToSourceMap.get(critNode);
                        if (sources == null) {
                            sources = new HashSet<PlanNode>();
                            critieriaToSourceMap.put(critNode, sources);
                        }
                        sources.add(node);
                        break;
                    }
                }
            }
        }            

        if (unsatisfiedAccessPatterns.isEmpty()) {
            return;
        }
        
        Map<GroupSymbol, PlanNode> dependentGroupToSourceMap = new HashMap<GroupSymbol, PlanNode>();
        
        for (PlanNode node : dependentJoinSourceNodes.keySet()) {
        	for (GroupSymbol symbol : node.getGroups()) {
                dependentGroupToSourceMap.put(symbol, node);
            }
        }
        
        for (Iterator<PlanNode> i = getCriteriaNodes().iterator(); i.hasNext();) {
            PlanNode node = i.next();
            
            for (GroupSymbol symbol : node.getGroups()) {
                if (dependentGroupToSourceMap.containsKey(symbol)) {
                    i.remove();
                    dependentCritieraNodes.add(node);
                    break;
                }
            }
        }
        
        dependentCriteriaElements = new HashMap<ElementSymbol, Set<Collection<GroupSymbol>>>();
        
        for (PlanNode critNode : dependentCritieraNodes) {
            Criteria crit = (Criteria)critNode.getProperty(NodeConstants.Info.SELECT_CRITERIA);
            if(!(crit instanceof CompareCriteria)) {
                continue;
            }
            CompareCriteria compCrit = (CompareCriteria) crit;                
            if(compCrit.getOperator() != CompareCriteria.EQ) {
                continue;
            }
            CompareCriteria compareCriteria = (CompareCriteria)crit;
            //this may be a proper dependent join criteria
            Collection<ElementSymbol>[] critElements = new Collection[2];
            critElements[0] = ElementCollectorVisitor.getElements(compareCriteria.getLeftExpression(), true);
            if (critElements[0].isEmpty()) {
            	continue;
            }
            critElements[1] = ElementCollectorVisitor.getElements(compareCriteria.getRightExpression(), true);
            if (critElements[1].isEmpty()) {
            	continue;
            }
            for (int expr = 0; expr < critElements.length; expr++) {
                //simplifying assumption that there will be a single element on the dependent side
                if (critElements[expr].size() != 1) {
                    continue;
                }
                ElementSymbol elem = critElements[expr].iterator().next();
                if (!dependentGroupToSourceMap.containsKey(elem.getGroupSymbol())) {
                    continue;
                }
                //this is also a simplifying assumption.  don't consider criteria that can't be pushed
                if (containsFunctionsThatCannotBePushed(expr==0?compareCriteria.getRightExpression():compareCriteria.getLeftExpression())) {
                    continue;
                }
                Set<Collection<GroupSymbol>> independentGroups = dependentCriteriaElements.get(elem);
                if (independentGroups == null) {
                    independentGroups = new HashSet<Collection<GroupSymbol>>();
                    dependentCriteriaElements.put(elem, independentGroups);
                }
                //set the other side as independent elements
                independentGroups.add(GroupsUsedByElementsVisitor.getGroups(critElements[(expr+1)%2]));
            }
        }
    }
    
    /**
     * Returns true if the expression is, or contains, any functions that cannot be pushed 
     * down to the source
     * @param expression
     * @return
     * @since 4.2
     */
    private static boolean containsFunctionsThatCannotBePushed(Expression expression) {
        Iterator functions = FunctionCollectorVisitor.getFunctions(expression, true).iterator();
        while (functions.hasNext()) {
            Function function = (Function)functions.next();
            if (function.getFunctionDescriptor().getPushdown() == FunctionMethod.CANNOT_PUSHDOWN) {
                return true;
            }
        }
        return false;
    }
    
    public List<PlanNode> getJoinCriteriaForGroups(Set<GroupSymbol> groups) {
        return getJoinCriteriaForGroups(groups, getCriteriaNodes());
    }
    
    //TODO: this should be better than a linear search
    protected List<PlanNode> getJoinCriteriaForGroups(Set<GroupSymbol> groups, Collection<PlanNode> nodes) {
        List<PlanNode> result = new LinkedList<PlanNode>();
        
        for (PlanNode critNode : nodes) {
            if (groups.containsAll(critNode.getGroups())) {
                result.add(critNode);
            }
        }
        
        return result;
    }
    
    public void changeJoinOrder(Object[] joinOrder) {
        List<Map.Entry<PlanNode, PlanNode>> joinSourceEntries = new ArrayList<Map.Entry<PlanNode, PlanNode>>(joinSourceNodes.entrySet());
        
        for (int i = 0; i < joinOrder.length; i++) {
            Integer source = (Integer)joinOrder[i];
            
            Map.Entry<PlanNode, PlanNode> entry = joinSourceEntries.get(source.intValue());
            
            this.joinSourceNodes.remove(entry.getKey());
            this.joinSourceNodes.put(entry.getKey(), entry.getValue());
        }
    }

}