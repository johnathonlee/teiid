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

package com.metamatrix.query.optimizer;

import com.metamatrix.api.exception.MetaMatrixComponentException;
import com.metamatrix.api.exception.query.QueryMetadataException;
import com.metamatrix.api.exception.query.QueryPlannerException;
import com.metamatrix.core.id.IDGenerator;
import com.metamatrix.query.analysis.AnalysisRecord;
import com.metamatrix.query.metadata.QueryMetadataInterface;
import com.metamatrix.query.optimizer.capabilities.CapabilitiesFinder;
import com.metamatrix.query.processor.ProcessorPlan;
import com.metamatrix.query.sql.lang.Command;
import com.metamatrix.query.util.CommandContext;

/**
 * <p>The common interface of all planners which take a user's command 
 * object and produce a 
 * {@link com.metamatrix.query.processor.ProcessorPlan ProcessorPlan} 
 * object, which is a plan for executing the query.</p>
 */
public interface CommandPlanner {
	
	/**
	 * Allows the planner a chance to optimize the canonical plan(s) stored in
	 * the CommandTreeNode tree.  This method should be called in a bottom-up
	 * manner; from leaf nodes up to the root node.
	 * @param command TODO
	 * @param metadata source of metadata
	 * @param capFinder Class usable to find the connector capabilities for a particular model
	 * @param context 
	 * @param debug whether or not to generate verbose debug output during planning
	 * @return ProcessorPlan implementation specific to the CommandPlanner
     * @throws QueryPlannerException indicating a problem in planning
     * @throws QueryMetadataException indicating an exception in accessing the metadata
     * @throws MetaMatrixComponentException indicating an unexpected exception
	 */
	ProcessorPlan optimize(Command command, IDGenerator idGenerator, QueryMetadataInterface metadata, CapabilitiesFinder capFinder, AnalysisRecord analysisRecord, CommandContext context)
	throws QueryPlannerException, QueryMetadataException, MetaMatrixComponentException;

}
