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

import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.annotations.SerializedName;
import com.wordnik.swagger.annotations.ApiClass;

@ApiClass( description = "Representation of a remote repository (proxy) definition.", value = "Remote repository proxy" )
public class RemoteRepository
    extends ArtifactStore
{
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger( RemoteRepository.class );

    public static final int DEFAULT_TIMEOUT_SECONDS = 120;

    @SerializedName( "url" )
    private String url;

    @SerializedName( "timeout_seconds" )
    private int timeoutSeconds;

    private String host;

    private int port;

    private String user;

    private String password;

    @SerializedName( "is_passthrough" )
    private boolean passthrough;

    @SerializedName( "cache_timeout_seconds" )
    private int cacheTimeoutSeconds;

    @SerializedName( "key_password" )
    private String keyPassword;

    @SerializedName( "key_certificate_pem" )
    private String keyCertificatePem;

    @SerializedName( "server_certificate_pem" )
    private String serverCertificatePem;

    @SerializedName( "proxy_host" )
    private String proxyHost;

    @SerializedName( "proxy_port" )
    private int proxyPort;

    @SerializedName( "proxy_user" )
    private String proxyUser;

    @SerializedName( "proxy_password" )
    private String proxyPassword;

    RemoteRepository()
    {
        super( StoreType.remote );
    }

    public RemoteRepository( final String name, final String remoteUrl )
    {
        super( StoreType.remote, name );
        this.url = remoteUrl;
        calculateFields();
    }

    RemoteRepository( final String name )
    {
        super( StoreType.remote, name );
    }

    public String getUrl()
    {
        calculateIfNeeded();
        return url;
    }

    public void setUrl( final String url )
    {
        this.url = url;
        calculateFields();
    }

    public String getUser()
    {
        calculateIfNeeded();
        return user;
    }

    private void calculateIfNeeded()
    {
        if ( host == null )
        {
            calculateFields();
        }
    }

    public void setUser( final String user )
    {
        this.user = user;
    }

    public String getPassword()
    {
        calculateIfNeeded();
        return password;
    }

    public void setPassword( final String password )
    {
        this.password = password;
    }

    public String getHost()
    {
        calculateIfNeeded();
        return host;
    }

    public void setHost( final String host )
    {
        this.host = host;
    }

    public int getPort()
    {
        calculateIfNeeded();
        return port;
    }

    public void setPort( final int port )
    {
        this.port = port;
    }

    public void calculateFields()
    {
        URL url = null;
        try
        {
            url = new URL( this.url );
        }
        catch ( final MalformedURLException e )
        {
            LOGGER.error( "Failed to parse repository URL: '{}'. Reason: {}", e, this.url, e.getMessage() );
        }

        if ( url == null )
        {
            return;
        }

        final String userInfo = url.getUserInfo();
        if ( userInfo != null && user == null && password == null )
        {
            user = userInfo;
            password = null;

            int idx = userInfo.indexOf( ':' );
            if ( idx > 0 )
            {
                user = userInfo.substring( 0, idx );
                password = userInfo.substring( idx + 1 );

                final StringBuilder sb = new StringBuilder();
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
            port = url.getProtocol()
                      .equals( "https" ) ? 443 : 80;
        }
        else
        {
            port = url.getPort();
        }
    }

    public int getTimeoutSeconds()
    {
        return timeoutSeconds < 0 ? DEFAULT_TIMEOUT_SECONDS : timeoutSeconds;
    }

    public void setTimeoutSeconds( final int timeoutSeconds )
    {
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String toString()
    {
        return String.format( "Repository [url=%s, timeoutSeconds=%s, host=%s, port=%s, user=%s, password=%s, getName()=%s, getKey()=%s]", url,
                              timeoutSeconds, host, port, user, password, getName(), getKey() );
    }

    public boolean isPassthrough()
    {
        return passthrough;
    }

    public void setPassthrough( final boolean passthrough )
    {
        this.passthrough = passthrough;
    }

    public int getCacheTimeoutSeconds()
    {
        return cacheTimeoutSeconds;
    }

    public void setCacheTimeoutSeconds( final int cacheTimeoutSeconds )
    {
        this.cacheTimeoutSeconds = cacheTimeoutSeconds;
    }

    public void setKeyPassword( final String keyPassword )
    {
        this.keyPassword = keyPassword;
    }

    public String getKeyPassword()
    {
        return keyPassword;
    }

    public void setKeyCertPem( final String keyCertificatePem )
    {
        this.keyCertificatePem = keyCertificatePem;
    }

    public String getKeyCertPem()
    {
        return keyCertificatePem;
    }

    public void setServerCertPem( final String serverCertificatePem )
    {
        this.serverCertificatePem = serverCertificatePem;
    }

    public String getServerCertPem()
    {
        return serverCertificatePem;
    }

    public String getProxyHost()
    {
        return proxyHost;
    }

    public int getProxyPort()
    {
        return proxyPort;
    }

    public String getProxyUser()
    {
        return proxyUser;
    }

    public String getProxyPassword()
    {
        return proxyPassword;
    }

    public void setProxyHost( final String proxyHost )
    {
        this.proxyHost = proxyHost;
    }

    public void setProxyPort( final int proxyPort )
    {
        this.proxyPort = proxyPort;
    }

    public void setProxyUser( final String proxyUser )
    {
        this.proxyUser = proxyUser;
    }

    public void setProxyPassword( final String proxyPassword )
    {
        this.proxyPassword = proxyPassword;
    }

}
