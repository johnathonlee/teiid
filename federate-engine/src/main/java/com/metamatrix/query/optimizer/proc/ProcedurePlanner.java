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

package com.metamatrix.query.optimizer.proc;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.metamatrix.api.exception.MetaMatrixComponentException;
import com.metamatrix.api.exception.query.QueryMetadataException;
import com.metamatrix.api.exception.query.QueryPlannerException;
import com.metamatrix.core.id.IDGenerator;
import com.metamatrix.query.analysis.AnalysisRecord;
import com.metamatrix.query.execution.QueryExecPlugin;
import com.metamatrix.query.metadata.QueryMetadataInterface;
import com.metamatrix.query.optimizer.CommandPlanner;
import com.metamatrix.query.optimizer.CommandTreeNode;
import com.metamatrix.query.optimizer.capabilities.CapabilitiesFinder;
import com.metamatrix.query.processor.ProcessorPlan;
import com.metamatrix.query.processor.proc.AbstractAssignmentInstruction;
import com.metamatrix.query.processor.proc.AssignmentInstruction;
import com.metamatrix.query.processor.proc.BreakInstruction;
import com.metamatrix.query.processor.proc.ContinueInstruction;
import com.metamatrix.query.processor.proc.ErrorInstruction;
import com.metamatrix.query.processor.proc.ExecDynamicSqlInstruction;
import com.metamatrix.query.processor.proc.ExecSqlInstruction;
import com.metamatrix.query.processor.proc.IfInstruction;
import com.metamatrix.query.processor.proc.LoopInstruction;
import com.metamatrix.query.processor.proc.ProcedureEnvironment;
import com.metamatrix.query.processor.proc.ProcedurePlan;
import com.metamatrix.query.processor.proc.WhileInstruction;
import com.metamatrix.query.processor.program.Program;
import com.metamatrix.query.processor.program.ProgramInstruction;
import com.metamatrix.query.sql.lang.Command;
import com.metamatrix.query.sql.lang.DynamicCommand;
import com.metamatrix.query.sql.lang.Into;
import com.metamatrix.query.sql.lang.Query;
import com.metamatrix.query.sql.proc.AssignmentStatement;
import com.metamatrix.query.sql.proc.Block;
import com.metamatrix.query.sql.proc.CommandStatement;
import com.metamatrix.query.sql.proc.CreateUpdateProcedureCommand;
import com.metamatrix.query.sql.proc.IfStatement;
import com.metamatrix.query.sql.proc.LoopStatement;
import com.metamatrix.query.sql.proc.Statement;
import com.metamatrix.query.sql.proc.WhileStatement;
import com.metamatrix.query.sql.symbol.Expression;
import com.metamatrix.query.sql.symbol.GroupSymbol;
import com.metamatrix.query.sql.visitor.ReferenceCollectorVisitor;
import com.metamatrix.query.util.CommandContext;

/**
 * <p> This prepares an {@link com.metamatrix.query.processor.proc.ProcedurePlan ProcedurePlan} from
 * a CreateUpdateProcedureCommand {@link com.metamatrix.query.sql.proc.CreateUpdateProcedureCommand CreateUpdateProcedureCommand}.
 * </p>
 */
public final class ProcedurePlanner implements CommandPlanner {
	
	/**
	 * <p>This method does nothing as the method call to {@link #optimize} directly produces
	 * the ProcessorPlan for the given procedure.</p>
	 *
	 * @param rootNode tree of CommandTreeNode object(s) rooted at rootNode
	 * @param debug whether or not to generate verbose debug output during planning
	 * @throws QueryPlannerException indicating a problem in planning
     * @throws MetaMatrixComponentException indicating an unexpected exception
	 */
	public void generateCanonical(CommandTreeNode rootNode, QueryMetadataInterface metadata, AnalysisRecord analysisRecord, CommandContext context)
	throws QueryPlannerException, MetaMatrixComponentException {
		// does nothing
	}

	/**
	 * <p>Produce a ProcessorPlan for the CreateUpdateProcedureCommand on the current node
	 * of the CommandTreeNode, the procedure plan construction involves using the child
	 * processor plans.</p>
	 * @param node root of a tree (or subtree) of CommandTreeNode objects, each of
	 * which should have its canonical plan
	 * @param metadata source of metadata
	 * @param debug whether or not to generate verbose debug output during planning
	 * @return ProcessorPlan This processorPlan is a <code>ProcedurePlan</code>
     * @throws QueryPlannerException indicating a problem in planning
     * @throws QueryMetadataException indicating an exception in accessing the metadata
     * @throws MetaMatrixComponentException indicating an unexpected exception
	 */
	public ProcessorPlan optimize(CommandTreeNode node, IDGenerator idGenerator, QueryMetadataInterface metadata, CapabilitiesFinder capFinder, AnalysisRecord analysisRecord, CommandContext context)
	throws QueryPlannerException, QueryMetadataException, MetaMatrixComponentException {

		// get the current command on the current node of the tree
		Command procCommand = node.getCommand();
		// set state of the planner with child nodes
		// to be used while planning
		List childNodes = node.getChildren();
		// index giving the next child node to be accessed in the list
        ChildIndexHolder childIndex = new ChildIndexHolder();

        boolean debug = analysisRecord.recordDebug();
        if(debug) {
            analysisRecord.println("\n####################################################"); //$NON-NLS-1$
            analysisRecord.println("PROCEDURE COMMAND: " + node.getCommand()); //$NON-NLS-1$
        }

        if(!(procCommand instanceof CreateUpdateProcedureCommand)) {
        	throw new QueryPlannerException(QueryExecPlugin.Util.getString("ProcedurePlanner.wrong_type", procCommand.getType())); //$NON-NLS-1$
        }

        Block block = ((CreateUpdateProcedureCommand) procCommand).getBlock();

		Program programBlock = planBlock(((CreateUpdateProcedureCommand)procCommand), block, metadata, childNodes, debug, childIndex, idGenerator, capFinder);

        if(debug) {
            analysisRecord.println("\n####################################################"); //$NON-NLS-1$
        }

        // create plan from program and initialized environment
        ProcedureEnvironment env = new ProcedureEnvironment();
        env.getProgramStack().push(programBlock);
        ProcedurePlan plan = new ProcedurePlan(env);
        env.initialize(plan);
        env.setUpdateProcedure(((CreateUpdateProcedureCommand)procCommand).isUpdateProcedure());
        env.setOutputElements(((CreateUpdateProcedureCommand)procCommand).getProjectedSymbols());
        
        if(debug) {
            analysisRecord.println("####################################################"); //$NON-NLS-1$
            analysisRecord.println("PROCEDURE PLAN :"+plan); //$NON-NLS-1$
            analysisRecord.println("####################################################"); //$NON-NLS-1$
        }

        return plan;
	}

	/**
	 * <p> Plan a {@link Block} object, recursively plan each statement in the given block and
	 * add the resulting {@link ProgramInstruction} a new {@link Program} for the block.</p>
	 * @param block The <code>Block</code> to be planned
	 * @param metadata Metadata used during planning
	 * @param childNodes list of CommandTreeNode objects that contain the ProcessorPlans of the child nodes of this procedure
	 * @param childIndex index giving the next child node to be accessed in the list
	 * @param debug Boolean detemining if procedure plan needs to be printed for debug purposes
	 * @return A Program resulting in the block planning
	 * @throws QueryPlannerException if invalid statement is encountered in the block
	 * @throws QueryMetadataException if there is an error accessing metadata
	 * @throws MetaMatrixComponentException if unexpected error occurs
	 */
    private Program planBlock(CreateUpdateProcedureCommand parentProcCommand, Block block, QueryMetadataInterface metadata, List childNodes, boolean debug, ChildIndexHolder childIndex, IDGenerator idGenerator, CapabilitiesFinder capFinder)
        throws QueryPlannerException, QueryMetadataException, MetaMatrixComponentException {

        Iterator stmtIter = block.getStatements().iterator();

        // Generate program and add instructions
        // this program represents the block on the procedure
        // instruction in the program would correspond to statements in the block
        Program programBlock = new Program();

		// plan each statement in the block
        while(stmtIter.hasNext()) {
			Statement statement = (Statement) stmtIter.next();
			Object instruction = planStatement(parentProcCommand, statement, metadata, childNodes, debug, childIndex, idGenerator, capFinder);
			//childIndex = ((Integer) array[0]).intValue();
            if(instruction instanceof ProgramInstruction){
                programBlock.addInstruction((ProgramInstruction)instruction);
            }else{
                //an array of ProgramInstruction
                ProgramInstruction[] insts = (ProgramInstruction[])instruction;
                for(int i=0; i<insts.length; i++){
			        programBlock.addInstruction(insts[i]);
                }
            }
        }

        return programBlock;
    }

	/**
	 * <p> Plan a {@link Statement} object, depending on the type of the statement construct the appropriate
	 * {@link ProgramInstruction} return it to added to a {@link Program}. If the statement references a
	 * <code>Command</code>, it looks up the child CommandTreeNodes to get approproiate child's ProcessrPlan
	 * and uses it for constructing the necessary instruction.</p>
	 * @param statement The statement to be planned
	 * @param metadata Metadata used during planning
	 * @param childNodes list of CommandTreeNode objects that contain the ProcessorPlans of the child nodes of this procedure
	 * @param childIndex index giving the next child node to be accessed in the list
	 * @param debug Boolean detemining if procedure plan needs to be printed for debug purposes
	 * @return An array containing index of the next child to be accessesd and the ProgramInstruction resulting
	 * in the statement planning
	 * @throws QueryPlannerException if invalid statement is encountered
	 * @throws QueryMetadataException if there is an error accessing metadata
	 * @throws MetaMatrixComponentException if unexpected error occurs
	 */
    private Object planStatement(CreateUpdateProcedureCommand parentProcCommand, Statement statement, QueryMetadataInterface metadata, List childNodes, boolean debug, ChildIndexHolder childIndex, IDGenerator idGenerator, CapabilitiesFinder capFinder)
        throws QueryPlannerException, QueryMetadataException, MetaMatrixComponentException {

		int stmtType = statement.getType();
		// object array containing updated child index and the process instruction
		//Object array[] = new Object[2];
		// program instr resulting in planning this statement
		Object instruction = null;
		switch(stmtType) {
            case Statement.TYPE_ERROR: 
			case Statement.TYPE_ASSIGNMENT:
            case Statement.TYPE_DECLARE:
            {
                AbstractAssignmentInstruction assignInstr = null;
				if (stmtType == Statement.TYPE_ERROR) {
                    assignInstr = new ErrorInstruction();
                } else {
                    assignInstr = new AssignmentInstruction();
                }
                instruction = assignInstr;
                
                AssignmentStatement assignStmt = (AssignmentStatement)statement;
                
                assignInstr.setVariable(assignStmt.getVariable());
                
                ProcessorPlan assignPlan = null;
				if(assignStmt.hasCommand()) {
					List references = ReferenceCollectorVisitor.getReferences(assignStmt.getCommand());
					assignPlan = ((CommandTreeNode)childNodes.get(childIndex.getChildIndex())).getProcessorPlan();
					childIndex.incrementChildIndex();                    
                    assignInstr.setProcessPlan(assignPlan);
                    assignInstr.setReferences(references);
				} else if (assignStmt.hasExpression()) {
					Expression asigExpr = assignStmt.getExpression();
                    assignInstr.setExpression(asigExpr);
					Collection references = ReferenceCollectorVisitor.getReferences(asigExpr);
                    assignInstr.setReferences(references);
				}
                if(debug) {
                    System.out.println("\t"+instruction.toString()+"\n" + statement); //$NON-NLS-1$ //$NON-NLS-2$
                    if (assignPlan != null) {
                        System.out.println("\t\tASSIGNMENT COMMAND PROCESS PLAN:\n " + assignPlan); //$NON-NLS-1$
                    }
                }
				break;
            }
			case Statement.TYPE_COMMAND:
            {
				CommandStatement cmdStmt = (CommandStatement) statement;
                Command command = cmdStmt.getCommand();
                GroupSymbol intoGroup = null;
                if(command instanceof Query){
                    Into into = ((Query)command).getInto();
                    if(into != null){
                        intoGroup = into.getGroup();
                    }
                }
				List references = ReferenceCollectorVisitor.getReferences(command);
				ProcessorPlan commandPlan = ((CommandTreeNode)childNodes.get(childIndex.getChildIndex())).getProcessorPlan();
				childIndex.incrementChildIndex();
                
				if (command.getType() == Command.TYPE_DYNAMIC){
					instruction = new ExecDynamicSqlInstruction(parentProcCommand,((DynamicCommand)command), references, metadata, idGenerator, capFinder );
				}else{
					instruction = new ExecSqlInstruction(commandPlan, references, intoGroup);
				}
                
				if(debug) {
		            System.out.println("\tCOMMAND STATEMENT:\n " + statement); //$NON-NLS-1$
		            System.out.println("\t\tSTATEMENT COMMAND PROCESS PLAN:\n " + commandPlan); //$NON-NLS-1$
				}
				break;
            }
			case Statement.TYPE_IF:
            {
				IfStatement ifStmt = (IfStatement)statement;
				Program ifProgram = planBlock(parentProcCommand, ifStmt.getIfBlock(), metadata, childNodes, debug, childIndex, idGenerator, capFinder);
				Program elseProgram = null;
				if(ifStmt.hasElseBlock()) {
					elseProgram = planBlock(parentProcCommand, ifStmt.getElseBlock(), metadata, childNodes, debug, childIndex, idGenerator, capFinder);
				}
				instruction = new IfInstruction(ifStmt.getCondition(), ifProgram, elseProgram);
				if(debug) {
		            System.out.println("\tIF STATEMENT:\n" + statement); //$NON-NLS-1$
				}
				break;
            }
            case Statement.TYPE_BREAK:
            {
                if(debug) {
                    System.out.println("\tBREAK STATEMENT:\n" + statement); //$NON-NLS-1$
                }
                instruction = new BreakInstruction();
                break;
            }
            case Statement.TYPE_CONTINUE:
            {
                if(debug) {
                    System.out.println("\tCONTINUE STATEMENT:\n" + statement); //$NON-NLS-1$
                }
                instruction = new ContinueInstruction();
                break;
            }
            case Statement.TYPE_LOOP:
            {
                LoopStatement loopStmt = (LoopStatement)statement;
                if(debug) {
                    System.out.println("\tLOOP STATEMENT:\n" + statement); //$NON-NLS-1$
                }
                String rsName = loopStmt.getCursorName();
                ProcessorPlan commandPlan = ((CommandTreeNode)childNodes.get(childIndex.getChildIndex())).getProcessorPlan();
                childIndex.incrementChildIndex();

                Command command = loopStmt.getCommand();
                List references = ReferenceCollectorVisitor.getReferences(command);

                Program loopProgram = planBlock(parentProcCommand, loopStmt.getBlock(), metadata, childNodes, debug, childIndex, idGenerator, capFinder);
                instruction = new LoopInstruction(loopProgram, rsName, commandPlan, references);
                break;
            }
            case Statement.TYPE_WHILE:
            {
                WhileStatement whileStmt = (WhileStatement)statement;
                Program whileProgram = planBlock(parentProcCommand, whileStmt.getBlock(), metadata, childNodes, debug, childIndex, idGenerator, capFinder);
                if(debug) {
                    System.out.println("\tWHILE STATEMENT:\n" + statement); //$NON-NLS-1$
                }
                instruction = new WhileInstruction(whileProgram, whileStmt.getCondition());
                break;
            }
			default:
	        	throw new QueryPlannerException(QueryExecPlugin.Util.getString("ProcedurePlanner.bad_stmt", stmtType)); //$NON-NLS-1$
		}
		return instruction;
    }
   
    static class ChildIndexHolder{
        private int childIndex;

        int getChildIndex() {
            return childIndex;
        }

        void incrementChildIndex() {
            childIndex++;
        }        
    }
} // END CLASS
