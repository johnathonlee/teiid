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

package com.metamatrix.query.resolver.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import com.metamatrix.api.exception.MetaMatrixComponentException;
import com.metamatrix.api.exception.query.QueryMetadataException;
import com.metamatrix.api.exception.query.QueryResolverException;
import com.metamatrix.common.types.DataTypeManager;
import com.metamatrix.common.types.TransformationException;
import com.metamatrix.common.types.DataTypeManager.DefaultDataTypes;
import com.metamatrix.core.util.StringUtil;
import com.metamatrix.query.QueryPlugin;
import com.metamatrix.query.function.FunctionDescriptor;
import com.metamatrix.query.function.FunctionLibrary;
import com.metamatrix.query.function.FunctionLibraryManager;
import com.metamatrix.query.metadata.GroupInfo;
import com.metamatrix.query.metadata.QueryMetadataInterface;
import com.metamatrix.query.metadata.SupportConstants;
import com.metamatrix.query.metadata.TempMetadataAdapter;
import com.metamatrix.query.metadata.TempMetadataID;
import com.metamatrix.query.metadata.TempMetadataStore;
import com.metamatrix.query.sql.LanguageObject;
import com.metamatrix.query.sql.lang.Limit;
import com.metamatrix.query.sql.lang.OrderBy;
import com.metamatrix.query.sql.symbol.AggregateSymbol;
import com.metamatrix.query.sql.symbol.AliasSymbol;
import com.metamatrix.query.sql.symbol.Constant;
import com.metamatrix.query.sql.symbol.ElementSymbol;
import com.metamatrix.query.sql.symbol.Expression;
import com.metamatrix.query.sql.symbol.ExpressionSymbol;
import com.metamatrix.query.sql.symbol.Function;
import com.metamatrix.query.sql.symbol.GroupSymbol;
import com.metamatrix.query.sql.symbol.Reference;
import com.metamatrix.query.sql.symbol.SelectSymbol;
import com.metamatrix.query.sql.symbol.SingleElementSymbol;
import com.metamatrix.query.util.ErrorMessageKeys;

/**
 * Utilities used during resolution
 */
public class ResolverUtil {

    // Can't construct
    private ResolverUtil() {}

    /*
     *                        Type Conversion Utilities
     */

    /**
     * Gets the most specific type to which all the given types have an implicit
     * conversion. The method decides a common type as follows:
     * <ul>
     *   <li>If one or more of the given types is a candidate, then this method
     *       will return the candidate that occurs first in the given array.
     *       This is why the order of the names in the array is important. </li>
     *   <li>Otherwise, if none of them is a candidate, this method will attempt
     *       to find a common type to which all of them can be implicitly
     *       converted.</li>
     *   <li>Otherwise this method is unable to find a common type to which all
     *       the given types can be implicitly converted, and therefore returns
     *       a null.</li>
     * </ul>
     * @param typeNames an ordered array of unique type names.
     * @return a type name to which all the given types can be converted
     */
    public static String getCommonType(String[] typeNames) {
        if (typeNames == null || typeNames.length == 0) {
            return null;
        }
        // If there is only one type, then simply return it
        if (typeNames.length == 1) {
            return typeNames[0];
        }
        // A type can be implicitly converted to itself, so we put the implicit
        // conversions as well as the original type in the working list of
        // conversions.
        HashSet commonConversions = new LinkedHashSet();
        commonConversions.add(typeNames[0]);
        commonConversions.addAll(DataTypeManager.getImplicitConversions(typeNames[0]));
        for (int i = 1; i < typeNames.length; i++ ) {
            HashSet conversions = new LinkedHashSet(DataTypeManager.getImplicitConversions(typeNames[i]));
            conversions.add(typeNames[i]);
            // Equivalent to set intersection
            commonConversions.retainAll(conversions);
        }
        if (commonConversions.isEmpty()) {
            return null;
        }
        for (int i = 0; i < typeNames.length; i++) {
            if (commonConversions.contains(typeNames[i])) {
                return typeNames[i];
            }
        }
    	commonConversions.remove(DefaultDataTypes.STRING);
        commonConversions.remove(DefaultDataTypes.OBJECT);
        if (!commonConversions.isEmpty()) {
            return (String)commonConversions.iterator().next(); 
        }
        return null;
    }

    /**
     * Gets whether there exists an implicit conversion from the source type to
     * the target type
     * @param fromType
     * @param toType
     * @return true if there exists an implicit conversion from the
     * <code>fromType</code> to the <code>toType</code>.
     */
    public static boolean canImplicitlyConvert(String fromType, String toType) {
        if (fromType.equals(toType)) return true;
        return DataTypeManager.isImplicitConversion(fromType, toType);
    }

    /**
     * Replaces a sourceExpression with a conversion of the source expression
     * to the target type. If the source type and target type are the same,
     * this method does nothing.
     * @param sourceExpression
     * @param targetTypeName
     * @return
     * @throws QueryResolverException 
     */
    public static Expression convertExpression(Expression sourceExpression, String targetTypeName) throws QueryResolverException {
        return convertExpression(sourceExpression,
                                 DataTypeManager.getDataTypeName(sourceExpression.getType()),
                                 targetTypeName);
    }

    /**
     * Replaces a sourceExpression with a conversion of the source expression
     * to the target type. If the source type and target type are the same,
     * this method does nothing.
     * @param sourceExpression
     * @param sourceTypeName
     * @param targetTypeName
     * @return
     * @throws QueryResolverException 
     */
    public static Expression convertExpression(Expression sourceExpression, String sourceTypeName, String targetTypeName) throws QueryResolverException {
        if (sourceTypeName.equals(targetTypeName)) {
            return sourceExpression;
        }
        
        if(canImplicitlyConvert(sourceTypeName, targetTypeName) 
                        || (sourceExpression instanceof Constant && canConvertConstant(sourceTypeName, targetTypeName, (Constant)sourceExpression))) {
            return getConversion(sourceExpression, sourceTypeName, targetTypeName);
        }

        //Expression is wrong type and can't convert
        throw new QueryResolverException(ErrorMessageKeys.RESOLVER_0041, QueryPlugin.Util.getString(ErrorMessageKeys.RESOLVER_0041, new Object[] {targetTypeName, sourceExpression, sourceTypeName}));
    }

    private static boolean canConvertConstant(String sourceTypeName,
                                           String targetTypeName,
                                           Constant constant) throws QueryResolverException {
        if (DataTypeManager.isTransformable(sourceTypeName, targetTypeName)) {
            
            //try to get the converted constant, if this fails then it is not in a valid format
            Constant result = getProperlyTypedConstant(constant.getValue(), DataTypeManager.getDataTypeClass(targetTypeName));
            
            if (DataTypeManager.DefaultDataTypes.STRING.equals(sourceTypeName)) {
                return true;
            }
            
            //for non-strings, ensure that the conversion is consistent
            if (DataTypeManager.isTransformable(targetTypeName, sourceTypeName)) {
                Constant reverse = getProperlyTypedConstant(result.getValue(), constant.getType());
                
                if (constant.equals(reverse)) {
                    return true;
                }
            }
        }
            
        return false;
    }

    private static Expression getConversion(Expression sourceExpression,
                                            String sourceTypeName,
                                            String targetTypeName) {
        Class srcType = DataTypeManager.getDataTypeClass(sourceTypeName);

        FunctionLibrary library = FunctionLibraryManager.getFunctionLibrary();
        FunctionDescriptor fd = library.findFunction(FunctionLibrary.CONVERT, new Class[] { srcType, DataTypeManager.DefaultDataClasses.STRING });

        Function conversion = new Function(fd.getName(), new Expression[] { sourceExpression, new Constant(targetTypeName) });
        conversion.setType(DataTypeManager.getDataTypeClass(targetTypeName));
        conversion.setFunctionDescriptor(fd);
        conversion.makeImplicit();

        return conversion;
    }

    /**
     * Utility to set the type of an expression if it is a Reference and has a null type.
     * @param expression the expression to test
     * @param targetType the target type, if the expression's type is null.
     * @throws QueryResolverException if unable to set the reference type to the target type.
     */
    public static void setTypeIfReference(Expression expression, Class targetType, LanguageObject surroundingExpression) throws QueryResolverException {
        if ((expression instanceof Reference) && expression.getType() == null) {
        	if (targetType == null) {
        		throw new QueryResolverException(ErrorMessageKeys.RESOLVER_0026, QueryPlugin.Util.getString(ErrorMessageKeys.RESOLVER_0026, surroundingExpression));
        	}
            Constant dummy = new Constant(null, targetType);
            ((Reference)expression).setExpression(dummy);
        }
    }
    
    /**
    * Attempt to resolve the order by
    * throws QueryResolverException if the symbol is not of SingleElementSymbol type
    * @param orderBy
    * @param fromClauseGroups groups of the FROM clause of the query (for 
    * resolving ambiguous unqualified element names), or empty List if a Set Query
    * Order By is being resolved
    * @param knownElements resolved elements from SELECT clause, which are only 
    * ones allowed to be in ORDER BY 
    * @param metadata QueryMetadataInterface
    */
    public static void resolveOrderBy(OrderBy orderBy, List fromClauseGroups, List knownElements, QueryMetadataInterface metadata)
        throws QueryResolverException, QueryMetadataException, MetaMatrixComponentException {

        orderBy.setInPlanForm(false);
        
        // Cached state, if needed
        String[] knownShortNames = null;

        // Collect all elements from order by
        List elements = orderBy.getVariables();
        Iterator elementIter = elements.iterator();

        // Walk through elements of order by
        while(elementIter.hasNext()){
            ElementSymbol symbol = (ElementSymbol) elementIter.next();
            SingleElementSymbol matchedSymbol = null;
            String symbolName = symbol.getName();
            String groupPart = metadata.getGroupName(symbolName);
            String shortName = symbol.getShortName();
            
            //check for union order by (allow uuids to skip this check)
            if (fromClauseGroups.isEmpty() && groupPart != null && !shortName.equals(symbolName)) {
                throw new QueryResolverException(ErrorMessageKeys.RESOLVER_0043, QueryPlugin.Util.getString(ErrorMessageKeys.RESOLVER_0043, symbolName));
            }

            // construct the array of short names of the SELECT columns (once only)
            if(knownShortNames == null) {
                knownShortNames = new String[knownElements.size()];

                for(int i=0; i<knownElements.size(); i++) {
                    SingleElementSymbol knownSymbol = (SingleElementSymbol) knownElements.get(i);
                    if (knownSymbol instanceof ExpressionSymbol) {
                        continue;
                    }
                    
                    String name = knownSymbol.getShortName();
                    //special check for uuid element symbols
                    if (knownSymbol instanceof ElementSymbol && knownSymbol.getShortName().equalsIgnoreCase(knownSymbol.getName())) {
                        name = metadata.getShortElementName(metadata.getFullName((((ElementSymbol)knownSymbol).getMetadataID())));
                    }  
                    
                    knownShortNames[i] = name;
                }
            }
            // walk the SELECT col short names, looking for a match on the current ORDER BY 'short name'
            for(int i=0; i<knownShortNames.length; i++) {
            	if( shortName.equalsIgnoreCase( knownShortNames[i] )) {
                    if (groupPart != null) {
                        Object knownSymbol = knownElements.get(i);
                        if(knownSymbol instanceof ElementSymbol) {
                            ElementSymbol knownElement = (ElementSymbol) knownSymbol;
                            GroupSymbol group = knownElement.getGroupSymbol();
                            
                            // skip this one if the two short names are not from the same group
                            if (!nameMatchesGroup(groupPart.toUpperCase(), group.getCanonicalName())) {
                                continue;
                            }
                        }
                    }
                    
                    // if we already have a matched symbol, matching again here means it is duplicate/ambiguous
                    if(matchedSymbol != null) {
                        throw new QueryResolverException(ErrorMessageKeys.RESOLVER_0042, QueryPlugin.Util.getString(ErrorMessageKeys.RESOLVER_0042, symbolName));
                    }
                    matchedSymbol = (SingleElementSymbol)knownElements.get(i);
                }
            }
                        
            // this clause handles the order by clause like  
            // select foo from bar order by "1"; where 1 is foo. 
            if (matchedSymbol == null && StringUtil.isDigits(symbolName)) {
                int elementOrder = Integer.valueOf(symbolName).intValue() - 1;
                // adjust for the 1 based index.
                if (elementOrder < knownElements.size() && elementOrder >= 0) {
                    matchedSymbol = (SingleElementSymbol)knownElements.get(elementOrder);
                    
                    for(int i=0; i<knownShortNames.length; i++) {
                        if (i == elementOrder) {
                            continue;
                        }
                        if (matchedSymbol.getShortCanonicalName().equalsIgnoreCase(knownShortNames[i])) {
                            throw new QueryResolverException(ErrorMessageKeys.RESOLVER_0042, QueryPlugin.Util.getString(ErrorMessageKeys.RESOLVER_0042, knownShortNames[i]));
                        }
                    }
                }
            }

            if(matchedSymbol == null) {
                // Didn't find it by full name or short name, so try resolving
                // and retrying by full name - this will work for uuid case
                try {
                	ResolverVisitor.resolveLanguageObject(symbol, fromClauseGroups, metadata);
                } catch(QueryResolverException e) {
                	throw new QueryResolverException(e, ErrorMessageKeys.RESOLVER_0043, QueryPlugin.Util.getString(ErrorMessageKeys.RESOLVER_0043, symbol.getName()) );
                }

                matchedSymbol = findMatchingElementByID(symbol, knownElements);
            }
                        
            TempMetadataID tempMetadataID = new TempMetadataID(symbol.getName(), matchedSymbol.getType());
            tempMetadataID.setPosition(knownElements.indexOf(matchedSymbol));
            symbol.setMetadataID(tempMetadataID);
            symbol.setType(matchedSymbol.getType());
        }
    }

    /**
     * Helper for resolveOrderBy to find a matching fully-qualified element in a list of
     * projected SELECT symbols.
     * @throws QueryResolverException 
     */
    private static SingleElementSymbol findMatchingElementByID(ElementSymbol symbol, List knownElements) throws QueryResolverException {
        Object elementID = symbol.getMetadataID();
        
        if(elementID == null) {
            throw new QueryResolverException(ErrorMessageKeys.RESOLVER_0043, QueryPlugin.Util.getString(ErrorMessageKeys.RESOLVER_0043, symbol.getName()));
        }
        
        Object groupID = symbol.getGroupSymbol().getMetadataID();

        for(int i=0; i<knownElements.size(); i++) {
            SingleElementSymbol selectSymbol = (SingleElementSymbol)knownElements.get(i);
            SingleElementSymbol knownSymbol = null;
            if(selectSymbol instanceof AliasSymbol) {
            	knownSymbol = ((AliasSymbol)selectSymbol).getSymbol();
            }

            if(knownSymbol instanceof ElementSymbol) {
                ElementSymbol knownElement = (ElementSymbol) knownSymbol;
                Object knownElementID = knownElement.getMetadataID();

                if(elementID.equals(knownElementID)) {
                    Object knownGroupID = knownElement.getGroupSymbol().getMetadataID();
                    if(groupID.equals(knownGroupID)) {
                        return selectSymbol;
                    }
                }
            }
        }

        throw new QueryResolverException(ErrorMessageKeys.RESOLVER_0043, QueryPlugin.Util.getString(ErrorMessageKeys.RESOLVER_0043, symbol.getName()));
    }

    /** 
     * Get the default value for the parameter, which could be null
     * if the parameter is set to NULLABLE.  If no default is available,
     * a QueryResolverException will be thrown.
     * 
     * @param symbol ElementSymbol retrieved from metadata, fully-resolved
     * @param metadata QueryMetadataInterface
     * @return expr param (if it is non-null) or default value (if there is one)
     * or null Constant (if parameter is optional and therefore allows this)
     * @throws QueryResolverException if expr is null, parameter is required and no
     * default value is defined
     * @throws QueryMetadataException for error retrieving metadata
     * @throws MetaMatrixComponentException
     * @since 4.3
     */
	public static Expression getDefault(ElementSymbol symbol, QueryMetadataInterface metadata) throws MetaMatrixComponentException, QueryMetadataException, QueryResolverException {
        //Check if there is a default value, if so use it
		Object mid = symbol.getMetadataID();
    	Class type = symbol.getType();
    	String name = symbol.getCanonicalName();
		
        Object defaultValue = metadata.getDefaultValue(mid);
        
        if (defaultValue == null && !metadata.elementSupports(mid, SupportConstants.Element.NULL)) {
            throw new QueryResolverException(QueryPlugin.Util.getString("ResolverUtil.required_param", name)); //$NON-NLS-1$
        }
        
        return getProperlyTypedConstant(defaultValue, type);
	}    
    
    /** 
     * Construct a Constant with proper type, given the String default
     * value for the parameter and the parameter type.  Throw a
     * QueryResolverException if the String can't be transformed.
     * @param defaultValue, either null or a String
     * @param parameterType modeled type of parameter (MetaMatrix runtime type)
     * @return Constant with proper type and default value Object of proper Class.  Will
     * be null Constant if defaultValue is null.
     * @throws QueryResolverException if TransformationException is encountered
     * @since 4.3
     */
    private static Constant getProperlyTypedConstant(Object defaultValue,
                                                Class parameterType)
    throws QueryResolverException{
        try {
            Object newValue = DataTypeManager.transformValue(defaultValue, parameterType);
            return new Constant(newValue, parameterType);
        } catch (TransformationException e) {
            throw new QueryResolverException(e, QueryPlugin.Util.getString("ResolverUtil.error_converting_value_type", defaultValue, defaultValue.getClass(), parameterType)); //$NON-NLS-1$
        }
    }

    /**
     * Returns the resolved elements in the given group.  This method has the side effect of caching the resolved elements on the group object.
     * The resolved elements may not contain non-selectable columns depending on the metadata first used for resolving.
     * 
     */
    public static List resolveElementsInGroup(GroupSymbol group, QueryMetadataInterface metadata)
    throws QueryMetadataException, MetaMatrixComponentException {
        return new ArrayList(getGroupInfo(group, metadata).getSymbolList());
    }
    
	static GroupInfo getGroupInfo(GroupSymbol group,
			QueryMetadataInterface metadata)
			throws MetaMatrixComponentException, QueryMetadataException {
		String key = GroupInfo.CACHE_PREFIX + group.getCanonicalName();
		GroupInfo groupInfo = (GroupInfo)metadata.getFromMetadataCache(group.getMetadataID(), key);
    	
        if (groupInfo == null) {
        	group = (GroupSymbol)group.clone();
            // get all elements from the metadata
            List elementIDs = metadata.getElementIDsInGroupID(group.getMetadataID());

    		LinkedHashMap<Object, ElementSymbol> symbols = new LinkedHashMap<Object, ElementSymbol>(elementIDs.size());
                        
            boolean groupIsAliased = group.getDefinition() != null;
            
            for (Object elementID : elementIDs) {
                String elementName = metadata.getFullName(elementID);
                String fullName = elementName;
    			// This is only really needed if the group is an ALIAS.  Doing the check outside the loop
                // and NOT doing unnecessary work if Aliased group.
                if(groupIsAliased) {
                	String shortName = metadata.getShortElementName(elementName);
                    fullName = metadata.getFullElementName(group.getName(), shortName);
                }

                // Form an element symbol from the ID
                ElementSymbol element = new ElementSymbol(fullName);
                element.setGroupSymbol(group);
                element.setMetadataID(elementID);
                element.setType( DataTypeManager.getDataTypeClass(metadata.getElementType(element.getMetadataID())) );

                symbols.put(elementID, element);
            }
            groupInfo = new GroupInfo(symbols);
            metadata.addToMetadataCache(group.getMetadataID(), key, groupInfo);
        }
		return groupInfo;
	}
    
    public static List resolveElements(GroupSymbol group, QueryMetadataInterface metadata, List elementIDs) throws MetaMatrixComponentException, QueryMetadataException {
    	GroupInfo groupInfo = getGroupInfo(group, metadata);
    	List result = new ArrayList(elementIDs.size());
    	for (Iterator iterator = elementIDs.iterator(); iterator.hasNext();) {
			Object id = iterator.next();
			ElementSymbol symbol = groupInfo.getSymbol(id);
			assert symbol != null;
			result.add(symbol);
		}
    	return result;
    }
        
    /**
     * When access patterns are flattened, they are an approximation the user
     * may need to enter as criteria.
     *  
     * @param metadata
     * @param groups
     * @param flatten
     * @return
     * @throws MetaMatrixComponentException
     * @throws QueryMetadataException
     */
	public static List getAccessPatternElementsInGroups(final QueryMetadataInterface metadata, Collection groups, boolean flatten) throws MetaMatrixComponentException, QueryMetadataException {
		List accessPatterns = null;
		Iterator i = groups.iterator();
		while (i.hasNext()){
		    
		    GroupSymbol group = (GroupSymbol)i.next();
		    
		    //Check this group for access pattern(s).
		    Collection accessPatternIDs = metadata.getAccessPatternsInGroup(group.getMetadataID());
		    if (accessPatternIDs != null && accessPatternIDs.size() > 0){
		        Iterator j = accessPatternIDs.iterator();
		        if (accessPatterns == null){
		            accessPatterns = new ArrayList();
		        }
		        while (j.hasNext()) {
		        	List elements = metadata.getElementIDsInAccessPattern(j.next());
		        	elements = resolveElements(group, metadata, elements);
		        	if (flatten) {
		        		accessPatterns.addAll(elements);
		        	} else {
		        		accessPatterns.add(new AccessPattern(elements));
		        	}
		        }
		    }
		}
                
		return accessPatterns;
	}

    public static void resolveLimit(Limit limit) throws QueryResolverException {
        if (limit.getOffset() != null) {
            setTypeIfReference(limit.getOffset(), DataTypeManager.DefaultDataClasses.INTEGER, limit);
        }
        setTypeIfReference(limit.getRowLimit(), DataTypeManager.DefaultDataClasses.INTEGER, limit);
    }
    
    public static void resolveImplicitTempGroup(TempMetadataAdapter metadata, GroupSymbol symbol, List symbols) 
        throws MetaMatrixComponentException, QueryResolverException {
        
        if (symbol.isImplicitTempGroupSymbol()) {
            if (metadata.getMetadataStore().getTempElementElementIDs(symbol.getCanonicalName())==null) {
                addTempGroup(metadata, symbol, symbols, true);
            }
            ResolverVisitorUtil.resolveGroup(symbol, metadata);
        }
    }

    public static void addTempGroup(TempMetadataAdapter metadata,
                                    GroupSymbol symbol,
                                    List symbols, boolean tempTable) throws QueryResolverException {
        HashSet names = new HashSet();
        for (Iterator i = symbols.iterator(); i.hasNext();) {
            SingleElementSymbol ses = (SingleElementSymbol)i.next();
            if (!names.add(ses.getShortCanonicalName())) {
                throw new QueryResolverException(QueryPlugin.Util.getString("ResolverUtil.duplicateName", symbol, ses.getShortName())); //$NON-NLS-1$
            }
        }
        
        if (tempTable) {
            resolveNullLiterals(symbols);
        }
        TempMetadataStore store = metadata.getMetadataStore();
        store.addTempGroup(symbol.getName(), symbols, !tempTable, tempTable);
    }
    
    public static void addTempTable(TempMetadataAdapter metadata,
                                     GroupSymbol symbol,
                                     List symbols) throws QueryResolverException {
        addTempGroup(metadata, symbol, symbols, true);
    }

    /** 
     * Look for a null literal in the SELECT clause and set it's type to STRING.  This ensures that 
     * the result set metadata retrieved for this query will be properly set to something other than
     * the internal NullType.  Added for defect 15437.
     * 
     * @param select The select clause
     * @since 4.2
     */
    public static void resolveNullLiterals(List symbols) {
        for (int i = 0; i < symbols.size(); i++) {
            SelectSymbol symbol = (SelectSymbol) symbols.get(i);
            if(symbol instanceof AliasSymbol) {
                symbol = ((AliasSymbol)symbol).getSymbol();
            }
            
            Class replacement = DataTypeManager.DefaultDataClasses.STRING;
            
            if(symbol instanceof ExpressionSymbol && !(symbol instanceof AggregateSymbol)) {
                ExpressionSymbol exprSymbol = (ExpressionSymbol) symbol;
                Expression expr = exprSymbol.getExpression();
                if(expr != null && expr instanceof Constant && ((Constant)expr).isNull()) {
                    exprSymbol.setExpression(new Constant(null, replacement));
                } else {
                	try {
						ResolverUtil.setTypeIfReference(expr, replacement, symbol);
					} catch (QueryResolverException e) {
						//cannot happen
					}
                }
            } else if(symbol instanceof ElementSymbol) {
                ElementSymbol elementSymbol = (ElementSymbol)symbol;
                Class elementType = elementSymbol.getType();
                if(elementType != null && elementType.equals(DataTypeManager.DefaultDataClasses.NULL)) {
                    elementSymbol.setType(replacement);
                }
            }
        }
    }
    
    /**
     *  
     * @param groupContext
     * @param groups
     * @param metadata
     * @return the List of groups that match the given groupContext out of the supplied collection
     * @throws MetaMatrixComponentException
     * @throws QueryMetadataException
     */
    public static List findMatchingGroups(String groupContext,
                            Collection groups,
                            QueryMetadataInterface metadata) throws MetaMatrixComponentException,
                                                            QueryMetadataException {

        if (groups == null) {
            return null;
        }

        LinkedList matchedGroups = new LinkedList();

        if (groupContext == null) {
            matchedGroups.addAll(groups);
        } else {
            Iterator iter = groups.iterator();
            while (iter.hasNext()) {
                GroupSymbol group = (GroupSymbol)iter.next();
                String fullName = group.getCanonicalName();
                if (nameMatchesGroup(groupContext, matchedGroups, group, fullName)) {
                    if (groupContext.length() == fullName.length()) {
                        return matchedGroups;
                    }
                    continue;
                }

                // don't try to vdb qualify temp metadata
                if (group.getMetadataID() instanceof TempMetadataID) {
                    continue;
                }

                String actualVdbName = metadata.getVirtualDatabaseName();

                if (actualVdbName != null) {
                    fullName = actualVdbName.toUpperCase() + ElementSymbol.SEPARATOR + fullName;
                    if (nameMatchesGroup(groupContext, matchedGroups, group, fullName)
                        && groupContext.length() == fullName.length()) {
                        return matchedGroups;
                    }
                }
            }
        }

        return matchedGroups;
    }

    
    private static boolean nameMatchesGroup(String groupContext,
                                            String fullName) {
        //if there is a name match, make sure that it is the full name or a proper qualifier
        if (fullName.endsWith(groupContext)) {
            int matchIndex = fullName.length() - groupContext.length();
            if (matchIndex == 0 || fullName.charAt(matchIndex - 1) == '.') {
                return true;
            }
        }
        return false;
    }
    
    private static boolean nameMatchesGroup(String groupContext,
                                            LinkedList matchedGroups,
                                            GroupSymbol group,
                                            String fullName) {
        if (nameMatchesGroup(groupContext, fullName)) {
            matchedGroups.add(group);
            return true;
        }
        return false;
    }
    
    /**
     * Checks if a variable is in the ORDER BY
     * @param position 0-based index of the variable
     * @return True if the ORDER BY contains the element
     */
    public static boolean orderByContainsVariable(OrderBy orderBy, SingleElementSymbol ses, int position) {
        if (!orderBy.isInPlanForm()) {
            for (final Iterator iterator = orderBy.getVariables().iterator(); iterator.hasNext();) {
                final ElementSymbol element = (ElementSymbol)iterator.next();
                if (position == ((TempMetadataID)element.getMetadataID()).getPosition()) {
                    return true;
                }
            } 
        } else {
            return orderBy.getVariables().contains(ses);
        }
        return false;
    }
    
}
