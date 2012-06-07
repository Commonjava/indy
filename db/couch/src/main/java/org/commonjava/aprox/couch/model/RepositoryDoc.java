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

import java.net.MalformedURLException;
import java.net.URL;

import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.couch.model.DenormalizedCouchDoc;
import org.commonjava.util.logging.Logger;

import com.google.gson.annotations.SerializedName;

public class RepositoryDoc
    extends AbstractArtifactStoreDoc
    implements DenormalizedCouchDoc, Repository
{
    private static final Logger LOGGER = new Logger( RepositoryDoc.class );

    private static final int DEFAULT_TIMEOUT_SECONDS = 120;

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

    RepositoryDoc()
    {
        super( StoreType.repository );
    }

    public RepositoryDoc( final String name, final String remoteUrl )
    {
        super( StoreType.repository, name );
        this.url = remoteUrl;
    }

    @Override
    public String getUrl()
    {
        return url;
    }

    @Override
    public void setUrl( final String url )
    {
        this.url = url;
    }

    @Override
    public String toString()
    {
        return String.format( "Repository [id='%s', rev='%s', name=%s, remoteUrl=%s, timeoutSeconds=%s]",
                              getCouchDocId(), getCouchDocRev(), getName(), url, timeoutSeconds );
    }

    @Override
    public String getUser()
    {
        return user;
    }

    @Override
    public void setUser( final String user )
    {
        this.user = user;
    }

    @Override
    public String getPassword()
    {
        return password;
    }

    @Override
    public void setPassword( final String password )
    {
        this.password = password;
    }

    @Override
    public String getHost()
    {
        return host;
    }

    public void setHost( final String host )
    {
        this.host = host;
    }

    @Override
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
        super.calculateDenormalizedFields();

        URL url = null;
        try
        {
            url = new URL( this.url );
        }
        catch ( final MalformedURLException e )
        {
            LOGGER.error( "Failed to parse repository URL: '%s'. Reason: %s", e, this.url, e.getMessage() );
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

    @Override
    public int getTimeoutSeconds()
    {
        return timeoutSeconds < 0 ? DEFAULT_TIMEOUT_SECONDS : timeoutSeconds;
    }

    @Override
    public void setTimeoutSeconds( final int timeoutSeconds )
    {
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public boolean isPassthrough()
    {
        return passthrough;
    }

    @Override
    public void setPassthrough( final boolean passthrough )
    {
        this.passthrough = passthrough;
    }

    @Override
    public int getCacheTimeoutSeconds()
    {
        return cacheTimeoutSeconds;
    }

    @Override
    public void setCacheTimeoutSeconds( final int cacheTimeoutSeconds )
    {
        this.cacheTimeoutSeconds = cacheTimeoutSeconds;
    }

    @Override
    public void setKeyPassword( final String keyPassword )
    {
        this.keyPassword = keyPassword;
    }

    @Override
    public String getKeyPassword()
    {
        return keyPassword;
    }

    @Override
    public void setKeyCertPem( final String keyCertificatePem )
    {
        this.keyCertificatePem = keyCertificatePem;
    }

    @Override
    public String getKeyCertPem()
    {
        return keyCertificatePem;
    }

    @Override
    public void setServerCertPem( final String serverCertificatePem )
    {
        this.serverCertificatePem = serverCertificatePem;
    }

    @Override
    public String getServerCertPem()
    {
        return serverCertificatePem;
    }

    @Override
    public String getProxyHost()
    {
        return proxyHost;
    }

    @Override
    public int getProxyPort()
    {
        return proxyPort;
    }

    @Override
    public String getProxyUser()
    {
        return proxyUser;
    }

    @Override
    public String getProxyPassword()
    {
        return proxyPassword;
    }

    @Override
    public void setProxyHost( final String proxyHost )
    {
        this.proxyHost = proxyHost;
    }

    @Override
    public void setProxyPort( final int proxyPort )
    {
        this.proxyPort = proxyPort;
    }

    @Override
    public void setProxyUser( final String proxyUser )
    {
        this.proxyUser = proxyUser;
    }

    @Override
    public void setProxyPassword( final String proxyPassword )
    {
        this.proxyPassword = proxyPassword;
    }

}
