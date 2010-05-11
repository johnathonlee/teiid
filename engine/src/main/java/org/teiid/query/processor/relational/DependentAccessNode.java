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

import org.teiid.core.TeiidComponentException;
import org.teiid.core.TeiidProcessingException;
import org.teiid.core.util.Assertion;
import org.teiid.query.sql.lang.Command;
import org.teiid.query.sql.lang.Criteria;
import org.teiid.query.sql.lang.Query;


/**
 * Takes a query with 1 or more dependent sets from 1 or more sources and creates a series of commands. Dependent sets from the
 * same source are treated as a special case. If there are multiple batches from that source, we will create replacement criteria
 * in lock step - rather than forming the full cartesian product space. This implementation assumes that ordering will be
 * respected above the access node and that the incoming dependent tuple values are already sorted.
 */
public class DependentAccessNode extends AccessNode {

    //plan state
    private int maxSetSize;

    //processing state
    private DependentCriteriaProcessor criteriaProcessor;
    private Criteria dependentCrit;
    private boolean sort = true;

    public DependentAccessNode(int nodeID) {
        super(nodeID);
    }

    /**
     * @see org.teiid.query.processor.relational.AccessNode#close()
     */
    public void closeDirect() {
        super.closeDirect();

        if (criteriaProcessor != null) {
            criteriaProcessor.close();
        }
    }

    public void reset() {
        super.reset();
        criteriaProcessor = null;
        dependentCrit = null;
        sort = true;
    }

    public Object clone() {
        DependentAccessNode clonedNode = new DependentAccessNode(super.getID());
        clonedNode.maxSetSize = this.maxSetSize;
        super.copy(this, clonedNode);
        return clonedNode;
    }

    /**
     * @return Returns the maxSize.
     */
    public int getMaxSetSize() {
        return this.maxSetSize;
    }

    /**
     * @param maxSize
     *            The maxSize to set.
     */
    public void setMaxSetSize(int maxSize) {
        this.maxSetSize = maxSize;
    }
    
    /**
     * @see org.teiid.query.processor.relational.AccessNode#prepareNextCommand(org.teiid.query.sql.lang.Command)
     */
    protected boolean prepareNextCommand(Command atomicCommand) throws TeiidComponentException, TeiidProcessingException {

        Assertion.assertTrue(atomicCommand instanceof Query);

        Query query = (Query)atomicCommand;

        if (this.criteriaProcessor == null) {
            this.criteriaProcessor = new DependentCriteriaProcessor(this.maxSetSize, this, query.getCriteria());
        }
        
        if (this.dependentCrit == null) {
            dependentCrit = criteriaProcessor.prepareCriteria();
        }
        
        query.setCriteria(dependentCrit);
        
        if (sort && query.getOrderBy() != null && criteriaProcessor.hasNextCommand()) {
            RelationalNode parent = this.getParent();
            while (parent != null && !(parent instanceof JoinNode)) {
                parent = parent.getParent();
            }
            if (parent != null) {
                JoinNode joinNode = (JoinNode)parent;
                if (joinNode.getJoinStrategy() instanceof MergeJoinStrategy) {
                    MergeJoinStrategy mjs = (MergeJoinStrategy)joinNode.getJoinStrategy();
                    mjs.setProcessingSortRight(true);
                }
            }
            sort = false;
        }
        if (!sort) {
            query.setOrderBy(null);
        }
                
        boolean result = super.prepareNextCommand(query);
        
        dependentCrit = null;
        
        criteriaProcessor.consumedCriteria();
        
        return result;
    }

    /**
     * @see org.teiid.query.processor.relational.AccessNode#hasNextCommand()
     */
    protected boolean hasNextCommand() {
        return criteriaProcessor.hasNextCommand();
    }

}