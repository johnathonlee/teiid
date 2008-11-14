/*
 * JBoss, Home of Professional Open Source.
 * Copyright (C) 2008 Red Hat, Inc.
 * Copyright (C) 2000-2007 MetaMatrix, Inc.
 * Licensed to Red Hat, Inc. under one or more contributor 
 * license agreements.  See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package com.metamatrix.query.processor.proc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.metamatrix.api.exception.MetaMatrixComponentException;
import com.metamatrix.api.exception.MetaMatrixProcessingException;
import com.metamatrix.common.buffer.BlockedException;
import com.metamatrix.common.log.LogManager;
import com.metamatrix.query.processor.ProcessorPlan;
import com.metamatrix.query.processor.program.ProgramEnvironment;
import com.metamatrix.query.processor.xml.ProcessorInstruction;
import com.metamatrix.query.sql.symbol.GroupSymbol;
import com.metamatrix.query.sql.util.VariableContext;
import com.metamatrix.query.util.LogConstants;

/**
 * <p> Executes a SQL statement, and remove its results from the buffer manager. Executing this
 * instruction does not modify the values of any of the variables, hence it's results are not
 * important so they are removed immediately.</p>
 */
public class ExecSqlInstruction extends CommandInstruction {

    public static final String RS_NAME = "EXECSQL_INSTRUCTION"; //$NON-NLS-1$

	// processor plan for the command on the CommandStatement
    private ProcessorPlan commandPlan;
    //group for into if it is Select Into
    private GroupSymbol intoGroup;

    /**
     * Constructor for ExecSqlInstruction.
     * @param command Object (such as a ProcessorPlan) that is executed when this instruction
     * is processed.
	 * @param references List of lists of reference objects,  these references
	 * would be updated to the values they evaluate to.
     */
	public ExecSqlInstruction(ProcessorPlan commandPlan, List references, GroupSymbol intoGroup) {
        this.commandPlan = commandPlan;
        this.intoGroup = intoGroup;
        setReferences(references);
	}

	/**
     * <p>Processing this instruction executes the ProcessorPlan for the command on the
     * CommandStatement of the update procedure language. Executing this plan does not effect
     * the values of any of the variables defined as part of the update procedure and hence
     * the results of the ProcessPlan execution need not be stored for furthur processing. The
     * results are removed from the buffer manager immediately after execution. The program
     * counter is incremented after execution of the plan.</p>
     * @throws BlockedException if this processing the plan thows a currentVarContext
     * @see ProcessorInstruction#process(ProcessorEnvironment)
     */
    public void process(ProgramEnvironment env)
        throws BlockedException, MetaMatrixComponentException, MetaMatrixProcessingException {

        LogManager.logTrace(LogConstants.CTX_QUERY_PLANNER, new Object[]{"Processing ExecSqlInstruction as part of processing the update procedure"}); //$NON-NLS-1$

        ProcedureEnvironment procEnv = (ProcedureEnvironment) env;
        VariableContext varContext = procEnv.getCurrentVariableContext();

        // get the current set of references and set their values
        setReferenceValues(varContext);

        if(intoGroup != null && intoGroup.isTempGroupSymbol()){
            procEnv.executePlan(commandPlan, intoGroup.getName());
        }else{
            procEnv.executePlan(commandPlan, RS_NAME);
        }

        // We don't close the result here because this ExecSQLInstruction may be the last one in the procedure,
        // in which case the containing ProcedurePlan.close will manage the lifecycle of the last tuple source.
        // Those tuple sources that are not the last will be closed by the ProcedureEnvironment.executePlan method before the next
        // instruction is executed.
    }

    public ProcessorPlan getProcessorPlan(){ //Defect 13291 - made public to support changes to ProcedurePlan
        return this.commandPlan;
    }
    
    /**
     * Returns a deep clone
     */
    public Object clone(){
		List copyReferences = cloneReferences();
        
        GroupSymbol clonedIntoGroup = null;
        if(this.intoGroup != null){
            clonedIntoGroup = (GroupSymbol)intoGroup.clone();
        }
        
        ExecSqlInstruction clone = new ExecSqlInstruction((ProcessorPlan)commandPlan.clone(), copyReferences, clonedIntoGroup);
        return clone;
    }

    public String toString() {
		return "ExecSQLInstruction"; //$NON-NLS-1$
    }

    public Map getDescriptionProperties() {
        Map props = new HashMap();
        props.put(PROP_TYPE, "SQL"); //$NON-NLS-1$
        props.put(PROP_SQL, RS_NAME); 
        props.put(PROP_PROGRAM, this.commandPlan.getDescriptionProperties());
        if(intoGroup != null) {
            props.put(PROP_GROUP, intoGroup.toString());
        }
        return props;
    }
    
    /** 
     * @see com.metamatrix.query.processor.program.ProgramInstruction#getChildPlans()
     * @since 4.2
     */
    public Collection getChildPlans() {
        List plans = new ArrayList(1);
        plans.add(this.commandPlan);
        return plans;
    }
    
}
