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

/*
 */
package org.teiid.translator.jdbc.sybase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.teiid.language.Command;
import org.teiid.language.Function;
import org.teiid.language.Limit;
import org.teiid.language.OrderBy;
import org.teiid.language.SetQuery;
import org.teiid.translator.Translator;
import org.teiid.translator.TranslatorException;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.SourceSystemFunctions;
import org.teiid.translator.jdbc.AliasModifier;
import org.teiid.translator.jdbc.ConvertModifier;
import org.teiid.translator.jdbc.EscapeSyntaxModifier;
import org.teiid.translator.jdbc.FunctionModifier;
import org.teiid.translator.jdbc.JDBCExecutionFactory;
import org.teiid.translator.jdbc.ModFunctionModifier;


@Translator(name="sybase")
public class SybaseExecutionFactory extends JDBCExecutionFactory {
    
    public void start() throws TranslatorException {
        super.start();
        
        registerFunctionModifier(SourceSystemFunctions.MOD, new ModFunctionModifier("%", getLanguageFactory())); //$NON-NLS-1$ 
        registerFunctionModifier(SourceSystemFunctions.CONCAT, new AliasModifier("+")); //$NON-NLS-1$ 
        registerFunctionModifier(SourceSystemFunctions.LCASE, new AliasModifier("lower")); //$NON-NLS-1$ 
        registerFunctionModifier(SourceSystemFunctions.IFNULL, new AliasModifier("isnull")); //$NON-NLS-1$ 
        registerFunctionModifier(SourceSystemFunctions.UCASE, new AliasModifier("upper")); //$NON-NLS-1$ 
        registerFunctionModifier(SourceSystemFunctions.REPEAT, new AliasModifier("replicate")); //$NON-NLS-1$
        registerFunctionModifier(SourceSystemFunctions.SUBSTRING, new SubstringFunctionModifier(getLanguageFactory()));
        registerFunctionModifier(SourceSystemFunctions.DAYNAME, new EscapeSyntaxModifier());
        registerFunctionModifier(SourceSystemFunctions.MONTHNAME, new EscapeSyntaxModifier());
        registerFunctionModifier(SourceSystemFunctions.DAYOFWEEK, new EscapeSyntaxModifier());
        registerFunctionModifier(SourceSystemFunctions.DAYOFYEAR, new EscapeSyntaxModifier());
        registerFunctionModifier(SourceSystemFunctions.DAYOFMONTH, new EscapeSyntaxModifier());
        registerFunctionModifier(SourceSystemFunctions.HOUR, new EscapeSyntaxModifier());
        registerFunctionModifier(SourceSystemFunctions.MINUTE, new EscapeSyntaxModifier());
        registerFunctionModifier(SourceSystemFunctions.QUARTER, new EscapeSyntaxModifier());
        registerFunctionModifier(SourceSystemFunctions.SECOND, new EscapeSyntaxModifier());
        registerFunctionModifier(SourceSystemFunctions.WEEK, new EscapeSyntaxModifier());
        registerFunctionModifier(SourceSystemFunctions.TIMESTAMPADD, new EscapeSyntaxModifier());
        registerFunctionModifier(SourceSystemFunctions.TIMESTAMPDIFF, new EscapeSyntaxModifier());
        registerFunctionModifier(SourceSystemFunctions.LENGTH, new EscapeSyntaxModifier());
        registerFunctionModifier(SourceSystemFunctions.ATAN2, new EscapeSyntaxModifier());
        
        //add in type conversion
        ConvertModifier convertModifier = new ConvertModifier();
        //boolean isn't treated as bit, since it doesn't support null
        //byte is treated as smallint, since tinyint is unsigned
    	convertModifier.addTypeMapping("smallint", FunctionModifier.BYTE, FunctionModifier.SHORT); //$NON-NLS-1$
    	convertModifier.addTypeMapping("int", FunctionModifier.INTEGER); //$NON-NLS-1$
    	convertModifier.addTypeMapping("numeric(19,0)", FunctionModifier.LONG); //$NON-NLS-1$
    	convertModifier.addTypeMapping("real", FunctionModifier.FLOAT); //$NON-NLS-1$
    	convertModifier.addTypeMapping("double precision", FunctionModifier.DOUBLE); //$NON-NLS-1$
    	convertModifier.addTypeMapping("numeric(38, 0)", FunctionModifier.BIGINTEGER); //$NON-NLS-1$
    	convertModifier.addTypeMapping("numeric(38, 19)", FunctionModifier.BIGDECIMAL); //$NON-NLS-1$
    	convertModifier.addTypeMapping("char(1)", FunctionModifier.CHAR); //$NON-NLS-1$
    	convertModifier.addTypeMapping("varchar(40)", FunctionModifier.STRING); //$NON-NLS-1$
    	convertModifier.addTypeMapping("datetime", FunctionModifier.DATE, FunctionModifier.TIME, FunctionModifier.TIMESTAMP); //$NON-NLS-1$
    	convertModifier.addConvert(FunctionModifier.TIMESTAMP, FunctionModifier.TIME, new FunctionModifier() {
			@Override
			public List<?> translate(Function function) {
				List<Object> result = new ArrayList<Object>();
				result.add("cast("); //$NON-NLS-1$
				result.add("'1970-01-01 ' + "); //$NON-NLS-1$
				result.addAll(convertTimeToString(function));
				result.add(" AS datetime)"); //$NON-NLS-1$
				return result;
			}
		});
    	convertModifier.addConvert(FunctionModifier.TIMESTAMP, FunctionModifier.DATE, new FunctionModifier() {
			@Override
			public List<?> translate(Function function) {
				List<Object> result = new ArrayList<Object>();
				result.add("cast("); //$NON-NLS-1$
				result.addAll(convertDateToString(function));
				result.add(" AS datetime)"); //$NON-NLS-1$
				return result;
			}
		});
    	convertModifier.addConvert(FunctionModifier.TIME, FunctionModifier.STRING, new FunctionModifier() {
			@Override
			public List<?> translate(Function function) {
				return convertTimeToString(function);
			}
		}); 
    	convertModifier.addConvert(FunctionModifier.DATE, FunctionModifier.STRING, new FunctionModifier() {
			@Override
			public List<?> translate(Function function) {
				return convertDateToString(function);
			}
		});
    	convertModifier.addConvert(FunctionModifier.TIMESTAMP, FunctionModifier.STRING, new FunctionModifier() {
			@Override
			public List<?> translate(Function function) {
				return convertTimestampToString(function);
			}
		});
    	convertModifier.addNumericBooleanConversions();
    	registerFunctionModifier(SourceSystemFunctions.CONVERT, convertModifier);
    }
    
	private List<Object> convertTimeToString(Function function) {
		return Arrays.asList("convert(varchar, ", function.getParameters().get(0), ", 8)"); //$NON-NLS-1$ //$NON-NLS-2$
	}
    
    protected List<Object> convertDateToString(Function function) {
		return Arrays.asList("stuff(stuff(convert(varchar, ", function.getParameters().get(0), ", 102), 5, 1, '-'), 8, 1, '-')"); //$NON-NLS-1$ //$NON-NLS-2$
	}
    
    /**
     * Written to only support version 15
     * @param function
     * @return
     */
	protected List<?> convertTimestampToString(Function function) {
		return Arrays.asList("stuff(convert(varchar, ", function.getParameters().get(0), ", 123), 11, 1, ' ')"); //$NON-NLS-1$ //$NON-NLS-2$
	}
    
    @Override
    public boolean useAsInGroupAlias() {
    	return false;
    }
    
    @Override
    public boolean hasTimeType() {
    	return false;
    }
    
    @Override
    public int getTimestampNanoPrecision() {
    	return 3;
    }
    
    /**
     * SetQueries don't have a concept of TOP, an inline view is needed.
     */
    @Override
    public List<?> translateCommand(Command command, ExecutionContext context) {
    	if (!(command instanceof SetQuery)) {
    		return null;
    	}
    	SetQuery queryCommand = (SetQuery)command;
		if (queryCommand.getLimit() == null) {
			return null;
    	}
		Limit limit = queryCommand.getLimit();
		OrderBy orderBy = queryCommand.getOrderBy();
		queryCommand.setLimit(null);
		queryCommand.setOrderBy(null);
		List<Object> parts = new ArrayList<Object>(6);
		parts.add("SELECT "); //$NON-NLS-1$
		parts.addAll(translateLimit(limit, context));
		parts.add(" * FROM ("); //$NON-NLS-1$
		parts.add(queryCommand);
		parts.add(") AS X"); //$NON-NLS-1$
		if (orderBy != null) {
			parts.add(" "); //$NON-NLS-1$
			parts.add(orderBy);
		}
		return parts;
    }
    
    @SuppressWarnings("unchecked")
	@Override
    public List<?> translateLimit(Limit limit, ExecutionContext context) {
    	return Arrays.asList("TOP ", limit.getRowLimit()); //$NON-NLS-1$
    }
    
    @Override
    public boolean useSelectLimit() {
    	return true;
    }
    
    @Override
    public int getMaxInCriteriaSize() {
    	return 250;
    }
    
    @Override
    public List<String> getSupportedFunctions() {
        List<String> supportedFunctions = new ArrayList<String>();
        supportedFunctions.addAll(super.getSupportedFunctions());
        supportedFunctions.add("ABS"); //$NON-NLS-1$
        supportedFunctions.add("ACOS"); //$NON-NLS-1$
        supportedFunctions.add("ASIN"); //$NON-NLS-1$
        supportedFunctions.add("ATAN"); //$NON-NLS-1$
        supportedFunctions.add("ATAN2"); //$NON-NLS-1$
        supportedFunctions.add("COS"); //$NON-NLS-1$
        supportedFunctions.add("COT"); //$NON-NLS-1$
        supportedFunctions.add("DEGREES"); //$NON-NLS-1$
        supportedFunctions.add("EXP"); //$NON-NLS-1$
        supportedFunctions.add("FLOOR"); //$NON-NLS-1$
        supportedFunctions.add("LOG"); //$NON-NLS-1$
        supportedFunctions.add("LOG10"); //$NON-NLS-1$
        supportedFunctions.add("MOD"); //$NON-NLS-1$
        supportedFunctions.add("PI"); //$NON-NLS-1$
        supportedFunctions.add("POWER"); //$NON-NLS-1$
        supportedFunctions.add("RADIANS"); //$NON-NLS-1$
        supportedFunctions.add("SIGN"); //$NON-NLS-1$
        supportedFunctions.add("SIN"); //$NON-NLS-1$
        supportedFunctions.add("SQRT"); //$NON-NLS-1$
        supportedFunctions.add("TAN"); //$NON-NLS-1$
        supportedFunctions.add("ASCII"); //$NON-NLS-1$
        supportedFunctions.add("CHAR"); //$NON-NLS-1$
        supportedFunctions.add("CHR"); //$NON-NLS-1$
        supportedFunctions.add("CONCAT"); //$NON-NLS-1$
        supportedFunctions.add("||"); //$NON-NLS-1$
        supportedFunctions.add("LCASE"); //$NON-NLS-1$
        supportedFunctions.add("LEFT"); //$NON-NLS-1$
        supportedFunctions.add("LENGTH"); //$NON-NLS-1$
        supportedFunctions.add("LOWER"); //$NON-NLS-1$
        supportedFunctions.add("LTRIM"); //$NON-NLS-1$
        supportedFunctions.add("REPEAT"); //$NON-NLS-1$
        //supportedFunctions.add("RAND"); //$NON-NLS-1$
        supportedFunctions.add("RIGHT"); //$NON-NLS-1$
        supportedFunctions.add("RTRIM"); //$NON-NLS-1$
        supportedFunctions.add("SPACE"); //$NON-NLS-1$
        supportedFunctions.add("SUBSTRING"); //$NON-NLS-1$
        supportedFunctions.add("UCASE"); //$NON-NLS-1$
        supportedFunctions.add("UPPER"); //$NON-NLS-1$
        //supportedFunctons.add("CURDATE"); //$NON-NLS-1$
        //supportedFunctons.add("CURTIME"); //$NON-NLS-1$
        supportedFunctions.add("DAYNAME"); //$NON-NLS-1$
        supportedFunctions.add("DAYOFMONTH"); //$NON-NLS-1$
        supportedFunctions.add("DAYOFWEEK"); //$NON-NLS-1$
        supportedFunctions.add("DAYOFYEAR"); //$NON-NLS-1$
        supportedFunctions.add("HOUR"); //$NON-NLS-1$
        supportedFunctions.add("MINUTE"); //$NON-NLS-1$
        supportedFunctions.add("MONTH"); //$NON-NLS-1$
        supportedFunctions.add("MONTHNAME"); //$NON-NLS-1$
        //supportedFunctions.add("NOW"); //$NON-NLS-1$
        supportedFunctions.add("QUARTER"); //$NON-NLS-1$
        supportedFunctions.add("SECOND"); //$NON-NLS-1$
        supportedFunctions.add("TIMESTAMPADD"); //$NON-NLS-1$
        supportedFunctions.add("TIMESTAMPDIFF"); //$NON-NLS-1$
        supportedFunctions.add("WEEK"); //$NON-NLS-1$
        supportedFunctions.add("YEAR"); //$NON-NLS-1$
        supportedFunctions.add("CAST"); //$NON-NLS-1$
        supportedFunctions.add("CONVERT"); //$NON-NLS-1$
        supportedFunctions.add("IFNULL"); //$NON-NLS-1$
        supportedFunctions.add("NVL");      //$NON-NLS-1$ 
        //supportedFunctions.add("FORMATTIMESTAMP");   //$NON-NLS-1$
        
        return supportedFunctions;
    }
    
    @Override
    public boolean supportsInlineViews() {
        return true;
    }

    @Override
    public boolean supportsFunctionsInGroupBy() {
        return true;
    }
    
    @Override
    public int getMaxFromGroups() {
        return 50;
    }    
}
