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
package org.teiid.translator.salesforce.execution.visitors;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import org.junit.Test;
import org.mockito.Mockito;
import org.teiid.cdk.api.TranslationUtility;
import org.teiid.core.types.DataTypeManager;
import org.teiid.language.Select;
import org.teiid.metadata.Column;
import org.teiid.metadata.MetadataStore;
import org.teiid.metadata.Schema;
import org.teiid.metadata.Table;
import org.teiid.metadata.Column.SearchType;
import org.teiid.query.metadata.CompositeMetadataStore;
import org.teiid.query.metadata.QueryMetadataInterface;
import org.teiid.query.metadata.TransformationMetadata;
import org.teiid.query.unittest.FakeMetadataFactory;
import org.teiid.query.unittest.RealMetadataFactory;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.salesforce.Constants;
import org.teiid.translator.salesforce.SalesforceConnection;
import org.teiid.translator.salesforce.execution.QueryExecutionImpl;

@SuppressWarnings("nls")
public class TestVisitors {

    public static QueryMetadataInterface exampleSalesforce() { 
    	MetadataStore store = new MetadataStore();
        // Create models
        Schema salesforceModel = RealMetadataFactory.createPhysicalModel("SalesforceModel", store); //$NON-NLS-1$
       
        // Create Account group
        Table accountTable = RealMetadataFactory.createPhysicalGroup("Account", salesforceModel); //$NON-NLS-1$
        accountTable.setNameInSource("Account"); //$NON-NLS-1$
        accountTable.setProperty("Supports Query", Boolean.TRUE.toString()); //$NON-NLS-1$
        accountTable.setProperty(Constants.SUPPORTS_RETRIEVE, Boolean.TRUE.toString());
        // Create Account Columns
        String[] acctNames = new String[] {
            "ID", "Name", "Stuff", "Industry"  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        };
        String[] acctTypes = new String[] {  
            DataTypeManager.DefaultDataTypes.STRING, DataTypeManager.DefaultDataTypes.STRING, DataTypeManager.DefaultDataTypes.STRING, DataTypeManager.DefaultDataTypes.STRING
        };
        
        List<Column> acctCols = RealMetadataFactory.createElements(accountTable, acctNames, acctTypes);
        acctCols.get(2).setNativeType("multipicklist"); //$NON-NLS-1$
        acctCols.get(2).setSearchType(SearchType.Like_Only);
        // Set name in source on each column
        String[] accountNameInSource = new String[] {
           "id", "AccountName", "Stuff", "Industry"             //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$  
        };
        for(int i=0; i<2; i++) {
            Column obj = acctCols.get(i);
            obj.setNameInSource(accountNameInSource[i]);
        }
        
        // Create Contact group
        Table contactTable = RealMetadataFactory.createPhysicalGroup("Contacts", salesforceModel); //$NON-NLS-1$
        contactTable.setNameInSource("Contact"); //$NON-NLS-1$
        contactTable.setProperty("Supports Query", Boolean.TRUE.toString()); //$NON-NLS-1$
        // Create Contact Columns
        String[] elemNames = new String[] {
            "ContactID", "Name", "AccountId", "InitialContact"  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        };
        String[] elemTypes = new String[] {  
            DataTypeManager.DefaultDataTypes.STRING, DataTypeManager.DefaultDataTypes.STRING, DataTypeManager.DefaultDataTypes.STRING, DataTypeManager.DefaultDataTypes.TIMESTAMP 
        };
        
        List<Column> contactCols = RealMetadataFactory.createElements(contactTable, elemNames, elemTypes);
        // Set name in source on each column
        String[] contactNameInSource = new String[] {
           "id", "ContactName", "accountid", "InitialContact"  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        };
        for(int i=0; i<2; i++) {
            Column obj = contactCols.get(i);
            obj.setNameInSource(contactNameInSource[i]);
        }
        return new TransformationMetadata(null, new CompositeMetadataStore(store), null, FakeMetadataFactory.SFM.getSystemFunctions(), null);
    }    

	private static TranslationUtility translationUtility = new TranslationUtility(exampleSalesforce());

	@Test public void testOr() throws Exception {
		Select command = (Select)translationUtility.parseCommand("select * from Account where Name = 'foo' or Stuff = 'bar'"); //$NON-NLS-1$
		SelectVisitor visitor = new SelectVisitor(translationUtility.createRuntimeMetadata());
		visitor.visit(command);
		assertEquals("SELECT Account.id, Account.AccountName, Account.Stuff, Account.Industry FROM Account WHERE (Account.AccountName = 'foo') OR (Account.Stuff = 'bar')", visitor.getQuery().toString().trim()); //$NON-NLS-1$
	}
	
	@Test public void testNot() throws Exception {
		Select command = (Select)translationUtility.parseCommand("select * from Account where not (Name = 'foo' and Stuff = 'bar')"); //$NON-NLS-1$
		SelectVisitor visitor = new SelectVisitor(translationUtility.createRuntimeMetadata());
		visitor.visit(command);
		assertEquals("SELECT Account.id, Account.AccountName, Account.Stuff, Account.Industry FROM Account WHERE (Account.AccountName != 'foo') OR (Account.Stuff != 'bar')", visitor.getQuery().toString().trim()); //$NON-NLS-1$
	}
	
	@Test public void testCountStart() throws Exception {
		Select command = (Select)translationUtility.parseCommand("select count(*) from Account"); //$NON-NLS-1$
		SelectVisitor visitor = new SelectVisitor(translationUtility.createRuntimeMetadata());
		visitor.visit(command);
		assertEquals("SELECT count() FROM Account", visitor.getQuery().toString().trim()); //$NON-NLS-1$
	}
	
	@Test public void testNotLike() throws Exception {
		Select command = (Select)translationUtility.parseCommand("select * from Account where Name not like '%foo' or Stuff = 'bar'"); //$NON-NLS-1$
		SelectVisitor visitor = new SelectVisitor(translationUtility.createRuntimeMetadata());
		visitor.visit(command);
		assertEquals("SELECT Account.id, Account.AccountName, Account.Stuff, Account.Industry FROM Account WHERE (NOT (Account.AccountName LIKE '%foo')) OR (Account.Stuff = 'bar')", visitor.getQuery().toString().trim()); //$NON-NLS-1$
	}

	
	@Test public void testIN() throws Exception {
		Select command = (Select)translationUtility.parseCommand("select * from Account where Industry IN (1,2,3)"); //$NON-NLS-1$
		SelectVisitor visitor = new SelectVisitor(translationUtility.createRuntimeMetadata());
		visitor.visit(command);
		assertFalse(visitor.hasOnlyIDCriteria());
		assertEquals("SELECT Account.id, Account.AccountName, Account.Stuff, Account.Industry FROM Account WHERE Industry IN('1','2','3')", visitor.getQuery().toString().trim()); //$NON-NLS-1$
		
	}

	@Test public void testOnlyIDsIN() throws Exception {
		// this can resolve to a better performing retrieve call
		Select command = (Select)translationUtility.parseCommand("select * from Account where ID IN (1,2,3)"); //$NON-NLS-1$
		SelectVisitor visitor = new SelectVisitor(translationUtility.createRuntimeMetadata());
		visitor.visit(command);
		assertTrue(visitor.hasOnlyIdInCriteria());
		assertEquals("Account", visitor.getTableName());
		assertEquals("Account.id, Account.AccountName, Account.Stuff, Account.Industry", visitor.getRetrieveFieldList());
		assertEquals(Arrays.asList(new String[]{"1", "2", "3"}), visitor.getIdInCriteria());	
	}
	
	@Test public void testJoin() throws Exception {
		Select command = (Select)translationUtility.parseCommand("SELECT Account.Name, Contacts.Name FROM Contacts LEFT OUTER JOIN Account ON Account.Id = Contacts.AccountId"); //$NON-NLS-1$
		SelectVisitor visitor = new JoinQueryVisitor(translationUtility.createRuntimeMetadata());
		visitor.visit(command);
		assertEquals("SELECT Account.AccountName, Contact.ContactName FROM Contact", visitor.getQuery().toString().trim()); //$NON-NLS-1$
	}
	
	@Test public void testJoin2() throws Exception {
		Select command = (Select)translationUtility.parseCommand("SELECT Account.Name, Contacts.Name FROM Account LEFT OUTER JOIN Contacts ON Account.Id = Contacts.AccountId"); //$NON-NLS-1$
		SelectVisitor visitor = new JoinQueryVisitor(translationUtility.createRuntimeMetadata());
		visitor.visit(command);
		assertEquals("SELECT Account.AccountName, (SELECT Contact.ContactName FROM Contacts) FROM Account", visitor.getQuery().toString().trim()); //$NON-NLS-1$
	}
	
	@Test public void testJoin3() throws Exception {
		Select command = (Select)translationUtility.parseCommand("SELECT Contacts.Name FROM Account LEFT OUTER JOIN Contacts ON Account.Id = Contacts.AccountId"); //$NON-NLS-1$
		SelectVisitor visitor = new JoinQueryVisitor(translationUtility.createRuntimeMetadata());
		visitor.visit(command);
		assertEquals("SELECT (SELECT Contact.ContactName FROM Contacts) FROM Account", visitor.getQuery().toString().trim()); //$NON-NLS-1$
	}
	
	@Test public void testInWithNameInSourceDifferent() throws Exception {
		Select command = (Select)translationUtility.parseCommand("SELECT Contacts.Name FROM Contacts WHERE Contacts.Name in ('x', 'y')"); //$NON-NLS-1$
		SelectVisitor visitor = new SelectVisitor(translationUtility.createRuntimeMetadata());
		visitor.visit(command);
		assertEquals("SELECT Contact.ContactName FROM Contact WHERE ContactName IN('x','y')", visitor.getQuery().toString().trim()); //$NON-NLS-1$
	}
	
	@Test public void testIDCriteria() throws Exception {
		Select command = (Select)translationUtility.parseCommand("select id, name from Account where id = 'bar'"); //$NON-NLS-1$
		SalesforceConnection sfc = Mockito.mock(SalesforceConnection.class);
		QueryExecutionImpl qei = new QueryExecutionImpl(command, sfc, translationUtility.createRuntimeMetadata(), Mockito.mock(ExecutionContext.class));
		qei.execute();
		Mockito.verify(sfc).retrieve("Account.id, Account.AccountName", "Account", Arrays.asList("bar"));
	}
	
	@Test public void testDateTimeFormating() throws Exception {
		TimeZone.setDefault(TimeZone.getTimeZone("GMT-06:00"));
		try {
			Select command = (Select)translationUtility.parseCommand("select name from contacts where initialcontact = {ts'2003-03-11 11:42:10.5'}"); //$NON-NLS-1$
			SelectVisitor visitor = new SelectVisitor(translationUtility.createRuntimeMetadata());
			visitor.visit(command);
			assertEquals("SELECT Contact.ContactName FROM Contact WHERE Contact.InitialContact = 2003-03-11T11:42:10.500-06:00", visitor.getQuery().toString().trim()); //$NON-NLS-1$
		} finally {
			TimeZone.setDefault(null);
		}
	}

}
