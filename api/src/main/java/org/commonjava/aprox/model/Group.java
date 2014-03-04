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
package org.commonjava.aprox.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.wordnik.swagger.annotations.ApiClass;

@ApiClass( description = "Representation of a grouping of artifact stores, which may be any mix of repositories, deploy-points, and other groups.", value = "Group of artifact stores" )
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
