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
package org.commonjava.indy.client.core.helper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpClientConnection;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloseBlockingConnectionManager
    implements HttpClientConnectionManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final HttpClientConnectionManager connectionManager;

    public CloseBlockingConnectionManager( final HttpClientConnectionManager connectionManager )
    {
        this.connectionManager = connectionManager;
    }

    @Override
    public ConnectionRequest requestConnection( final HttpRoute route, final Object state )
    {
        return connectionManager.requestConnection( route, state );
    }

    @Override
    public void releaseConnection( final HttpClientConnection conn, final Object newState, final long validDuration, final TimeUnit timeUnit )
    {
        connectionManager.releaseConnection( conn, newState, validDuration, timeUnit );
    }

    @Override
    public void connect( final HttpClientConnection conn, final HttpRoute route, final int connectTimeout, final HttpContext context )
        throws IOException
    {
        connectionManager.connect( conn, route, connectTimeout, context );
    }

    @Override
    public void upgrade( final HttpClientConnection conn, final HttpRoute route, final HttpContext context )
        throws IOException
    {
        connectionManager.upgrade( conn, route, context );
    }

    @Override
    public void routeComplete( final HttpClientConnection conn, final HttpRoute route, final HttpContext context )
        throws IOException
    {
        connectionManager.routeComplete( conn, route, context );
    }

    @Override
    public void closeIdleConnections( final long idletime, final TimeUnit tunit )
    {
        connectionManager.closeIdleConnections( idletime, tunit );
    }

    @Override
    public void closeExpiredConnections()
    {
        connectionManager.closeExpiredConnections();
    }

    @Override
    public void shutdown()
    {
        logger.info( "BLOCKED connection-manager shutdown" );
    }

    public void reallyShutdown()
    {
        connectionManager.shutdown();
    }

}
