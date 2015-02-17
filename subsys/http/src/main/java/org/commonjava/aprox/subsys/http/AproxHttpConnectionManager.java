package org.commonjava.aprox.subsys.http;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;

import org.apache.http.HttpClientConnection;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AproxHttpConnectionManager
    extends PoolingHttpClientConnectionManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final boolean closeConnectionsOnRelease;

    protected AproxHttpConnectionManager()
    {
        closeConnectionsOnRelease = true;
    }

    public AproxHttpConnectionManager( final boolean closeConnectionsOnRelease )
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
