/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.model.core;

import io.swagger.annotations.ApiModel;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@ApiModel( description = "Grouping of other artifact stores, with a defined order to the membership that determines content preference", parent = ArtifactStore.class )
public class Group
    extends ArtifactStore
{

    private static final long serialVersionUID = 1L;

    private List<StoreKey> constituents;

    private final Object monitor = new Object();

    Group()
    {
    }

    public Group( final String packageType, final String name, final List<StoreKey> constituents )
    {
        super( packageType, StoreType.group, name );
        this.constituents = new LinkedList<>( constituents );
    }

    @Deprecated
    public Group( final String name, final List<StoreKey> constituents )
    {
        super( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, StoreType.group, name );
        this.constituents = new LinkedList<>( constituents );
    }

    public Group( final String packageType, final String name, final StoreKey... constituents )
    {
        super( packageType, StoreType.group, name );
        this.constituents = new LinkedList<>( Arrays.asList( constituents ) );
    }

    @Deprecated
    public Group( final String name, final StoreKey... constituents )
    {
        super( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, StoreType.group, name );
        this.constituents = new LinkedList<>( Arrays.asList( constituents ) );
    }

    public List<StoreKey> getConstituents()
    {
        return constituents == null ? Collections.emptyList() : Collections.unmodifiableList( constituents );
    }

    public boolean addConstituent( final ArtifactStore store )
    {
        if ( store == null )
        {
            return false;
        }

        return addConstituent( store.getKey() );
    }

    public boolean addConstituent( final StoreKey repository )
    {
        if ( repository == null )
        {
            return false;
        }

        if ( constituents == null )
        {
            constituents = new LinkedList<>();
        }

        synchronized ( monitor )
        {
            if ( constituents.contains( repository ) )
            {
                return false;
            }

            // We will add new hosted repo as the first member in group, as this hosted will most likely
            // contain the most recent dependencies for the subsequent build.
            if ( repository.getType() == StoreType.hosted )
            {
                ( (LinkedList<StoreKey>) constituents ).push( repository );
                return true;
            }
            else
            {

                return constituents.add( repository );
            }
        }
    }

    public boolean removeConstituent( final ArtifactStore constituent )
    {
        return constituent != null && removeConstituent( constituent.getKey() );
    }

    public boolean removeConstituent( final StoreKey repository )
    {
        if ( constituents != null )
        {
            synchronized ( monitor )
            {
                return constituents.remove( repository );
            }
        }

        return false;
    }

    public void setConstituents( final List<StoreKey> constituents )
    {
        if ( this.constituents == null )
        {
            this.constituents = new LinkedList<>( constituents );
        }
        else
        {
            synchronized ( monitor )
            {
                this.constituents.clear();
                this.constituents.addAll( constituents );
            }
        }
    }

    @Override
    public String toString()
    {
        return String.format( "Group[%s]", getName() );
    }

    @Override
    public Group copyOf()
    {
        return copyOf( getPackageType(), getName() );
    }

    @Override
    public Group copyOf( final String packageType, final String name )
    {
        Group g = new Group( packageType, name, new LinkedList<>( getConstituents() ) );
        copyBase( g );

        return g;
    }

}
