package org.commonjava.aprox.boot;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;

public class BootFinder
{

    public static BootInterface find()
        throws AproxBootException
    {
        return find( Thread.currentThread()
                           .getContextClassLoader() );
    }

    public static BootInterface find( final ClassLoader classloader )
        throws AproxBootException
    {
        final InputStream stream =
            classloader.getResourceAsStream( "META-INF/services/" + BootInterface.class.getName() );
        if ( stream == null )
        {
            throw new AproxBootException( "No BootInterface implementations registered." );
        }

        List<String> lines;
        try
        {
            lines = IOUtils.readLines( stream );
        }
        catch ( final IOException e )
        {
            throw new AproxBootException( "Failed to read registration of BootInterface: " + e.getMessage(), e );
        }

        final String className = lines.get( 0 );
        try
        {
            final Class<?> cls = classloader.loadClass( className );
            return (BootInterface) cls.newInstance();
        }
        catch ( ClassNotFoundException | InstantiationException | IllegalAccessException e )
        {
            throw new AproxBootException( "Failed to initialize BootInterface: %s. Reason: %s", e, className,
                                          e.getMessage() );
        }
    }

}
