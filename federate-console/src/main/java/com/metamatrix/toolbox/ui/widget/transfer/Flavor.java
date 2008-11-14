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

//################################################################################################################################
package com.metamatrix.toolbox.ui.widget.transfer;

// JDK imports
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

/**
@since 2.0
@version 2.0
@author <a href="mailto:jverhaeg@metamatrix.com">John P. A. Verhaeg</a>
*/
public class Flavor extends DataFlavor {
    //############################################################################################################################
    //# Instance Variables                                                                                                       #
    //############################################################################################################################
    
    private Transferable xferable;

    //############################################################################################################################
    //# Constructors                                                                                                             #
    //############################################################################################################################
    
    public Flavor(final String mimeType, final String name) {
        super(mimeType, name);
    }
         
    //############################################################################################################################
    //# Instance Methods                                                                                                         #
    //############################################################################################################################

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
    @since 2.0
    */
    public Transferable getTransferable() {
        return xferable;
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
    @since 2.0
    */
    public void setTransferable(final Transferable transferable) {
        xferable = transferable;
    }
}
