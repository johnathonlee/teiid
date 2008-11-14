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

package com.metamatrix.common.extensionmodule.protocol;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Iterator;
import java.util.List;

import com.metamatrix.common.extensionmodule.protocol.extensionjar.Handler;

/**
 * Factory to convert String urls into URL objects.  This factory
 * will instantiate both "normal" URLs as well as special MetaMatrix
 * extension module URLs (URLs of resources in the MetaMatrix store
 * of extension modules).
 */
public class URLFactory {
   
    static{
        //Very important that this property is set, so that loading of 
        //custom extension module URLHandlers will work

        String propKey = "java.protocol.handler.pkgs"; //$NON-NLS-1$
        String directory = "com.metamatrix.common.extensionmodule.protocol"; //$NON-NLS-1$
        
        String value = System.getProperty(propKey);
        if (value == null){
            System.setProperty(propKey, directory);
        } else if (value.trim().length() == 0){
            System.setProperty(propKey, directory);
        } else if (value.indexOf(directory) < 0){
            value = value + "|" + directory; //$NON-NLS-1$
            System.setProperty(propKey, value);
        }
    }
   
    /**
     * The protocol of a MetaMatrix jar file extension module URL
     */
    public static final String MM_JAR_PROTOCOL = "extensionjar"; //$NON-NLS-1$
    
    private static final String SEPARATOR = ":"; //$NON-NLS-1$

    private static final String QUESTION_MARK = "?"; //$NON-NLS-1$

    /**
     * Can't instantiate
     */
    private URLFactory() {
        super();
    }

    /**
     * Utility method needed by extension module framework, should not
     * be called directly.
     */
    public static String getFileName(String url){
        String filename = url; 
        
        if (filename.indexOf(MM_JAR_PROTOCOL + SEPARATOR) >= 0){
            filename = filename.substring(url.lastIndexOf(SEPARATOR) + SEPARATOR.length());
        }
        if (filename.indexOf(QUESTION_MARK) >= 0){
            filename = filename.substring(0, filename.indexOf(QUESTION_MARK));
        }

        return filename;
        
    }

    /**
     * Parse the url string into a URL object within the given context.
     * @param context URL the context within which to parse the url string.
     * @param url String url
     * @return URL object
     * @throws MalformedURLException if url String is invalid
     */
    public static URL parseURL(URL context, String url) throws MalformedURLException {
        URL result = null;
        
        if (url.indexOf(MM_JAR_PROTOCOL + SEPARATOR) >= 0){
            String filename = url.substring(url.lastIndexOf(SEPARATOR) + SEPARATOR.length());
            result = new URL(MM_JAR_PROTOCOL, "", -1, filename, new Handler()); //$NON-NLS-1$
        } else {
            result = new URL(context, url);
        }
        return result;
    }
    
    /**
     * Parse the url string into a URL object
     * @param url String url
     * @return URL object
     * @throws MalformedURLException if url String is invalid
     */
    public static URL parseURL(String url) throws MalformedURLException {
        return parseURL(null, url);
    }
    
    /**
     * Parse the List of string urls into an array of 
     * URL objects
     * @param urls List of String urls
     * @return array of URL objects
     * @throws MalformedURLException if any of the url Strings
     * are invalid
     */
    public static URL[] parseURLs(List urls) throws MalformedURLException{
        URL[] result = new URL[urls.size()];
        
        Iterator urlIter = urls.iterator();
        for (int i=0; urlIter.hasNext(); i++){
            result[i] = parseURL((String)urlIter.next());
        }
        
        return result;
    }

    /**
     * Parse the single delimited String of URLs into an array of URL objects 
     * @param delimitedURLs a single String of delimiter-separated URL strings
     * @param delimiter delimiter of tokens in delmitedURLs - if this is null, a single 
     * whitespace will be assumed as the delimiter
     * @return URL[] array
     * @throws MalformedURLException if any of the url Strings are invalid
     */
    public static URL[] parseURLs(String delimitedURLs, String delimiter) throws MalformedURLException{
        
        if (delimiter == null){
            delimiter = " "; //$NON-NLS-1$
        }
        StringTokenizer toke = new StringTokenizer(delimitedURLs, delimiter);
        List urlStrings = new ArrayList(toke.countTokens());
        while (toke.hasMoreElements()) {
            String urlString = toke.nextToken();
            urlStrings.add(urlString);
        }
        return parseURLs(urlStrings);
    }
    
}
