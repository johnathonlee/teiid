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

package com.metamatrix.core.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class SimpleMock extends MixinProxy {
	
	SimpleMock(Object[] baseInstances) {
		super(baseInstances);
	}
	
	@Override
	protected Object noSuchMethodFound(Object proxy, Method method,
			Object[] args) throws Throwable {
		Class clazz = method.getReturnType();
        
        if (clazz == Void.TYPE) {
            return null;
        }
        
        if (clazz.isPrimitive()) {
            return null;
        }

        if (!clazz.isInterface()) {
            try {
                Constructor c = clazz.getDeclaredConstructor(new Class[] {});
                if (c != null) {
                	try {
                		return c.newInstance(new Object[] {});
                	} catch (InvocationTargetException e) {
                		throw e.getTargetException();
                	}
                }
            } catch (NoSuchMethodException e) {
            }
            
            return null;
        }
        
        Class[] interfaces = clazz.getInterfaces();
        
        if (clazz.isInterface()) {
            interfaces = new Class[] {clazz}; 
        }

        if (interfaces != null && interfaces.length > 0) {
            return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), interfaces, this);
        }
        
        return null;
    }
	
	public static <T> T createSimpleMock(Class<T> clazz) {
		return (T)Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] {clazz}, new SimpleMock(new Object[] {}));
	}

	public static <T> T createSimpleMock(Object baseInstance, Class<T> clazz) {
		if (baseInstance instanceof Object[]) {
			return (T)Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] {clazz}, new SimpleMock((Object[])baseInstance));
		}
		return (T)Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] {clazz}, new SimpleMock(new Object[] {baseInstance}));
	}
	
	public static Object createSimpleMock(Object[] baseInstances, Class[] interfaces) {
		return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), interfaces, new SimpleMock(baseInstances));
	}
	
}
