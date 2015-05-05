package org.teiid.query.processor;

import static org.teiid.query.processor.TestProcessor.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.teiid.common.buffer.BlockedException;
import org.teiid.common.buffer.TupleSource;
import org.teiid.core.TeiidComponentException;
import org.teiid.core.TeiidException;
import org.teiid.core.TeiidProcessingException;
import org.teiid.query.metadata.TransformationMetadata;
import org.teiid.query.optimizer.TestAggregatePushdown;
import org.teiid.query.optimizer.TestOptimizer;
import org.teiid.query.optimizer.TestOptimizer.ComparisonMode;
import org.teiid.query.optimizer.capabilities.BasicSourceCapabilities;
import org.teiid.query.optimizer.capabilities.CapabilitiesFinder;
import org.teiid.query.optimizer.capabilities.DefaultCapabilitiesFinder;
import org.teiid.query.optimizer.capabilities.FakeCapabilitiesFinder;
import org.teiid.query.optimizer.capabilities.SourceCapabilities.Capability;
import org.teiid.query.sql.lang.Command;
import org.teiid.query.unittest.RealMetadataFactory;
import org.teiid.query.util.CommandContext;

@SuppressWarnings({"nls", "unchecked"})
public class TestWithClauseProcessing {
	
	@Test public void testSingleItem() {
	    String sql = "with a (x, y, z) as (select e1, e2, e3 from pm1.g1) SELECT pm1.g2.e2, a.x from pm1.g2, a where e1 = x and z = 1 order by x"; //$NON-NLS-1$
	    
	    List<?>[] expected = new List[] { 
	        Arrays.asList(0, "a"),
	        Arrays.asList(3, "a"),
	        Arrays.asList(0, "a"),
	        Arrays.asList(1, "c"),
	    };    
	
	    FakeDataManager dataManager = new FakeDataManager();
	    dataManager.setBlockOnce();
	    sampleData1(dataManager);
	    
	    ProcessorPlan plan = helpGetPlan(helpParse(sql), RealMetadataFactory.example1Cached());
	    
	    helpProcess(plan, dataManager, expected);
	}
	
	@Test public void testMultipleItems() {
	    String sql = "with a (x, y, z) as (select e1, e2, e3 from pm1.g1), b as (SELECT * from pm1.g2, a where e1 = x and z = 1 order by e2 limit 2) SELECT a.x, b.e1 from a, b where a.x = b.e1"; //$NON-NLS-1$
	    
	    List<?>[] expected = new List[] { 
	        Arrays.asList("a", "a"),
	        Arrays.asList("a", "a"),
	        Arrays.asList("a", "a"),
	        Arrays.asList("a", "a"),
	        Arrays.asList("a", "a"),
	        Arrays.asList("a", "a"),
	    };    
	
	    FakeDataManager dataManager = new FakeDataManager();
	    dataManager.setBlockOnce();
	    sampleData1(dataManager);
	    
	    ProcessorPlan plan = helpGetPlan(helpParse(sql), RealMetadataFactory.example1Cached());
	    
	    helpProcess(plan, dataManager, expected);
	}
	
	@Test public void testWithPushdown() throws TeiidException {
		 FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder();
         BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities();
         caps.setCapabilitySupport(Capability.COMMON_TABLE_EXPRESSIONS, true);
         caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN_SELFJOIN, true);
         capFinder.addCapabilities("pm1", caps); //$NON-NLS-1$
        
	    String sql = "with a (x, y, z) as (select e1, e2, e3 from pm1.g1) SELECT a.x from a, a z"; //$NON-NLS-1$
	    
	    HardcodedDataManager dataManager = new HardcodedDataManager();
	    List<?>[] expected = new List[] { 
		        Arrays.asList("a", 1, Boolean.FALSE),
		    };    

	    dataManager.addData("WITH a (x, y, z) AS (SELECT g_0.e1, g_0.e2, g_0.e3 FROM pm1.g1 AS g_0) SELECT g_0.x FROM a AS g_0, a AS g_1", expected);
	    
	    ProcessorPlan plan = TestOptimizer.helpPlan(sql, RealMetadataFactory.example1Cached(), null, capFinder, new String[] {"WITH a (x, y, z) AS (SELECT g_0.e1, g_0.e2, g_0.e3 FROM pm1.g1 AS g_0) SELECT g_0.x FROM a AS g_0, a AS g_1"}, ComparisonMode.EXACT_COMMAND_STRING);
	    
	    helpProcess(plan, dataManager, expected);
	}
	
	@Test public void testWithPushdown1() throws TeiidException {
		FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder();
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities();
        caps.setCapabilitySupport(Capability.COMMON_TABLE_EXPRESSIONS, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN_SELFJOIN, true);
        caps.setCapabilitySupport(Capability.QUERY_AGGREGATES_COUNT, true);
        capFinder.addCapabilities("pm1", caps); //$NON-NLS-1$
       
	    String sql = "with a as (select x, y, z from (select e1 as x, e2 as y, e3 as z from pm1.g1) v) SELECT count(a.x) from a, a z"; //$NON-NLS-1$
	    
	    HardcodedDataManager dataManager = new HardcodedDataManager(RealMetadataFactory.example1Cached());
	    List<?>[] expected = new List[] { 
		        Arrays.asList("a", 1, Boolean.FALSE),
		    };    

	    dataManager.addData("WITH a (x, y, z) AS (SELECT g_0.e1, g_0.e2, g_0.e3 FROM g1 AS g_0) SELECT COUNT(g_0.x) FROM a AS g_0, a AS g_1", expected);
	    
	    ProcessorPlan plan = TestOptimizer.helpPlan(sql, RealMetadataFactory.example1Cached(), null, capFinder, new String[] {"WITH a (x, y, z) AS (SELECT g_0.e1, g_0.e2, g_0.e3 FROM pm1.g1 AS g_0) SELECT COUNT(g_0.x) FROM a AS g_0, a AS g_1"}, ComparisonMode.EXACT_COMMAND_STRING);
	    
	    helpProcess(plan, dataManager, expected);
	}
	
	/**
	 * This tests both an intervening parent plan construct (count) and a reference to a parent with in a subquery
	 */
	@Test public void testWithPushdownNotFullyPushed() throws TeiidException {
		FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder();
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities();
        caps.setCapabilitySupport(Capability.COMMON_TABLE_EXPRESSIONS, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN_SELFJOIN, true);
        caps.setCapabilitySupport(Capability.QUERY_AGGREGATES_COUNT, false);
        capFinder.addCapabilities("pm1", caps); //$NON-NLS-1$
       
	    String sql = "with a as (select x, y, z from (select e1 as x, e2 as y, e3 as z from pm1.g1) v), b as (select e4 from pm1.g3) SELECT count(a.x), max(a.y) from a, a z group by z.x having max(a.y) < (with b as (select e1 from pm1.g1) select a.y from a, b where a.x = z.x)"; //$NON-NLS-1$
	    
	    HardcodedDataManager dataManager = new HardcodedDataManager(RealMetadataFactory.example1Cached());
	    List<?>[] expected = new List[] { 
		        Arrays.asList("a", 1, "a"),
		    };    

	    dataManager.addData("WITH a (x, y, z) AS (SELECT g_0.e1, g_0.e2, g_0.e3 FROM g1 AS g_0) SELECT g_1.x, g_0.y, g_0.x FROM a AS g_0, a AS g_1", expected);
	    dataManager.addData("WITH b (e1) AS (SELECT g_0.e1 FROM g1 AS g_0), a (x, y, z) AS (SELECT g_0.e1, g_0.e2, g_0.e3 FROM g1 AS g_0) SELECT g_0.y FROM a AS g_0, b AS g_1 WHERE g_0.x = 'a'", 
	    		new List[] {Arrays.asList(2)});
	    
	    ProcessorPlan plan = TestOptimizer.helpPlan(sql, RealMetadataFactory.example1Cached(), null, capFinder, new String[] {"WITH a (x, y, z) AS (SELECT g_0.e1, g_0.e2, g_0.e3 FROM pm1.g1 AS g_0) SELECT g_1.x, g_0.y, g_0.x FROM a AS g_0, a AS g_1"}, ComparisonMode.EXACT_COMMAND_STRING);
	    
	    helpProcess(plan, dataManager, new List[] { 
		        Arrays.asList(1, 1),
		    });
	}
	
	/**
	 * Tests source affinity
	 */
	@Test public void testWithPushdownNotFullyPushed1() throws TeiidException {
		FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder();
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities();
        caps.setCapabilitySupport(Capability.COMMON_TABLE_EXPRESSIONS, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN_SELFJOIN, true);
        caps.setCapabilitySupport(Capability.QUERY_AGGREGATES_COUNT, false);
        capFinder.addCapabilities("pm1", caps); //$NON-NLS-1$
        capFinder.addCapabilities("pm2", caps); //$NON-NLS-1$
       
	    String sql = "with a as (select e1 from pm1.g1), b as (select e1 from pm2.g2), c as (select count(*) as x from pm1.g1) SELECT a.e1, (select max(x) from c), pm1.g1.e2 from pm1.g1, a, b"; //$NON-NLS-1$
	    
	    HardcodedDataManager dataManager = new HardcodedDataManager(RealMetadataFactory.example1Cached());

	    dataManager.addData("WITH a (e1) AS (SELECT g_0.e1 FROM g1 AS g_0) SELECT g_0.e1 FROM a AS g_0",  new List[] { 
		        Arrays.asList("a"),
		    });
	    dataManager.addData("WITH b (e1) AS (SELECT g_0.e1 FROM g2 AS g_0) SELECT 1 FROM b AS g_0",  new List[] { 
		        Arrays.asList("b"),
		    });
	    dataManager.addData("SELECT g_0.e2 FROM g1 AS g_0", new List[] { 
		        Arrays.asList(1), Arrays.asList(2)
		    });
	    dataManager.addData("SELECT 1 FROM g1 AS g_0", new List[] { 
		        Arrays.asList(1), Arrays.asList(1), Arrays.asList(1)
		    });
	    
	    ProcessorPlan plan = TestOptimizer.helpPlan(sql, RealMetadataFactory.example1Cached(), null, capFinder, new String[] {
	    	"SELECT g_0.e2 FROM pm1.g1 AS g_0", 
	    	"WITH b (e1) AS (SELECT g_0.e1 FROM pm2.g2 AS g_0) SELECT 1 FROM b AS g_0", 
	    	"WITH a (e1) AS (SELECT g_0.e1 FROM pm1.g1 AS g_0) SELECT g_0.e1 FROM a AS g_0"}, ComparisonMode.EXACT_COMMAND_STRING);
	    
	    helpProcess(plan, dataManager,  new List[] { 
		        Arrays.asList("a", 3, 1),
		        Arrays.asList("a", 3, 2),
		    });
	}

	@Test public void testWithPushdownWithConstants() throws TeiidException {
		 FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder();
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities();
        caps.setCapabilitySupport(Capability.COMMON_TABLE_EXPRESSIONS, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN_SELFJOIN, true);
        capFinder.addCapabilities("pm1", caps); //$NON-NLS-1$
       
	    String sql = "with a (x, y) as (select 1, 2 from pm1.g1) SELECT a.x from a, a z"; //$NON-NLS-1$
	    
	    TestOptimizer.helpPlan(sql, RealMetadataFactory.example1Cached(), null, capFinder, new String[] {"WITH a (x, y) AS (SELECT 1, 2 FROM pm1.g1 AS g_0) SELECT g_0.x FROM a AS g_0, a AS g_1"}, ComparisonMode.EXACT_COMMAND_STRING);
	}
	
	@Test public void testWithOrderBy() throws TeiidException {
		FakeCapabilitiesFinder capFinder = new FakeCapabilitiesFinder();
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities();
        caps.setCapabilitySupport(Capability.COMMON_TABLE_EXPRESSIONS, true);
        caps.setCapabilitySupport(Capability.QUERY_FROM_JOIN_SELFJOIN, true);
        capFinder.addCapabilities("pm1", caps); //$NON-NLS-1$
      
	    String sql = "with a (x, y) as (select 1, 2 from pm1.g1) SELECT a.x from a, a z order by x"; //$NON-NLS-1$
	    
	    HardcodedDataManager dataManager = new HardcodedDataManager(RealMetadataFactory.example1Cached());
	    dataManager.addData("WITH a (x, y) AS (SELECT 1, 2 FROM g1 AS g_0) SELECT g_0.x AS c_0 FROM a AS g_0, a AS g_1 ORDER BY c_0", new List[0]);
	    ProcessorPlan plan = TestOptimizer.helpPlan(sql, RealMetadataFactory.example1Cached(), null, capFinder, new String[] {"WITH a (x, y) AS (SELECT 1, 2 FROM pm1.g1 AS g_0) SELECT g_0.x AS c_0 FROM a AS g_0, a AS g_1 ORDER BY c_0"}, ComparisonMode.EXACT_COMMAND_STRING);
	    
	    helpProcess(plan, dataManager,  new List[0]);
	}
	
	@Test public void testWithJoinPlanning() throws TeiidException {
		TransformationMetadata metadata = RealMetadataFactory.example1();
		RealMetadataFactory.setCardinality("pm1.g2", 100000, metadata);
	    String sql = "with a (x) as (select e1 from pm1.g1) SELECT a.x from pm1.g2, a where (pm1.g2.e1 = a.x)"; //$NON-NLS-1$
	    
	    TestOptimizer.helpPlan(sql, metadata, null, TestOptimizer.getGenericFinder(false), new String[] {"SELECT g_0.e1 FROM pm1.g2 AS g_0 WHERE g_0.e1 IN (<dependent values>)", "SELECT a.x FROM a ORDER BY a.x"}, ComparisonMode.EXACT_COMMAND_STRING);
	}
	
	@Test public void testWithJoinPlanning1() throws TeiidException {
		TransformationMetadata metadata = RealMetadataFactory.example1Cached();
	    String sql = "with a (x) as (select e1 from pm1.g1) SELECT a.x from pm1.g2, a where (pm1.g2.e1 = a.x)"; //$NON-NLS-1$
	    
	    TestOptimizer.helpPlan(sql, metadata, null, TestOptimizer.getGenericFinder(false), new String[] {"SELECT g_0.e1 FROM pm1.g2 AS g_0 WHERE g_0.e1 IN (<dependent values>)", "SELECT a.x FROM a ORDER BY a.x"}, ComparisonMode.EXACT_COMMAND_STRING);
	}
	
	@Test public void testWithBlockingJoin() throws TeiidException {
	      
	    String sql = "with a (x, y) as (select e1, e2 from pm1.g1) SELECT a.x, a.y, pm1.g2.e1 from a left outer join pm1.g2 makenotdep on (rtrim(a.x) = pm1.g2.e1) order by a.y"; //$NON-NLS-1$
	    
	    HardcodedDataManager dataManager = new HardcodedDataManager() {
	    	@Override
	    	public TupleSource registerRequest(CommandContext context,
	    			Command command, String modelName,
	    			RegisterRequestParameter parameterObject)
	    			throws TeiidComponentException {
	    		final TupleSource ts = super.registerRequest(context, command, modelName, parameterObject);
	    		return new TupleSource() {
	    			int i = 0;
					
					@Override
					public List<?> nextTuple() throws TeiidComponentException,
							TeiidProcessingException {
						if ((i++ % 100)<3) {
							throw BlockedException.INSTANCE;
						}
						return ts.nextTuple();
					}
					
					@Override
					public void closeSource() {
						ts.closeSource();
					}
				};
	    	}
	    };
	    List<?>[] rows = new List[10];
	    for (int i = 0; i < rows.length; i++) {
	    	rows[i] = Arrays.asList(String.valueOf(i));
	    }
	    dataManager.addData("SELECT g_0.e1 AS c_0 FROM pm1.g2 AS g_0 ORDER BY c_0", rows);
	    rows = new List[100];
	    for (int i = 0; i < rows.length; i++) {
	    	rows[i] = Arrays.asList(String.valueOf(i), i);
	    }
	    dataManager.addData("SELECT g_0.e1, g_0.e2 FROM pm1.g1 AS g_0", rows);
	    
	    dataManager.addData("WITH a (x, y) AS (SELECT 1, 2 FROM g1 AS g_0) SELECT g_0.x AS c_0 FROM a AS g_0, a AS g_1 ORDER BY c_0", new List[0]);
	    ProcessorPlan plan = TestOptimizer.helpPlan(sql, RealMetadataFactory.example1Cached(), null, new DefaultCapabilitiesFinder(TestOptimizer.getTypicalCapabilities()), new String[] {"SELECT a.x, a.y FROM a", "SELECT g_0.e1 AS c_0 FROM pm1.g2 AS g_0 ORDER BY c_0"}, ComparisonMode.EXACT_COMMAND_STRING);
	    //check the full pushdown command
	    
	    List<?>[] result = new List[100];
	    for (int i = 0; i < result.length; i++) {
	    	result[i] = Arrays.asList(String.valueOf(i), i, i < 10?String.valueOf(i):null);
	    }
	    
	    helpProcess(plan, dataManager, result);
	}
	
	@Test public void testX() {
		String sql = "select q.str_a, q.a from(WITH qry_0 as (SELECT e2 AS a1, e1 as str FROM pm1.g1 AS t) SELECT a1 as a, str as str_a from qry_0) as q group by q.str_a, q.a";
		
		List<?>[] expected1 = new List[] {Arrays.asList(1, "a")};
		List<?>[] expected2 = new List[] {Arrays.asList("a", 1)};    
		
		HardcodedDataManager dataManager = new HardcodedDataManager();
	    dataManager.addData("SELECT g_0.e2, g_0.e1 FROM pm1.g1 AS g_0", expected1);
		
	    ProcessorPlan plan = helpGetPlan(helpParse(sql), RealMetadataFactory.example1Cached(), TestAggregatePushdown.getAggregatesFinder());
	    
	    helpProcess(plan, dataManager, expected2);
	}
	
	@Test public void testScalarInlining() throws TeiidComponentException, TeiidProcessingException {
	    String sql = "WITH t(n) AS ( select 1 ) SELECT n FROM t as t1, pm1.g1"; //$NON-NLS-1$

	    BasicSourceCapabilities bsc = TestOptimizer.getTypicalCapabilities();
	    CapabilitiesFinder capFinder = new DefaultCapabilitiesFinder(bsc);
	    TestOptimizer.helpPlan(sql, RealMetadataFactory.example1Cached(), new String[] {"SELECT 1 FROM pm1.g1 AS g_0"}, capFinder, ComparisonMode.EXACT_COMMAND_STRING);
	}

}
