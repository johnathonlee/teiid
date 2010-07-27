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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.teiid.api.exception.query.QueryMetadataException;
import org.teiid.api.exception.query.QueryPlannerException;
import org.teiid.core.TeiidComponentException;
import org.teiid.core.TeiidProcessingException;
import org.teiid.core.TeiidRuntimeException;
import org.teiid.core.types.DataTypeManager;
import org.teiid.core.util.Assertion;
import org.teiid.query.execution.QueryExecPlugin;
import org.teiid.query.metadata.QueryMetadataInterface;
import org.teiid.query.optimizer.relational.plantree.NodeConstants;
import org.teiid.query.optimizer.relational.plantree.NodeEditor;
import org.teiid.query.optimizer.relational.plantree.NodeFactory;
import org.teiid.query.optimizer.relational.plantree.PlanNode;
import org.teiid.query.processor.ProcessorPlan;
import org.teiid.query.resolver.util.AccessPattern;
import org.teiid.query.resolver.util.ResolverUtil;
import org.teiid.query.rewriter.QueryRewriter;
import org.teiid.query.sql.lang.Command;
import org.teiid.query.sql.lang.CompoundCriteria;
import org.teiid.query.sql.lang.Criteria;
import org.teiid.query.sql.lang.GroupBy;
import org.teiid.query.sql.lang.OrderBy;
import org.teiid.query.sql.lang.QueryCommand;
import org.teiid.query.sql.lang.Select;
import org.teiid.query.sql.lang.StoredProcedure;
import org.teiid.query.sql.symbol.Constant;
import org.teiid.query.sql.symbol.ElementSymbol;
import org.teiid.query.sql.symbol.Expression;
import org.teiid.query.sql.symbol.GroupSymbol;
import org.teiid.query.sql.symbol.SingleElementSymbol;
import org.teiid.query.sql.util.SymbolMap;
import org.teiid.query.sql.visitor.ElementCollectorVisitor;
import org.teiid.query.sql.visitor.ExpressionMappingVisitor;
import org.teiid.query.sql.visitor.GroupsUsedByElementsVisitor;
import org.teiid.query.util.ErrorMessageKeys;


public class FrameUtil {

    public static void convertFrame(PlanNode startNode, GroupSymbol oldGroup, Set<GroupSymbol> newGroups, Map symbolMap, QueryMetadataInterface metadata) 
        throws QueryPlannerException {

        PlanNode current = startNode;
        
        PlanNode endNode = NodeEditor.findParent(startNode.getType()==NodeConstants.Types.SOURCE?startNode.getParent():startNode, NodeConstants.Types.SOURCE);
        
        while(current != endNode) { 
            
            // Make translations as defined in node in each current node
            convertNode(current, oldGroup, newGroups, symbolMap, metadata);
            
            PlanNode parent = current.getParent(); 
            
            //check if this is not the first set op branch
            if (parent != null && parent.getType() == NodeConstants.Types.SET_OP && parent.getFirstChild() != current) {
                return;
            }

            // Move up the tree
            current = parent;
        }
                
        if(endNode == null) {
            return;
        }
        // Top of a frame - fix symbol mappings on endNode      
        SymbolMap parentSymbolMap = (SymbolMap) endNode.getProperty(NodeConstants.Info.SYMBOL_MAP);
        if(parentSymbolMap == null) {
            return;
        }
        
        for (Map.Entry<ElementSymbol, Expression> entry : parentSymbolMap.asUpdatableMap().entrySet()) {
			entry.setValue(convertExpression(entry.getValue(), symbolMap));
		}
        
    }
    
    static boolean canConvertAccessPatterns(PlanNode sourceNode) {
        List accessPatterns = (List)sourceNode.getProperty(NodeConstants.Info.ACCESS_PATTERNS);
        if (accessPatterns == null) {
            return true;
        }
        SymbolMap symbolMap = (SymbolMap)sourceNode.getProperty(NodeConstants.Info.SYMBOL_MAP);
        for (Iterator i = accessPatterns.iterator(); i.hasNext();) {
            AccessPattern ap = (AccessPattern)i.next();
            for (Iterator elems = ap.getUnsatisfied().iterator(); elems.hasNext();) {
                ElementSymbol symbol = (ElementSymbol)elems.next();
                Expression mapped = convertExpression(symbol, symbolMap.asMap());
                if (ElementCollectorVisitor.getElements(mapped, true).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 
     * @param symbolMap
     * @param node
     * @throws QueryPlannerException
     */
    private static void convertAccessPatterns(Map symbolMap,
                                              PlanNode node) throws QueryPlannerException {
        List<AccessPattern> accessPatterns = (List<AccessPattern>)node.getProperty(NodeConstants.Info.ACCESS_PATTERNS);
        if (accessPatterns != null) {
        	for (AccessPattern ap : accessPatterns) {
                Set<ElementSymbol> newElements = new HashSet<ElementSymbol>();
                for (Iterator elems = ap.getUnsatisfied().iterator(); elems.hasNext();) {
                    ElementSymbol symbol = (ElementSymbol)elems.next();
                    Expression mapped = convertExpression(symbol, symbolMap);
                    newElements.addAll(ElementCollectorVisitor.getElements(mapped, true));
                }
                ap.setUnsatisfied(newElements);
                Set<ElementSymbol> newHistory = new HashSet<ElementSymbol>();
                for (Iterator elems = ap.getCurrentElements().iterator(); elems.hasNext();) {
                    ElementSymbol symbol = (ElementSymbol)elems.next();
                    Expression mapped = convertExpression(symbol, symbolMap);
                    newHistory.addAll(ElementCollectorVisitor.getElements(mapped, true));
                }
                ap.addElementHistory(newHistory);
            }
            Collections.sort(accessPatterns);
        }
    }
        
    // If newGroup == null, this will be performing a straight symbol swap - that is, 
    // an oldGroup is undergoing a name change and that is the only difference in the 
    // symbols.  In that case, some additional work can be done because we can assume 
    // that an oldElement isn't being replaced by an expression using elements from 
    // multiple new groups.  
    static void convertNode(PlanNode node, GroupSymbol oldGroup, Set<GroupSymbol> newGroups, Map symbolMap, QueryMetadataInterface metadata)
        throws QueryPlannerException {

        // Convert expressions from correlated subquery references;
        List<SymbolMap> refMaps = node.getAllReferences();
        LinkedList<Expression> correlatedExpression = new LinkedList<Expression>(); 
        for (SymbolMap refs : refMaps) {
        	for (Map.Entry<ElementSymbol, Expression> ref : refs.asUpdatableMap().entrySet()) {
	            Expression expr = ref.getValue();
	            Expression convertedExpr = convertExpression(expr, symbolMap);
	            ref.setValue(convertedExpr);
	            correlatedExpression.add(convertedExpr);
	        }
        }

        // Update groups for current node   
        Set<GroupSymbol> groups = node.getGroups();  
        
        boolean hasOld = groups.remove(oldGroup);

        int type = node.getType();
        
        boolean singleMapping = newGroups != null && newGroups.size() == 1;
        
        if(singleMapping) {
            if (!hasOld) {
                return;
            }
            groups.addAll(newGroups);
        } else if ((type & (NodeConstants.Types.ACCESS | NodeConstants.Types.JOIN | NodeConstants.Types.SOURCE)) == type) {
        	if (newGroups != null) {
        		groups.addAll(newGroups);
        	}
        } else {
        	groups.clear();
        }
        
    	groups.addAll(GroupsUsedByElementsVisitor.getGroups(correlatedExpression));
        
        if(type == NodeConstants.Types.SELECT) { 
            Criteria crit = (Criteria) node.getProperty(NodeConstants.Info.SELECT_CRITERIA);
            crit = convertCriteria(crit, symbolMap, metadata);
            node.setProperty(NodeConstants.Info.SELECT_CRITERIA, crit);
            
            if (!singleMapping) {
                GroupsUsedByElementsVisitor.getGroups(crit, groups);
            }
                            
        } else if(type == NodeConstants.Types.PROJECT) {                    
            List<SingleElementSymbol> projectedSymbols = (List<SingleElementSymbol>)node.getProperty(NodeConstants.Info.PROJECT_COLS);
            Select select = new Select(projectedSymbols);
            ExpressionMappingVisitor.mapExpressions(select, symbolMap);
            node.setProperty(NodeConstants.Info.PROJECT_COLS, select.getSymbols());
            if (!singleMapping) {
                GroupsUsedByElementsVisitor.getGroups(select, groups);
            }
        } else if(type == NodeConstants.Types.JOIN) { 
            // Convert join criteria property
            List<Criteria> joinCrits = (List<Criteria>) node.getProperty(NodeConstants.Info.JOIN_CRITERIA);
            if(joinCrits != null && !joinCrits.isEmpty()) {
            	Criteria crit = new CompoundCriteria(joinCrits);
            	crit = convertCriteria(crit, symbolMap, metadata);
            	if (crit instanceof CompoundCriteria) {
            		node.setProperty(NodeConstants.Info.JOIN_CRITERIA, ((CompoundCriteria)crit).getCriteria());
            	} else {
            		joinCrits = new ArrayList<Criteria>();
            		joinCrits.add(crit);
            		node.setProperty(NodeConstants.Info.JOIN_CRITERIA, joinCrits);
            	}
            }
            
            convertAccessPatterns(symbolMap, node);
        
        } else if(type == NodeConstants.Types.SORT) { 
        	OrderBy orderBy = (OrderBy)node.getProperty(NodeConstants.Info.SORT_ORDER);
            ExpressionMappingVisitor.mapExpressions(orderBy, symbolMap);
            if (!singleMapping) {
                GroupsUsedByElementsVisitor.getGroups(orderBy, groups);
            }
        } else if(type == NodeConstants.Types.GROUP) {  
        	List<SingleElementSymbol> groupCols = (List<SingleElementSymbol>)node.getProperty(NodeConstants.Info.GROUP_COLS);
            if (groupCols != null) {
                GroupBy groupBy= new GroupBy(groupCols);
                ExpressionMappingVisitor.mapExpressions(groupBy, symbolMap);
                node.setProperty(NodeConstants.Info.GROUP_COLS, groupBy.getSymbols());
                if (!singleMapping) {
                    GroupsUsedByElementsVisitor.getGroups(groupCols, groups);
                }
            }               
        } else if (type == NodeConstants.Types.SOURCE || type == NodeConstants.Types.ACCESS) {
            convertAccessPatterns(symbolMap, node);
        }
    }
    
    private static Expression convertExpression(Expression expression, Map symbolMap) {
        
        if (expression == null || expression instanceof Constant) {
            return expression;
        }
        
        if(expression instanceof SingleElementSymbol) { 
            Expression mappedSymbol = (Expression) symbolMap.get(expression);
            if (mappedSymbol != null) {
                return mappedSymbol;
            }
            return expression;
        }
                
        ExpressionMappingVisitor.mapExpressions(expression, symbolMap);
        
        return expression;
    }   
        
    static Criteria convertCriteria(Criteria criteria, final Map symbolMap, QueryMetadataInterface metadata)
        throws QueryPlannerException {

        ExpressionMappingVisitor.mapExpressions(criteria, symbolMap);
        
        // Simplify criteria if possible
        try {
            return QueryRewriter.rewriteCriteria(criteria, null, null, metadata);
        } catch(TeiidProcessingException e) {
            throw new QueryPlannerException(e, QueryExecPlugin.Util.getString(ErrorMessageKeys.OPTIMIZER_0023, criteria));
        } catch (TeiidComponentException e) {
        	throw new TeiidRuntimeException(e);
        }
    }

    /**
     * creates a symbol map of elements in oldGroup mapped to corresponding elements in newGroup
     * 
     * if newGroup is null, then a mapping of oldGroup elements to null constants will be returned
     *  
     * @param oldGroup
     * @param newGroup
     * @param metadata
     * @return
     * @throws QueryMetadataException
     * @throws TeiidComponentException
     */
    public static Map<ElementSymbol, Expression> buildSymbolMap(GroupSymbol oldGroup, GroupSymbol newGroup, QueryMetadataInterface metadata) 
        throws QueryMetadataException, TeiidComponentException {

        String newGroupName = null;
        if (newGroup != null) {
            newGroupName = newGroup.getName();
        }
        Map map = new HashMap();    

        // Get elements of old group
        List elements = ResolverUtil.resolveElementsInGroup(oldGroup, metadata);
        
        Iterator iter = elements.iterator();
        while(iter.hasNext()) {
            ElementSymbol oldElementSymbol = (ElementSymbol)iter.next();
            
            Expression symbol = null;
            if (newGroup != null) {
                String newFullName = metadata.getFullElementName(newGroupName, oldElementSymbol.getShortName());
                ElementSymbol newElementSymbol = new ElementSymbol(newFullName);
                newElementSymbol.setGroupSymbol(newGroup);
                newElementSymbol.setMetadataID(oldElementSymbol.getMetadataID());
                String elementType = metadata.getElementType(newElementSymbol.getMetadataID());
                newElementSymbol.setType(DataTypeManager.getDataTypeClass(elementType));
                symbol = newElementSymbol;
            } else {
                symbol = new Constant(null, oldElementSymbol.getType());
            }
            
            // Update map
            map.put(oldElementSymbol, symbol);                
        }
        
        return map;
    }
    
    /**
     * Find the SOURCE, SET_OP, JOIN, or NULL node that originates the given groups (typically from a criteria node).
     * In the case of join nodes the best fit will be found rather than just the first
     * join node that contains all the groups. 
     *
     * Returns null if the originating node cannot be found.
     * 
     * @param root
     * @param groups
     * @return
     */
    static PlanNode findOriginatingNode(PlanNode root, Set<GroupSymbol> groups) {
        return findOriginatingNode(root, groups, false);
    }
    
    /**
     * 
     * Find the ACCESS, SOURCE, SET_OP, JOIN, or NULL node that originates the given groups, but will stop at the
     * first join node rather than searching for the best fit.
     * 
     * Returns null if the originating node cannot be found.
     * 
     * @param root
     * @param groups
     * @return
     */
    public static PlanNode findJoinSourceNode(PlanNode root) {
        return findOriginatingNode(root, root.getGroups(), true);
    }
    
    private static PlanNode findOriginatingNode(PlanNode root, Set<GroupSymbol> groups, boolean joinSource) {
        boolean containsGroups = false;
        
    	if(root.getType() == NodeConstants.Types.NULL || root.getType() == NodeConstants.Types.SOURCE 
                        || root.getType() == NodeConstants.Types.JOIN || root.getType() == NodeConstants.Types.SET_OP ||
                        (joinSource && root.getType() == NodeConstants.Types.ACCESS)) {
    	    
            //if there are no groups then the first possible match is the one we want
            if(groups.isEmpty()) {
               return root;
            }
            
            containsGroups = root.getGroups().containsAll(groups);
            
            if (containsGroups) {
                //if this is a group, source, or set op we're done, else if join make sure the groups match
                if (root.getType() != NodeConstants.Types.JOIN || joinSource || root.getGroups().size() == groups.size()) {
                    return root;
                }
            }
            
            //check to see if a recursion is not necessary
            if (root.getType() != NodeConstants.Types.JOIN || joinSource || !containsGroups) {
                return null;
            }
        }
    
    	// Check children, left to right
    	for (PlanNode child : root.getChildren()) {
    		PlanNode found = findOriginatingNode(child, groups, joinSource);
    		if(found != null) {
    			return found;
    		}
    	}
        
        //look for best fit instead after visiting children
        if(root.getType() == NodeConstants.Types.JOIN && containsGroups) {
            return root;
        }
        
    	// Not here
    	return null;
    }
    
    /**
     * Replaces the given node with a NULL node.  This will also preserve
     * the groups that originate under the node on the NULL node 
     *  
     * @param node
     */
    static void replaceWithNullNode(PlanNode node) {
        PlanNode nullNode = NodeFactory.getNewNode(NodeConstants.Types.NULL);
        PlanNode source = FrameUtil.findJoinSourceNode(node);
        if (source != null) {
            nullNode.addGroups(source.getGroups());
        }
        node.getParent().replaceChild(node, nullNode);
    }

    /**
     * Look for SOURCE node either one or two steps below the access node.  Typically
     * these options look like ACCESS-SOURCE or ACCESS-PROJECT-SOURCE.
     * 
     * @param accessNode
     * @return
     */
    static ProcessorPlan getNestedPlan(PlanNode accessNode) {
        PlanNode sourceNode = accessNode.getFirstChild(); 
        if (sourceNode == null) {
        	return null;
        }
        if(sourceNode.getType() != NodeConstants.Types.SOURCE) {
            sourceNode = sourceNode.getFirstChild();
        }
        if(sourceNode != null && sourceNode.getType() == NodeConstants.Types.SOURCE) {
            return (ProcessorPlan) sourceNode.getProperty(NodeConstants.Info.PROCESSOR_PLAN);
        }
        return null;            
    }

    /**
     * Look for SOURCE node either one or two steps below the access node.  Typically
     * these options look like ACCESS-SOURCE or ACCESS-PROJECT-SOURCE.
     *
     * @param accessNode
     * @return The actual stored procedure
     */
    static Command getNonQueryCommand(PlanNode node) {
        PlanNode sourceNode = node.getFirstChild();
        if (sourceNode == null) {
        	return null;
        }
        if(sourceNode.getType() != NodeConstants.Types.SOURCE) {
            sourceNode = sourceNode.getFirstChild();
        } 
        if(sourceNode != null && sourceNode.getType() == NodeConstants.Types.SOURCE) {
            Command command = (Command) sourceNode.getProperty(NodeConstants.Info.VIRTUAL_COMMAND);
            if(! (command instanceof QueryCommand)) {
                return command;
            }                
        }
        return null;
    }
    
    static boolean isProcedure(PlanNode projectNode) {
        if(projectNode.getType() == NodeConstants.Types.PROJECT && projectNode.getChildCount() > 0) {
            PlanNode accessNode = projectNode.getFirstChild();
            Command command = getNonQueryCommand(accessNode);
            return command instanceof StoredProcedure;
        }
        return false;
    }
    
    /**
     * Finds the closest project columns in the current frame
     */
    static List<SingleElementSymbol> findTopCols(PlanNode node) {
    	PlanNode project = NodeEditor.findNodePreOrder(node, NodeConstants.Types.PROJECT, NodeConstants.Types.SOURCE);
        if (project == null) {
            project = NodeEditor.findParent(node, NodeConstants.Types.PROJECT, NodeConstants.Types.SOURCE);
        }
        if (project != null) {
            return (List<SingleElementSymbol>)project.getProperty(NodeConstants.Info.PROJECT_COLS);
        }
        Assertion.failed("no top cols in frame"); //$NON-NLS-1$
        return null;
    }
    
    public static boolean isOrderedLimit(PlanNode node) {
    	return node.getType() == NodeConstants.Types.TUPLE_LIMIT && NodeEditor.findNodePreOrder(node, NodeConstants.Types.SORT, NodeConstants.Types.PROJECT | NodeConstants.Types.SET_OP) != null;
    }

}
