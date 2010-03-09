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

package org.teiid.connector.metadata.runtime;

import java.util.List;

import junit.framework.TestCase;

import org.teiid.connector.language.Call;
import org.teiid.connector.language.ColumnReference;
import org.teiid.connector.language.DerivedColumn;
import org.teiid.connector.language.NamedTable;
import org.teiid.connector.language.Select;

import com.metamatrix.cdk.api.TranslationUtility;
import com.metamatrix.core.util.UnitTestUtil;

/**
 */
public class TestMetadataObject extends TestCase {

    private static TranslationUtility CONNECTOR_METADATA_UTILITY = createTranslationUtility(getTestVDBName());

    /**
     * Constructor for TestMetadataID.
     * @param name
     */
    public TestMetadataObject(String name) {
        super(name);
    }

    private static String getTestVDBName() {
    	return UnitTestUtil.getTestDataPath() + "/ConnectorMetadata.vdb"; //$NON-NLS-1$
    }
    
    public static TranslationUtility createTranslationUtility(String vdbName) {
        return new TranslationUtility(vdbName);        
    }
    
    
    // ################ TEST GROUP METADATAID ######################
    
    public Table getGroupID(String groupName, TranslationUtility transUtil) {
        Select query = (Select) transUtil.parseCommand("SELECT 1 FROM " + groupName); //$NON-NLS-1$
        NamedTable group = (NamedTable) query.getFrom().get(0);
        return group.getMetadataObject();
    }

    public void helpTestGroupID(String fullGroupName, String shortGroupName, int elementCount, TranslationUtility transUtil) throws Exception {
        Table groupID = getGroupID(fullGroupName, transUtil);     
        assertEquals(fullGroupName, groupID.getFullName());
        assertEquals(shortGroupName, groupID.getName());
        // Check children
        List<Column> children = groupID.getColumns();
        assertEquals(elementCount, children.size());
        for (Column element : children) {
            assertEquals(groupID, element.getParent());
            assertTrue(element.getFullName().startsWith(groupID.getFullName()));            
        }
    }
    
    public void testGroupID() throws Exception {
        helpTestGroupID("ConnectorMetadata.TestTable", "TestTable", 7, CONNECTOR_METADATA_UTILITY);//$NON-NLS-1$ //$NON-NLS-2$ 
    }   

    public void testGroupID_longName() throws Exception {
        helpTestGroupID("ConnectorMetadata.TestCatalog.TestSchema.TestTable2", "TestCatalog.TestSchema.TestTable2", 1, CONNECTOR_METADATA_UTILITY);//$NON-NLS-1$ //$NON-NLS-2$ 
    }   

    // ################ TEST ELEMENT METADATAID ######################
    
    public Column getElementID(String groupName, String elementName, TranslationUtility transUtil) {
        Select query = (Select) transUtil.parseCommand("SELECT " + elementName + " FROM " + groupName); //$NON-NLS-1$ //$NON-NLS-2$
        DerivedColumn symbol = query.getDerivedColumns().get(0);
        ColumnReference element = (ColumnReference) symbol.getExpression();
        return element.getMetadataObject();
    }
    
    public void helpTestElementID(String groupName, String elementName, TranslationUtility transUtil) throws Exception {
        Column elementID = getElementID(groupName, elementName, transUtil);     
        assertEquals(groupName + "." + elementName, elementID.getFullName()); //$NON-NLS-1$
        assertEquals(elementName, elementID.getName());
        assertNotNull(elementID.getParent());
        assertEquals(groupName, elementID.getParent().getFullName());        
    }
    
    public void testElementID() throws Exception {
        helpTestElementID("ConnectorMetadata.TestTable", "TestNameInSource", CONNECTOR_METADATA_UTILITY);//$NON-NLS-1$ //$NON-NLS-2$ 
    }   

    public void testElementID_longName() throws Exception {
        helpTestElementID("ConnectorMetadata.TestCatalog.TestSchema.TestTable2", "TestCol", CONNECTOR_METADATA_UTILITY);//$NON-NLS-1$ //$NON-NLS-2$ 
    }   

    // ################ TEST PROCEDURE AND PARAMETER METADATAID ######################
    
    public Procedure getProcedureID(String procName, int inputParamCount, TranslationUtility transUtil) {
        StringBuffer sql = new StringBuffer("EXEC "); //$NON-NLS-1$
        sql.append(procName);
        sql.append("("); //$NON-NLS-1$
        for(int i=0; i<inputParamCount; i++) {
            sql.append("null"); //$NON-NLS-1$
            if(i<(inputParamCount-1)) { 
                sql.append(", ");                 //$NON-NLS-1$
            }
        }
        sql.append(")"); //$NON-NLS-1$
        
        Call proc = (Call) transUtil.parseCommand(sql.toString()); 
        return proc.getMetadataObject();
    }
    
    public void helpTestProcedureID(String procName, String shortName, int inputParamCount, String[] paramNames, String rsParamName, TranslationUtility transUtil) throws Exception {
        Procedure procID = getProcedureID(procName, inputParamCount, transUtil);     
        assertEquals(procName, procID.getFullName()); 
        assertEquals(shortName, procID.getName());
        
        // Check children
        List<ProcedureParameter> children = procID.getParameters();
        int i = 0;
        for (ProcedureParameter childID : children) {
            assertEquals(procID, childID.getParent());
            assertTrue(childID.getFullName() + " " + procID.getFullName(), childID.getFullName().startsWith(procID.getFullName())); //$NON-NLS-1$
            assertEquals(paramNames[i++], childID.getName());            
        }
        
        if (rsParamName != null) {
        	assertEquals(rsParamName, procID.getResultSet().getName());
        } else {
        	assertNull(procID.getResultSet());
        }
    }
    
    public void testProcedureID() throws Exception {
        String[] paramNames = new String[] { "InParam", "OutParam", "InOutParam", "ReturnParam" };          //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$
        helpTestProcedureID("ConnectorMetadata.TestProc1", "TestProc1", 2, paramNames, null, CONNECTOR_METADATA_UTILITY); //$NON-NLS-1$ //$NON-NLS-2$               
    }

    public void testProcedureID_resultSet() throws Exception {
        String[] paramNames = new String[] { "Param1"};          //$NON-NLS-1$
        helpTestProcedureID("ConnectorMetadata.TestProc2", "TestProc2", 1, paramNames, "RSParam", CONNECTOR_METADATA_UTILITY); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$               
    }

    public void testProcedureID_longName() throws Exception {
        helpTestProcedureID("ConnectorMetadata.TestCatalog.TestSchema.TestProc", "TestCatalog.TestSchema.TestProc", 0, new String[0], null, CONNECTOR_METADATA_UTILITY); //$NON-NLS-1$ //$NON-NLS-2$
    }


}
