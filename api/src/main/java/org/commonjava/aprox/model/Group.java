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
package org.commonjava.aprox.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Group
    extends ArtifactStore
{

    private static final long serialVersionUID = 1L;

    private List<StoreKey> constituents;

    Group()
    {
    }

    public Group( final String name, final List<StoreKey> constituents )
    {
        super( name );
        this.constituents = constituents;
    }

    public Group( final String name, final StoreKey... constituents )
    {
        super( name );
        this.constituents = new ArrayList<StoreKey>( Arrays.asList( constituents ) );
    }

    public List<StoreKey> getConstituents()
    {
        return constituents;
    }

    public boolean addConstituent( final ArtifactStore store )
    {
        if ( store == null )
        {
            return false;
        }

        return addConstituent( store.getKey() );
    }

    public synchronized boolean addConstituent( final StoreKey repository )
    {
        if ( constituents == null )
        {
            constituents = new ArrayList<StoreKey>();
        }

        return constituents.add( repository );
    }

    public boolean removeConstituent( final ArtifactStore constituent )
    {
        return constituent == null ? false : removeConstituent( constituent.getKey() );
    }

    public boolean removeConstituent( final StoreKey repository )
    {
        return constituents == null ? false : constituents.remove( repository );
    }

    public void setConstituents( final List<StoreKey> constituents )
    {
        this.constituents = constituents;
    }

    public void setConstituentProxies( final List<RemoteRepository> constituents )
    {
        this.constituents = null;
        for ( final ArtifactStore proxy : constituents )
        {
            addConstituent( proxy );
        }
    }

    @Override
    public String toString()
    {
        return String.format( "Group [constituents=%s, getName()=%s, getKey()=%s]", constituents, getName(), getKey() );
    }

    @Override
    protected StoreKey initKey( final String name )
    {
        return new StoreKey( StoreType.group, name );
    }

}
