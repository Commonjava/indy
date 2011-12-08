/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.couch.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreKey;
import org.commonjava.aprox.core.model.StoreType;

public class GroupDoc
    extends AbstractArtifactStoreDoc
    implements Group
{

    private List<StoreKey> constituents;

    GroupDoc()
    {
        super( StoreType.group );
    }

    public GroupDoc( final String name, final List<StoreKey> constituents )
    {
        super( StoreType.group, name );
        this.constituents = constituents;
    }

    public GroupDoc( final String name, final StoreKey... constituents )
    {
        super( StoreType.group, name );
        this.constituents = new ArrayList<StoreKey>( Arrays.asList( constituents ) );
    }

    @Override
    public List<StoreKey> getConstituents()
    {
        return constituents;
    }

    @Override
    public boolean addConstituent( final ArtifactStore store )
    {
        if ( store == null )
        {
            return false;
        }

        return addConstituent( store.getKey() );
    }

    @Override
    public synchronized boolean addConstituent( final StoreKey repository )
    {
        if ( constituents == null )
        {
            constituents = new ArrayList<StoreKey>();
        }

        return constituents.add( repository );
    }

    @Override
    public boolean removeConstituent( final ArtifactStore constituent )
    {
        return constituent == null ? false : removeConstituent( constituent.getKey() );
    }

    @Override
    public boolean removeConstituent( final StoreKey repository )
    {
        return constituents == null ? false : constituents.remove( repository );
    }

    @Override
    public void setConstituents( final List<StoreKey> constituents )
    {
        this.constituents = constituents;
    }

    @Override
    public void setConstituentProxies( final List<Repository> constituents )
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
        return String.format( "Group [name=%s, constituents=%s]", getName(), constituents );
    }

}
