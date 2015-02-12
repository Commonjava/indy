package org.commonjava.aprox.client.core.util;

import java.io.FilterInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.client.core.helper.HttpResources;

public class HttpResourcesManagingInputStream
    extends FilterInputStream
{

    private final HttpResources resources;

    public HttpResourcesManagingInputStream( final HttpResources httpResources )
        throws IOException
    {
        super( httpResources.getResponseEntityContent() );
        this.resources = httpResources;
    }

    @Override
    public void close()
        throws IOException
    {
        super.close();
        IOUtils.closeQuietly( resources );
    }

}
