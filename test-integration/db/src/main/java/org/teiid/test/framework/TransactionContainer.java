/*
 * Copyright (c) 2000-2007 MetaMatrix, Inc.
 * All rights reserved.
 */
package org.teiid.test.framework;

import java.util.Properties;

import net.sf.saxon.functions.Substring;

import org.teiid.test.framework.connection.ConnectionStrategy;
import org.teiid.test.framework.connection.ConnectionStrategyFactory;
import org.teiid.test.framework.datasource.DataSourceFactory;
import org.teiid.test.framework.exception.QueryTestFailedException;
import org.teiid.test.framework.exception.TransactionRuntimeException;

import com.metamatrix.core.util.StringUtil;


public abstract class TransactionContainer {
	
		private boolean debug = false;
		
		protected ConfigPropertyLoader config = null;
		protected Properties props;
		protected ConnectionStrategy connStrategy;
		protected DataSourceFactory dsfactory;
		
		protected String testClassName = null;
	    
	    protected TransactionContainer(ConfigPropertyLoader propertyconfig){        
	    	this.config = propertyconfig;
	    }
	    
	    protected void setUp(TransactionQueryTest test) throws QueryTestFailedException  {
	    	this.dsfactory = new DataSourceFactory(config);
	    	
	    	this.connStrategy = ConnectionStrategyFactory.createConnectionStrategy(config, dsfactory);
	        this.props = new Properties();
	        this.props.putAll(this.connStrategy.getEnvironment());
	        
	    	test.setConnectionStrategy(connStrategy);

	       	test.setupDataSource();

   	
	    }
	    
	    protected void before(TransactionQueryTest test){}
	    
	    protected void after(TransactionQueryTest test) {}
	        
	    public void runTransaction(TransactionQueryTest test) {
	    	
	    	this.testClassName =StringUtil.getLastToken(test.getClass().getName(), ".");
 		
	        try {		 
	        	
	        	runIt(test);
	        	
	        } finally {
	        	debug("	test.cleanup");
	        	
	        	try {
	        		test.cleanup();
	        	} finally {
			    	
			   		// cleanup all defined datasources for the last test and
	        		// any overrides regarding inclusions and exclusions.
		    		this.dsfactory.cleanup();

			    	
		    		// cleanup all connections created for this test.
		    		connStrategy.shutdown();
	        		
	        	}
	        }

	    }
	    
	    private void runIt(TransactionQueryTest test)  {
	    	
	    	detail("Start transaction test: " + test.getTestName());
 
	        try {  
	        	
	           	setUp(test);
	        	
	        	debug("	setConnection");
	            test.setConnection(this.connStrategy.getConnection());
	            test.setExecutionProperties(this.props);
	            debug("	before(test)");
	                        
	            before(test);
	            debug("	test.before");

	            test.before();
	            
	            debug("	test.testcase");

	            // run the test
	            test.testCase();
	            
	        	debug("	test.after");

	            test.after();
	            debug("	after(test)");

	            after(test);
	            
	            detail("End transaction test: " + test.getTestName());

	            
	        }catch(Throwable e) {
	        	if (!test.exceptionExpected()) {
	        		e.printStackTrace();
	        	}
	            throw new TransactionRuntimeException(e.getMessage());
	        }
	        
            if (test.exceptionExpected() && !test.exceptionOccurred()) {
            	throw new TransactionRuntimeException("Expected exception, but one did not occur for test: " + this.getClass().getName() + "." + test.getTestName());
            }
	        
	        try {
		        detail("Start validation: " + test.getTestName());

	        	test.validateTestCase();
	        	
	        	detail("End validation: " + test.getTestName());

	        }catch(Exception e) {
	            throw new TransactionRuntimeException(e);
	        }
            
	    	detail("Completed transaction test: " + test.getTestName());


	    }       
	    
	    public Properties getEnvironmentProperties() {
	    	return props;
	    }
	    
	    protected void debug(String message) {
	    	if (debug) {
	    		System.out.println("[" + this.testClassName + "] " + message);
	    	}
	    	
	    }
	    
	    protected void detail(String message) {
	    	System.out.println("[" + this.testClassName + "] " + message);
	    }
	    

    

}
