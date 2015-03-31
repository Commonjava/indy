package org.commonjava.aprox.core.bind.jaxrs.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.commonjava.maven.galley.model.Transfer;

public class TransferStreamingOutput
    implements StreamingOutput
{

    private final Transfer item;

    public TransferStreamingOutput( final Transfer item )
    {
        this.item = item;
    }

    @Override
    public void write( final OutputStream out )
        throws IOException, WebApplicationException
    {
        InputStream in = null;
        try
        {
            in = item.openInputStream();
            IOUtils.copy( in, out );
        }
        finally
        {
            IOUtils.closeQuietly( in );
        }
    }

}
