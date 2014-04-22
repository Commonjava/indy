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
package org.commonjava.aprox.model.galley;

import java.util.HashMap;
import java.util.Map;

import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.maven.galley.transport.htcli.model.HttpLocation;

public class RepositoryLocation
    implements HttpLocation, KeyedLocation
{

    private final RemoteRepository repository;

    private final Map<String, Object> attributes = new HashMap<String, Object>();

    public RepositoryLocation( final RemoteRepository repository )
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
