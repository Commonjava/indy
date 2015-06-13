package org.commonjava.aprox.util;

import java.io.IOException;
import java.io.InputStream;

import javax.activation.MimetypesFileTypeMap;
import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MimeTyper
{

    private static final String EXTRA_MIME_TYPES = "extra-mime.types";

    private final MimetypesFileTypeMap typeMap = new MimetypesFileTypeMap();

    public MimeTyper()
    {
        final InputStream stream = Thread.currentThread()
                                         .getContextClassLoader()
                                         .getResourceAsStream( EXTRA_MIME_TYPES );
        if ( stream != null )
        {
            try
            {
                final String extraTypes = IOUtils.toString( stream );
                typeMap.addMimeTypes( extraTypes );
            }
            catch ( final IOException e )
            {
                LoggerFactory.getLogger( getClass() )
                             .error( "Cannot read extra mime types from classpath: " + EXTRA_MIME_TYPES, e );
            }
        }
    }

    public String getContentType( final String path )
    {
        return new MimetypesFileTypeMap().getContentType( path );
    }

}
