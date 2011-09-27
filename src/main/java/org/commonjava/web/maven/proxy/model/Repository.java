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

import java.net.MalformedURLException;
import java.net.URL;

import org.commonjava.couch.model.AbstractCouchDocument;
import org.commonjava.couch.model.DenormalizedCouchDoc;
import org.commonjava.util.logging.Logger;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Repository
    extends AbstractCouchDocument
    implements DenormalizedCouchDoc
{
    public static final String NAMESPACE = "repository";

    private static final Logger LOGGER = new Logger( Repository.class );

    private String name;

    @SerializedName( "url" )
    private String url;

    private String host;

    private int port;

    private String user;

    private String password;

    @Expose( deserialize = false )
    private final String doctype = NAMESPACE;

    Repository()
    {}

    public Repository( final String name, final String remoteUrl )
    {
        setName( name );
        this.url = remoteUrl;
    }

    public String getName()
    {
        return name;
    }

    void setName( final String name )
    {
        this.name = name;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl( final String url )
    {
        this.url = url;
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
        Repository other = (Repository) obj;
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
        return String.format( "Repository [id='%s', rev='%s', name=%s, remoteUrl=%s]",
                              getCouchDocId(), getCouchDocRev(), name, url );
    }

    public String getDoctype()
    {
        return doctype;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser( final String user )
    {
        this.user = user;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword( final String password )
    {
        this.password = password;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost( final String host )
    {
        this.host = host;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort( final int port )
    {
        this.port = port;
    }

    @Override
    public void calculateDenormalizedFields()
    {
        setCouchDocId( namespaceId( NAMESPACE, name ) );

        URL url = null;
        try
        {
            url = new URL( this.url );
        }
        catch ( MalformedURLException e )
        {
            LOGGER.error( "Failed to parse repository URL: '%s'. Reason: %s", e, this.url,
                          e.getMessage() );
        }

        if ( url == null )
        {
            return;
        }

        String userInfo = url.getUserInfo();
        if ( userInfo != null && user == null && password == null )
        {
            user = userInfo;
            password = null;

            int idx = userInfo.indexOf( ':' );
            if ( idx > 0 )
            {
                user = userInfo.substring( 0, idx );
                password = userInfo.substring( idx + 1 );

                StringBuilder sb = new StringBuilder();
                idx = this.url.indexOf( "://" );
                sb.append( this.url.substring( 0, idx + 3 ) );

                idx = this.url.indexOf( "@" );
                if ( idx > 0 )
                {
                    sb.append( this.url.substring( idx + 1 ) );
                }

                this.url = sb.toString();
            }
        }

        host = url.getHost();
        if ( url.getPort() < 0 )
        {
            port = url.getProtocol().equals( "https" ) ? 443 : 80;
        }
        else
        {
            port = url.getPort();
        }
    }

}
