package org.commonjava.aprox.client.core.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.client.core.helper.HttpResources;

public class HttpResourcesManagingInputStream
    extends FilterInputStream
{

    private final HttpResources resources;

    public HttpResourcesManagingInputStream( final HttpResources httpResources )
        throws AproxClientException
    {
        super( getStream( httpResources ) );
        this.resources = httpResources;
    }

    private static InputStream getStream( final HttpResources resources )
        throws AproxClientException
    {
        try
        {
            return resources.getResponse()
                            .getEntity()
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
        IOUtils.closeQuietly( resources );
    }

}
