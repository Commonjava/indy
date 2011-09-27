/*******************************************************************************
 * Copyright (C) 2011  John Casey
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see 
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.web.maven.proxy.model;

import static org.commonjava.couch.util.IdUtils.namespaceId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.commonjava.couch.model.AbstractCouchDocument;
import org.commonjava.couch.model.DenormalizedCouchDoc;

import com.google.gson.annotations.Expose;

public class Group
    extends AbstractCouchDocument
    implements DenormalizedCouchDoc
{

    public static final String NAMESPACE = "group";

    private String name;

    @Expose( deserialize = false )
    private final String doctype = NAMESPACE;

    private List<String> constituents;

    Group()
    {}

    public Group( final String name, final List<String> constituents )
    {
        this.name = name;
        this.constituents = constituents;
    }

    public Group( final String name, final String... constituents )
    {
        this.name = name;
        this.constituents = new ArrayList<String>( Arrays.asList( constituents ) );
    }

    public String getName()
    {
        return name;
    }

    void setName( final String name )
    {
        this.name = name;
    }

    public List<String> getConstituents()
    {
        return constituents;
    }

    public boolean addConstituent( final Repository repository )
    {
        if ( repository == null )
        {
            return false;
        }

        return addConstituent( repository.getName() );
    }

    public synchronized boolean addConstituent( final String repository )
    {
        if ( constituents == null )
        {
            constituents = new ArrayList<String>();
        }

        return constituents.add( repository );
    }

    public boolean removeConstituent( final Repository constituent )
    {
        return constituent == null ? false : removeConstituent( constituent.getName() );
    }

    public boolean removeConstituent( final String repository )
    {
        return constituents == null ? false : constituents.remove( repository );
    }

    public void setConstituents( final List<String> constituents )
    {
        this.constituents = constituents;
    }

    public void setConstituentProxies( final List<Repository> constituents )
    {
        this.constituents = null;
        for ( Repository proxy : constituents )
        {
            addConstituent( proxy );
        }
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( !super.equals( obj ) )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        Group other = (Group) obj;
        if ( name == null )
        {
            if ( other.name != null )
            {
                return false;
            }
        }
        else if ( !name.equals( other.name ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "Group [name=%s, constituents=%s]", name, constituents );
    }

    public String getDoctype()
    {
        return doctype;
    }

    @Override
    public void calculateDenormalizedFields()
    {
        setCouchDocId( namespaceId( NAMESPACE, name ) );
    }

}
