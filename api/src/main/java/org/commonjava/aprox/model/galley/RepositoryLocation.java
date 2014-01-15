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
package org.commonjava.aprox.model.galley;

import java.util.HashMap;
import java.util.Map;

import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.maven.galley.transport.htcli.model.HttpLocation;

public class RepositoryLocation
    implements HttpLocation, KeyedLocation
{

    private final Repository repository;

    private final Map<String, Object> attributes = new HashMap<String, Object>();

    public RepositoryLocation( final Repository repository )
    {
        this.repository = repository;
    }

    @Override
    public boolean allowsPublishing()
    {
        return false;
    }

    @Override
    public boolean allowsStoring()
    {
        return true;
    }

    @Override
    public boolean allowsSnapshots()
    {
        return true;
    }

    @Override
    public boolean allowsReleases()
    {
        return true;
    }

    @Override
    public String getUri()
    {
        return repository.getUrl();
    }

    @Override
    public int getTimeoutSeconds()
    {
        return repository.getTimeoutSeconds();
    }

    @Override
    public Map<String, Object> getAttributes()
    {
        return attributes;
    }

    @Override
    public <T> T getAttribute( final String key, final Class<T> type )
    {
        final Object value = attributes.get( key );
        return value == null ? null : type.cast( value );
    }

    @Override
    public Object removeAttribute( final String key )
    {
        return attributes.remove( key );
    }

    @Override
    public Object setAttribute( final String key, final Object value )
    {
        return attributes.put( key, value );
    }

    @Override
    public String getKeyCertPem()
    {
        return repository.getKeyCertPem();
    }

    @Override
    public String getUser()
    {
        return repository.getUser();
    }

    @Override
    public String getHost()
    {
        return repository.getHost();
    }

    @Override
    public int getPort()
    {
        return repository.getPort();
    }

    @Override
    public String getServerCertPem()
    {
        return repository.getServerCertPem();
    }

    @Override
    public String getProxyHost()
    {
        return repository.getProxyHost();
    }

    @Override
    public int getProxyPort()
    {
        return repository.getProxyPort();
    }

    @Override
    public String getProxyUser()
    {
        return repository.getProxyUser();
    }

    @Override
    public StoreKey getKey()
    {
        return repository.getKey();
    }

    @Override
    public boolean allowsDownloading()
    {
        return true;
    }

    @Override
    public String toString()
    {
        return "RepositoryLocation [" + repository.getKey() + "]";
    }

    @Override
    public String getName()
    {
        return getKey().toString();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( repository == null ) ? 0 : repository.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final RepositoryLocation other = (RepositoryLocation) obj;
        if ( repository == null )
        {
            if ( other.repository != null )
            {
                return false;
            }
        }
        else if ( !repository.equals( other.repository ) )
        {
            return false;
        }
        return true;
    }
}
