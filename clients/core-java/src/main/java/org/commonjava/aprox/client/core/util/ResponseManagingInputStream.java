package org.commonjava.aprox.client.core.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.commonjava.aprox.client.core.AproxClientException;

public class ResponseManagingInputStream
    extends FilterInputStream
{

    private final CloseableHttpResponse response;

    public ResponseManagingInputStream( final CloseableHttpResponse response )
        throws AproxClientException
    {
        super( getStream( response ) );
        this.response = response;
    }

    private static InputStream getStream( final CloseableHttpResponse response )
        throws AproxClientException
    {
        try
        {
            return response.getEntity()
                           .getContent();
        }
        catch ( IllegalStateException | IOException e )
        {
            throw new AproxClientException( "Failed to retrieve InputStream embedded in HTTP response: %s", e,
                                            e.getMessage() );
        }
    }

    @Override
    public void close()
        throws IOException
    {
        super.close();
        IOUtils.closeQuietly( response );
    }

}
