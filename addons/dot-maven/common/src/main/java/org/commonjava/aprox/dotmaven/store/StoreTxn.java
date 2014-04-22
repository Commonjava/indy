/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.dotmaven.store;

import java.security.Principal;

import net.sf.webdav.spi.ITransaction;

public class StoreTxn
    implements ITransaction
{

    private final Principal principal;

    public StoreTxn( final Principal principal )
    {
        this.principal = principal;
    }

    @Override
    public Principal getPrincipal()
    {
        return principal;
    }

    @Override
    public String toString()
    {
        return String.format( "DotMavenTxn [principal=%s]", principal );
    }

}
