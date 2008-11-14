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

package com.metamatrix.connector.metadata.internal;

import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import com.metamatrix.cdk.CommandBuilder;
import com.metamatrix.core.util.StringUtil;
import com.metamatrix.core.util.UnitTestUtil;
import com.metamatrix.data.language.ICommand;
import com.metamatrix.data.metadata.runtime.RuntimeMetadata;
import com.metamatrix.dqp.internal.datamgr.metadata.MetadataFactory;
import com.metamatrix.dqp.internal.datamgr.metadata.RuntimeMetadataImpl;
import com.metamatrix.metadata.runtime.FakeMetadataService;
import com.metamatrix.metadata.runtime.FakeQueryMetadata;

public class TestObjectQueryProcessor extends TestCase {
    public static final String TEST_FILE_NAME = UnitTestUtil.getTestDataPath() + "/PartsSupplier.vdb"; //$NON-NLS-1$

    private CommandBuilder commandBuilder;
    private static TstFileReader testFile;

    private static PrintWriter actualResultsFile;

    public TestObjectQueryProcessor(String name) {
        super(name);
    }

    private void runNextTest() throws Exception {
        FakeMetadataService metaService = new FakeMetadataService(TEST_FILE_NAME);

        String queryText = testFile.readQuery();
        actualResultsFile.append(queryText);
        actualResultsFile.append(StringUtil.LINE_SEPARATOR);

        ObjectQuery query = new ObjectQuery(getRuntimeMetadata(), getCommand(queryText));
        Iterator results = new ObjectQueryProcessor(metaService.getMetadataObjectSource("PartsSupplier", "1")).process(query); //$NON-NLS-1$ //$NON-NLS-2$

        checkResults(testFile.readResults(), dumpResults(results));
    }

    public static RuntimeMetadata getRuntimeMetadata() {
        return new RuntimeMetadataImpl(new MetadataFactory(FakeQueryMetadata.getQueryMetadata()));
    }

    private void checkResults(String expected, String actual) {
        actualResultsFile.append(actual);
        actualResultsFile.append(StringUtil.LINE_SEPARATOR);
        assertEquals(expected, actual);
    }

    public void testNameField() throws Exception {
        runNextTest();
    }

    public void testCardinalityField() throws Exception {
        runNextTest();
    }

    public void testSupportsUpdateField() throws Exception {
        runNextTest();
    }

    public void testColumns() throws Exception {
        runNextTest();
    }

    public void testTwoFields() throws Exception {
        runNextTest();
    }

    public void testModels() throws Exception {
        runNextTest();
    }

    public void testPathModels() throws Exception {
        runNextTest();
    }

    public void testUUIDModels() throws Exception {
        runNextTest();
    }

    public void testCriteria() throws Exception {
        runNextTest();
    }

    public void testMultipleCriteria() throws Exception {
        runNextTest();
    }

    public void testFkKeys() throws Exception {
        runNextTest();
    }

    public void testPkKeys() throws Exception {
        runNextTest();
    }

    public void testCaseCriteria1() throws Exception {
        runNextTest();
    }

    public void testCaseCriteria2() throws Exception {
        runNextTest();
    }

    public void testCaseCriteria3() throws Exception {
        runNextTest();
    }

    public void testCaseCriteria4() throws Exception {
        runNextTest();
    }

    public void testCaseCriteria5() throws Exception {
        runNextTest();
    }

    public void testCaseCriteria6() throws Exception {
        runNextTest();
    }

    public void testCaseCriteria7() throws Exception {
        runNextTest();
    }

    public void testCaseCriteria8() throws Exception {
        runNextTest();
    }

    public void testCaseCriteria9() throws Exception {
        runNextTest();
    }

    public void testCaseCriteria10() throws Exception {
        runNextTest();
    }

    public void testNullCriteria() throws Exception {
        runNextTest();
    }

    public void testWildCardCriteria() throws Exception {
        runNextTest();
    }

    public void testInCriteria() throws Exception {
        runNextTest();
    }

    public static String dumpResults(Iterator rows){
        StringBuffer result = new StringBuffer();
        while(rows.hasNext()){
            List row = (List) rows.next();
            Iterator elementIterator = row.iterator();
            boolean firstElement = true;
            while(elementIterator.hasNext()){
                Object element = elementIterator.next();
                if (!firstElement){
                    result.append('\t');
                }
                result.append(element);
                firstElement = false;
            }
            result.append( StringUtil.LINE_SEPARATOR );
        }
        return result.toString();
    }

    private ICommand getCommand(String sql){
        return commandBuilder.getCommand(sql);
    }


    /*
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        commandBuilder = new CommandBuilder(FakeQueryMetadata.getQueryMetadata());
        if (testFile == null) {
            testFile = new TstFileReader(UnitTestUtil.getTestDataPath() + File.separator+"tests.txt"); //$NON-NLS-1$
        }
        if (actualResultsFile == null) {
            actualResultsFile = new PrintWriter(UnitTestUtil.getTestScratchFile("results.txt")); //$NON-NLS-1$
            actualResultsFile.write(""); //$NON-NLS-1$
        }
    }
}
