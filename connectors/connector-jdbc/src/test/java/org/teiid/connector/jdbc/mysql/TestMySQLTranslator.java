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

package org.teiid.connector.jdbc.mysql;

import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;
import org.teiid.connector.api.ConnectorException;
import org.teiid.connector.jdbc.TranslationHelper;

import com.metamatrix.cdk.api.EnvironmentUtility;

/**
 */
public class TestMySQLTranslator {

    private static MySQLTranslator TRANSLATOR; 
    
    @BeforeClass public static void oneTimeSetup() throws ConnectorException {
        TRANSLATOR = new MySQLTranslator();        
        TRANSLATOR.initialize(EnvironmentUtility.createEnvironment(new Properties(), false));
    }

    private String getTestVDB() {
        return TranslationHelper.PARTS_VDB;
    }
    
    private String getTestBQTVDB() {
        return TranslationHelper.BQT_VDB; 
    }
    
    @Test public void testConversion1() throws Exception {
        String input = "SELECT char(convert(PART_WEIGHT, integer) + 100) FROM PARTS"; //$NON-NLS-1$
        String output = "SELECT char((cast(PARTS.PART_WEIGHT AS signed) + 100)) FROM PARTS";  //$NON-NLS-1$

        TranslationHelper.helpTestVisitor(getTestVDB(),
            input, 
            output, TRANSLATOR);
    }
          
    @Test public void testConversion2() throws Exception {
        String input = "SELECT convert(PART_WEIGHT, long) FROM PARTS"; //$NON-NLS-1$
        String output = "SELECT cast(PARTS.PART_WEIGHT AS signed) FROM PARTS";  //$NON-NLS-1$

        TranslationHelper.helpTestVisitor(getTestVDB(),
            input, 
            output, TRANSLATOR);
    }
          
    @Test public void testConversion3() throws Exception {
        String input = "SELECT convert(convert(PART_WEIGHT, long), string) FROM PARTS"; //$NON-NLS-1$
        String output = "SELECT cast(cast(PARTS.PART_WEIGHT AS signed) AS char) FROM PARTS";  //$NON-NLS-1$

        TranslationHelper.helpTestVisitor(getTestVDB(),
            input, 
            output, TRANSLATOR);
    }
          
    @Test public void testConversion4() throws Exception {
        String input = "SELECT convert(convert(PART_WEIGHT, date), string) FROM PARTS"; //$NON-NLS-1$
        String output = "SELECT date_format(DATE(PARTS.PART_WEIGHT), '%Y-%m-%d') FROM PARTS";  //$NON-NLS-1$

        TranslationHelper.helpTestVisitor(getTestVDB(),
            input, 
            output, TRANSLATOR);
    }
    @Test public void testConversion5() throws Exception {
        String input = "SELECT convert(convert(PART_WEIGHT, time), string) FROM PARTS"; //$NON-NLS-1$
        String output = "SELECT date_format(TIME(PARTS.PART_WEIGHT), '%H:%i:%S') FROM PARTS";  //$NON-NLS-1$

        TranslationHelper.helpTestVisitor(getTestVDB(),
            input, 
            output, TRANSLATOR);
    }
    @Test public void testConversion6() throws Exception {
        String input = "SELECT convert(convert(PART_WEIGHT, timestamp), string) FROM PARTS"; //$NON-NLS-1$
        String output = "SELECT date_format(TIMESTAMP(PARTS.PART_WEIGHT), '%Y-%m-%d %H:%i:%S.%f') FROM PARTS";  //$NON-NLS-1$

        TranslationHelper.helpTestVisitor(getTestVDB(),
            input, 
            output, TRANSLATOR);
    }
    @Test public void testConversion8() throws Exception {
        String input = "SELECT ifnull(PART_WEIGHT, 'otherString') FROM PARTS"; //$NON-NLS-1$
        String output = "SELECT ifnull(PARTS.PART_WEIGHT, 'otherString') FROM PARTS";  //$NON-NLS-1$

        TranslationHelper.helpTestVisitor(getTestVDB(),
            input, 
            output, TRANSLATOR);
    }
    @Test public void testConversion7() throws Exception {
        String input = "SELECT convert(convert(PART_WEIGHT, integer), string) FROM PARTS"; //$NON-NLS-1$
        String output = "SELECT cast(cast(PARTS.PART_WEIGHT AS signed) AS char) FROM PARTS";  //$NON-NLS-1$

        TranslationHelper.helpTestVisitor(getTestVDB(),
            input, 
            output, TRANSLATOR);
    }
    @Test public void testInsert() throws Exception {
        String input = "SELECT insert(PART_WEIGHT, 1, 5, 'chimp') FROM PARTS"; //$NON-NLS-1$
        String output = "SELECT insert(PARTS.PART_WEIGHT, 1, 5, 'chimp') FROM PARTS";  //$NON-NLS-1$

        TranslationHelper.helpTestVisitor(getTestVDB(),
            input, 
            output, TRANSLATOR);
    }

    /**
     * Test the translator's ability to rewrite the LOCATE() function in a form 
     * suitable for the data source.
     * <p>
     * <code>SELECT locate(INTNUM, 'chimp', 1) FROM BQT1.SMALLA</code>
     *  
     * @throws Exception
     */
    @Test public void testLocate() throws Exception {
        String input = "SELECT locate(INTNUM, 'chimp', 1) FROM BQT1.SMALLA"; //$NON-NLS-1$
        String output = "SELECT LOCATE(cast(SmallA.IntNum AS char), 'chimp', 1) FROM SmallA";  //$NON-NLS-1$

        TranslationHelper.helpTestVisitor(TranslationHelper.BQT_VDB,
                input, output, 
                TRANSLATOR);
    }

    /**
     * Test the translator's ability to rewrite the LOCATE() function in a form 
     * suitable for the data source.
     * <p>
     * <code>SELECT locate(STRINGNUM, 'chimp') FROM BQT1.SMALLA</code>
     *  
     * @throws Exception
     */
    @Test public void testLocate2() throws Exception {
        String input = "SELECT locate(STRINGNUM, 'chimp') FROM BQT1.SMALLA"; //$NON-NLS-1$
        String output = "SELECT LOCATE(SmallA.StringNum, 'chimp') FROM SmallA";  //$NON-NLS-1$

        TranslationHelper.helpTestVisitor(TranslationHelper.BQT_VDB,
                input, output, 
                TRANSLATOR);
    }

    /**
     * Test the translator's ability to rewrite the LOCATE() function in a form 
     * suitable for the data source.
     * <p>
     * <code>SELECT locate(INTNUM, '234567890', 1) FROM BQT1.SMALLA WHERE INTKEY = 26</code>
     *  
     * @throws Exception
     */
    @Test public void testLocate3() throws Exception {
        String input = "SELECT locate(INTNUM, '234567890', 1) FROM BQT1.SMALLA WHERE INTKEY = 26"; //$NON-NLS-1$
        String output = "SELECT LOCATE(cast(SmallA.IntNum AS char), '234567890', 1) FROM SmallA WHERE SmallA.IntKey = 26";  //$NON-NLS-1$

        TranslationHelper.helpTestVisitor(TranslationHelper.BQT_VDB,
                input, output, 
                TRANSLATOR);
    }

    /**
     * Test the translator's ability to rewrite the LOCATE() function in a form 
     * suitable for the data source.
     * <p>
     * <code>SELECT locate('c', 'chimp', 1) FROM BQT1.SMALLA</code>
     *  
     * @throws Exception
     */
    @Test public void testLocate4() throws Exception {
        String input = "SELECT locate('c', 'chimp', 1) FROM BQT1.SMALLA"; //$NON-NLS-1$
        String output = "SELECT 1 FROM SmallA";  //$NON-NLS-1$

        TranslationHelper.helpTestVisitor(TranslationHelper.BQT_VDB,
                input, output, 
                TRANSLATOR);
    }

    /**
     * Test the translator's ability to rewrite the LOCATE() function in a form 
     * suitable for the data source.
     * <p>
     * <code>SELECT locate(STRINGNUM, 'chimp', -5) FROM BQT1.SMALLA</code>
     *  
     * @throws Exception
     */
    @Test public void testLocate5() throws Exception {
        String input = "SELECT locate(STRINGNUM, 'chimp', -5) FROM BQT1.SMALLA"; //$NON-NLS-1$
        String output = "SELECT LOCATE(SmallA.StringNum, 'chimp', 1) FROM SmallA";  //$NON-NLS-1$

        TranslationHelper.helpTestVisitor(TranslationHelper.BQT_VDB,
                input, output, 
                TRANSLATOR);
    }

    /**
     * Test the translator's ability to rewrite the LOCATE() function in a form 
     * suitable for the data source.
     * <p>
     * <code>SELECT locate(STRINGNUM, 'chimp', INTNUM) FROM BQT1.SMALLA</code>
     *  
     * @throws Exception
     */
    @Test public void testLocate6() throws Exception {
        String input = "SELECT locate(STRINGNUM, 'chimp', INTNUM) FROM BQT1.SMALLA"; //$NON-NLS-1$
        String output = "SELECT LOCATE(SmallA.StringNum, 'chimp', CASE WHEN SmallA.IntNum < 1 THEN 1 ELSE SmallA.IntNum END) FROM SmallA";  //$NON-NLS-1$

        TranslationHelper.helpTestVisitor(TranslationHelper.BQT_VDB,
                input, output, 
                TRANSLATOR);
    }

    /**
     * Test the translator's ability to rewrite the LOCATE() function in a form 
     * suitable for the data source.
     * <p>
     * <code>SELECT locate(STRINGNUM, 'chimp', LOCATE(STRINGNUM, 'chimp') + 1) FROM BQT1.SMALLA</code>
     *  
     * @throws Exception
     */
    @Test public void testLocate7() throws Exception {
        String input = "SELECT locate(STRINGNUM, 'chimp', LOCATE(STRINGNUM, 'chimp') + 1) FROM BQT1.SMALLA"; //$NON-NLS-1$
        String output = "SELECT LOCATE(SmallA.StringNum, 'chimp', CASE WHEN (LOCATE(SmallA.StringNum, 'chimp') + 1) < 1 THEN 1 ELSE (LOCATE(SmallA.StringNum, 'chimp') + 1) END) FROM SmallA";  //$NON-NLS-1$

        TranslationHelper.helpTestVisitor(TranslationHelper.BQT_VDB,
                input, output, 
                TRANSLATOR);
    }
    
    @Test public void testSubstring1() throws Exception {
        String input = "SELECT substring(PART_WEIGHT, 1) FROM PARTS"; //$NON-NLS-1$
        String output = "SELECT substring(PARTS.PART_WEIGHT, 1) FROM PARTS";  //$NON-NLS-1$

        TranslationHelper.helpTestVisitor(getTestVDB(),
            input, 
            output, TRANSLATOR);
    }
    @Test public void testSubstring2() throws Exception {
        String input = "SELECT substring(PART_WEIGHT, 1, 5) FROM PARTS"; //$NON-NLS-1$
        String output = "SELECT substring(PARTS.PART_WEIGHT, 1, 5) FROM PARTS";  //$NON-NLS-1$

        TranslationHelper.helpTestVisitor(getTestVDB(),
            input, 
            output, TRANSLATOR);
    }
    @Test public void testUnionWithOrderBy() throws Exception {
        String input = "SELECT PART_ID FROM PARTS UNION SELECT PART_ID FROM PARTS ORDER BY PART_ID"; //$NON-NLS-1$
        String output = "(SELECT PARTS.PART_ID FROM PARTS) UNION (SELECT PARTS.PART_ID FROM PARTS) ORDER BY PART_ID";  //$NON-NLS-1$

        TranslationHelper.helpTestVisitor(getTestVDB(),
            input, 
            output, TRANSLATOR);
    }
    
    @Test public void testRowLimit2() throws Exception {
        String input = "select intkey from bqt1.smalla limit 100"; //$NON-NLS-1$
        String output = "SELECT SmallA.IntKey FROM SmallA LIMIT 100"; //$NON-NLS-1$
               
        TranslationHelper.helpTestVisitor(getTestBQTVDB(),
            input, 
            output, TRANSLATOR);        
    }
    
    @Test public void testRowLimit3() throws Exception {
        String input = "select intkey from bqt1.smalla limit 50, 100"; //$NON-NLS-1$
        String output = "SELECT SmallA.IntKey FROM SmallA LIMIT 50, 100"; //$NON-NLS-1$
               
        TranslationHelper.helpTestVisitor(getTestBQTVDB(),
            input, 
            output, TRANSLATOR);        
    }
    
    @Test public void testBitAnd() throws Exception {
        String input = "select bitand(intkey, intnum) from bqt1.smalla"; //$NON-NLS-1$
        String output = "SELECT cast((SmallA.IntKey & SmallA.IntNum) AS signed) FROM SmallA"; //$NON-NLS-1$
               
        TranslationHelper.helpTestVisitor(TranslationHelper.BQT_VDB,
            input, 
            output, TRANSLATOR);        
    }

    @Test public void testJoins() throws Exception {
        String input = "select smalla.intkey from bqt1.smalla inner join bqt1.smallb on smalla.stringkey=smallb.stringkey cross join bqt1.mediuma"; //$NON-NLS-1$
        String output = "SELECT SmallA.IntKey FROM (SmallA INNER JOIN SmallB ON SmallA.StringKey = SmallB.StringKey) CROSS JOIN MediumA"; //$NON-NLS-1$
          
        TranslationHelper.helpTestVisitor(TranslationHelper.BQT_VDB,
            input, 
            output, TRANSLATOR);        
    }
    
    @Test public void testTimestampLiteral() throws Exception {
        String input = "select smalla.intkey from bqt1.smalla where smalla.timestampvalue = '2009-08-06 12:23:34.999'"; //$NON-NLS-1$
        String output = "SELECT SmallA.IntKey FROM SmallA WHERE SmallA.TimestampValue = {ts '2009-08-06 12:23:34.0'}"; //$NON-NLS-1$
          
        TranslationHelper.helpTestVisitor(TranslationHelper.BQT_VDB,
            input, 
            output, TRANSLATOR);        
    }
}
