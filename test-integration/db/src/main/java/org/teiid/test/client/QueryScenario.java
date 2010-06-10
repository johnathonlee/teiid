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
package org.teiid.test.client;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.teiid.core.util.FileUtils;
import org.teiid.core.util.PropertiesUtils;
import org.teiid.test.client.TestProperties.RESULT_MODES;
import org.teiid.test.framework.ConfigPropertyLoader;
import org.teiid.test.framework.TestLogger;
import org.teiid.test.framework.exception.QueryTestFailedException;
import org.teiid.test.framework.exception.TransactionRuntimeException;



/**
 * The QueryScenario manages all the information required to run one scenario of
 * tests. This includes the following: <li>The queryreader and its query sets to
 * be executed as a scenario</li> <li>Provides the expected results that
 * correspond to a query set</li> <li>The results generator that would be used
 * when {@link RESULT_MODES#GENERATE} is specified</li>
 * 
 * @author vanhalbert
 * 
 */
public abstract class QueryScenario {

    protected QueryReader reader = null;
    protected ResultsGenerator resultsGen = null;

    private String resultMode = TestProperties.RESULT_MODES.NONE;

    private Properties props = null;
    private String scenarioName;
    private String querySetName;

    public QueryScenario(String scenarioName, Properties queryProperties) {
	this.props = queryProperties;
	this.scenarioName = scenarioName;

	this.querySetName = props.getProperty(TestProperties.QUERY_SET_NAME,
		"querysetnamenotdefined");

	setUp();

    }

    protected void setUp() {
	Collection args = new ArrayList(1);
	args.add(scenarioName);
	args.add(props);

	reader = ClassFactory.createQueryReader(args);

	args = new ArrayList(2);
	args.add(this.scenarioName);
	args.add(this.props);

	resultsGen = ClassFactory.createResultsGenerator(args);

	if (reader.getQuerySetIDs() == null
		|| reader.getQuerySetIDs().isEmpty()) {
	    throw new TransactionRuntimeException(
		    "The queryreader did not return any queryset ID's to process");
	}

	validateResultsMode(this.props);
	
	// TODO:  deployprops.loc not needed in remote testing
//	try {
//		setupVDBs(this.getProperties());
//	} catch (IOException e) {
//		throw new TransactionRuntimeException(e.getMessage());
//	}

    }

 //   protected void setupVDBs(Properties props) throws IOException {
	// NOTE: this is probably a hack, because the only way I could get all
	// the vdb's available when running multiple scenarions
	// was to update the deploy.properties by setting the vdb.definition
	// property containing the location of
	// all the vdbs

//	
//	// if disabled, no configuration of the vdb is needed
//	if (ConfigPropertyLoader.getInstance().isDataStoreDisabled()) {
//	    return;
//	}
//	
//	String deployPropLoc = props.getProperty("deployprops.loc");
//	Properties deployProperties = PropertiesUtils.load(deployPropLoc);
//
//	// set the vdb.definition property that contains all the vdbs
//	String vdb_loc = props.getProperty("vdb.loc");
//	File vdbfiles[] = FileUtils.findAllFilesInDirectoryHavingExtension(vdb_loc, ".vdb");
//	if (vdbfiles == null || vdbfiles.length == 0) {
//	    throw new TransactionRuntimeException((new StringBuilder()).append(
//		    "No vdbs found in directory ").append(vdb_loc).toString());
//	}
//	StringBuffer vdbdef = new StringBuffer();
//
//	for (int i = 0; i < vdbfiles.length; i++) {
//	    vdbdef.append(vdbfiles[i].getAbsolutePath() + ";");
//	}
//	TestLogger.log("=====  Connect to VDBs: " + vdbdef.toString());
//	
//	deployProperties.setProperty("vdb.definition", vdbdef.toString());
//	PropertiesUtils.print(deployPropLoc, deployProperties,"Updated for vdb.definition");

 //   }


    protected void validateResultsMode(Properties props) {
	// Determine from property what to do with query results
	String resultModeStr = props.getProperty(
		TestProperties.PROP_RESULT_MODE, "");
	// No need to check for null prop here since we've just checked for this
	// required property

	if (resultModeStr.equalsIgnoreCase(TestProperties.RESULT_MODES.NONE)
		|| resultModeStr
			.equalsIgnoreCase(TestProperties.RESULT_MODES.COMPARE)
		|| resultModeStr
			.equalsIgnoreCase(TestProperties.RESULT_MODES.GENERATE)) { //$NON-NLS-1$
	    resultMode = resultModeStr;
	}
	// otherwise use default of NONE

	TestLogger.log("\nResults mode: " + resultMode); //$NON-NLS-1$

    }

    /**
     * Return the name that identifies this query set. It should use the
     * {@link TestProperties#QUERY_SET_NAME} property to obtain the name.
     * 
     * @return String query set name;
     */
    public String getQuerySetName() {
	return this.querySetName;
    }

    /**
     * Return the identifier for the current scenario
     * 
     * @return String name of scenario
     */
    public String getQueryScenarioIdentifier() {
	return this.scenarioName;
    }

    /**
     * Return the properties defined for this scenario
     * 
     * @return Properties
     */
    public Properties getProperties() {
	return this.props;
    }

    /**
     * Return a <code>Map</code> containing the query identifier as the key, and
     * the value is the query. In most simple cases, the query will be a
     * <code>String</code> However, complex types (i.e., to execute prepared
     * statements or other arguments), it maybe some other type.
     * 
     * @param querySetID
     *            identifies a set of queries
     * @return Map<String, Object>
     */

    public List<QueryTest> getQueries(String querySetID) {
	try {
	    return reader.getQueries(querySetID);
	} catch (QueryTestFailedException e) {
	    throw new TransactionRuntimeException(e);
	}
    }

    /**
     * Return a <code>Collection</code> of <code>querySetID</code>s that the
     * {@link QueryReader} will be providing. The <code>querySetID</code> can be
     * used to obtain it associated set of queries by call
     * {@link #getQueries(String)}
     * 
     * @return Collection of querySetIDs
     */
    public Collection<String> getQuerySetIDs() {
	return reader.getQuerySetIDs();
    }

    /**
     * Return the result mode that was defined by the property
     * {@link TestProperties#PROP_RESULT_MODE}
     * 
     * @return String result mode
     */
    public String getResultsMode() {
	return this.resultMode;
    }

    /**
     * Return the {@link ExpectedResults} for the specified
     * <code>querySetID</code>. These expected results will be used to compare
     * with the actual results in order to determine success or failure.
     * 
     * @param querySetID
     * @return ExpectedResults
     */
    public ExpectedResults getExpectedResults(String querySetID) {
	Collection args = new ArrayList(2);
	args.add(querySetID);
	args.add(props);

	return ClassFactory.createExpectedResults(args);

    }

    /**
     * Return the {@link ResultsGenerator} that is to be used to create new sets
     * of expected results.
     * 
     * @return
     */
    public ResultsGenerator getResultsGenerator() {
	return this.resultsGen;
    }

    /**
     * Return the {@link QueryReader} that is to be used to obtain the queries
     * to process.
     * 
     * @return
     */
    public QueryReader getQueryReader() {
	return this.reader;
    }


    public abstract void handleTestResult(TestResult tr, ResultSet resultSet, int updateCnt, boolean resultFromQuery, String sql);


}
