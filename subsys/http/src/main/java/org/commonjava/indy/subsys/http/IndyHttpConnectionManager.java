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
package org.commonjava.indy.subsys.http;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;

import org.apache.http.HttpClientConnection;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IndyHttpConnectionManager
    extends PoolingHttpClientConnectionManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final boolean closeConnectionsOnRelease;

    protected IndyHttpConnectionManager()
    {
        closeConnectionsOnRelease = true;
    }

    public IndyHttpConnectionManager( final boolean closeConnectionsOnRelease )
    {
        this.closeConnectionsOnRelease = closeConnectionsOnRelease;
    }

    @Override
    public void releaseConnection( final HttpClientConnection conn, final Object state, final long keepalive,
                                   final TimeUnit tunit )
    {
        logger.info( "RELEASE: {}, keepalive: {}, tunit: {}", conn, keepalive, tunit );

        super.releaseConnection( conn, state, 0, TimeUnit.MILLISECONDS );
        if ( closeConnectionsOnRelease )
        {
            try
            {
                logger.info( "CLOSING: {}", conn );
                conn.close();
            }
            catch ( final IOException e )
            {
                logger.debug( "I/O error closing connection", e );
            }
        }
    }

}
