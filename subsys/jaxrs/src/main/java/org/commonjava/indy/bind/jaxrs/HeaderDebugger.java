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
package org.commonjava.indy.bind.jaxrs;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;

import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeaderDebugger
    implements HttpHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final HttpHandler handler;

    public HeaderDebugger( final HttpHandler handler )
    {
        this.handler = handler;
    }

    public static final class Wrapper
        implements HandlerWrapper
    {

        @Override
        public HttpHandler wrap( final HttpHandler handler )
        {
            return new HeaderDebugger( handler );
        }

    }

    @Override
    public void handleRequest( final HttpServerExchange exchange )
        throws Exception
    {
        if ( logger.isTraceEnabled() )
        {
            logger.trace( "{} {}", exchange.getRequestMethod(), exchange.getRequestPath() );
            logger.trace( "FROM: {}", exchange.getSourceAddress() );
            final HeaderMap headerMap = exchange.getRequestHeaders();
            final Collection<HttpString> names = headerMap.getHeaderNames();
            logger.trace( "HEADERS: " );
            for ( final HttpString name : names )
            {
                final HeaderValues values = headerMap.get( name );
                for ( final String value : values )
                {
                    logger.trace( "{}: {}", name, value );
                }
            }

            logger.trace( "COOKIES: " );
            final Map<String, Cookie> cookies = exchange.getRequestCookies();
            for ( final String key : cookies.keySet() )
            {
                final Cookie cookie = cookies.get( key );
                if ( cookie == null )
                {
                    continue;
                }

                logger.trace( "Cookie({}):\n  Domain: {}\n  Name: {}\n  Path: {}\n  Value: {}\n  Expires: {}\n  Max-Age: {}\n  Comment: {}\n  Version: {}\n\n",
                              key, cookie.getDomain(), cookie.getName(), cookie.getPath(), cookie.getValue(),
                              cookie.getExpires(), cookie.getMaxAge(), cookie.getComment(), cookie.getVersion() );
            }
        }

        handler.handleRequest( exchange );
    }

}
