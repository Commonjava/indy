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

public enum ApplicationHeader
{

    content_type( "Content-Type" ),
    location( "Location" ),
    uri( "URI" ),
    content_length( "Content-Length" ),
    last_modified( "Last-Modified" ),
    deprecated( "Deprecated-Use-Alt" ),
    accept( "Accept" ),
    allow( "Allow" ),
    authorization( "Authorization" ),
    proxy_authenticate( "Proxy-Authenticate" ),
    proxy_authorization( "Proxy-Authorization" ),
    cache_control( "Cache-Control" ),
    content_disposition( "Content-Disposition" ),
    indy_origin( "Indy-Origin" ),
    transfer_encoding( "Transfer-Encoding" );

    private final String key;

    ApplicationHeader( final String key )
    {
        this.key = key;
    }

    public String key()
    {
        return key;
    }

    public String upperKey()
    {
        return key.toUpperCase();
    }

}
