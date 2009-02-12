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

package com.metamatrix.common.tree;

import java.util.List;

import com.metamatrix.common.object.ObjectDefinition;

/**
 * This interface is used to encapsulate the rules that govern which types of
 * nodes may have children.
 */
public class DefaultChildRules implements ChildRules {
    public boolean isHidden(ObjectDefinition defn) {
        return false;
    }
    public boolean getAllowsChildren(ObjectDefinition defn) {
        return true;
    }
    public boolean getAllowsChild(ObjectDefinition parentDefn, ObjectDefinition childDefn) {
        return getAllowsChildren(parentDefn);
    }
    public void setValidName(TreeNode newNode, List children ) {
    }
    public void setValidName(TreeNode newNode, ObjectDefinition childType, List children ) {
    }
}

