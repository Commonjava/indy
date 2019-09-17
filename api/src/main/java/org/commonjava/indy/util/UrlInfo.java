/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.util;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

public final class UrlInfo
{
    private final String url;

    private final String protocol;

    private String user;

    private String password;

    private final String host;

    private int port;

    private final String rawUrl;

    private final String fileWithNoLastSlash;

    private final String urlWithNoSchemeAndLastSlash;

    public UrlInfo( final String u )
    {
        this.rawUrl = u;
        String resultUrl = u;

        URL url;
        try
        {
            url = new URL( u );
        }
        catch ( final MalformedURLException e )
        {
            throw new IllegalArgumentException( "Failed to parse repository URL: '" + u + "'. Reason: "
                + e.getMessage(), e );
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
                idx = this.rawUrl.indexOf( "://" );
                sb.append( this.rawUrl.substring( 0, idx + 3 ) );

                idx = this.rawUrl.indexOf( "@" );
                if ( idx > 0 )
                {
                    sb.append( this.rawUrl.substring( idx + 1 ) );
                }

                resultUrl = sb.toString();
            }
        }

        this.url = resultUrl;

        this.protocol = url.getProtocol();

        host = url.getHost();
        if ( url.getPort() < 0 )
        {
            port = url.getDefaultPort();
        }
        else
        {
            port = url.getPort();
        }

        if ( url.getFile().endsWith( "/" ) )
        {
            final StringBuilder fileWithSlash = new StringBuilder( url.getFile() );
            int index = fileWithSlash.lastIndexOf( "/" );
            fileWithNoLastSlash = fileWithSlash.deleteCharAt( index ).toString();

            final StringBuilder urlWithSlash = new StringBuilder( host + ':' + port + url.getFile() );
            index = urlWithSlash.lastIndexOf( "/" );
            urlWithNoSchemeAndLastSlash = urlWithSlash.deleteCharAt( index ).toString();
        }
        else
        {
            fileWithNoLastSlash = url.getFile();
            urlWithNoSchemeAndLastSlash = host + ':' + port + url.getFile();
        }
    }

    public String getRawUrl()
    {
        return rawUrl;
    }

    public String getUrl()
    {
        return url;
    }

    public String getProtocol()
    {
        return protocol;
    }

    public String getUser()
    {
        return user;
    }

    public String getPassword()
    {
        return password;
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public String getUrlWithNoSchemeAndLastSlash(){
        return urlWithNoSchemeAndLastSlash;
    }

    public String getFileWithNoLastSlash()
    {
        return fileWithNoLastSlash;
    }

    public String getIpForUrl() throws UnknownHostException{
        InetAddress address = InetAddress.getByName( getHost() );

        String ip = null;
        if ( address != null )
        {
            ip = address.getHostAddress();
        }
        return ip;
    }



}
