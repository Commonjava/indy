/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
        return String.format( "DotMavenTxn [principal={}]", principal );
    }

}
