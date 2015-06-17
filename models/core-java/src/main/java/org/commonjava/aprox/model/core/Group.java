/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.aprox.model.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.wordnik.swagger.annotations.ApiModel;

@ApiModel( description = "Grouping of other artifact stores, with a defined order to the membership that determines content preference", parent = ArtifactStore.class )
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
        return constituents == null ? Collections.<StoreKey> emptyList() : constituents;
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
        if ( repository == null )
        {
            return false;
        }

        if ( constituents == null )
        {
            constituents = new ArrayList<StoreKey>();
        }

        if ( constituents.contains( repository ) )
        {
            return false;
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
