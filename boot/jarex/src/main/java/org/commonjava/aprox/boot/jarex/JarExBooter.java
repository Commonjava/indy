package org.commonjava.aprox.boot.jarex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.boot.AproxBootException;
import org.commonjava.aprox.boot.BootFinder;
import org.commonjava.aprox.boot.BootInterface;
import org.commonjava.aprox.boot.BootOptions;
import org.commonjava.aprox.boot.BootStatus;

public class JarExBooter
    implements BootInterface
{

    private static final int ERR_CANT_READ_JAREX_CLASSPATH = 11;

    private static final int ERR_CANT_LOOKUP_BOOTER_SERVICE = 12;

    @SuppressWarnings( "resource" )
    public static void main( final String[] args )
    {
        final ClassLoader myCL = JarExBooter.class.getClassLoader();
        final List<URL> cpUrls = new ArrayList<>();

        BufferedReader reader = null;
        try
        {
            final InputStream stream = myCL.getResourceAsStream( "META-INF/jarex-classpath.lst" );
            if ( stream == null )
            {
                reader = new BufferedReader( new InputStreamReader( stream ) );
            }

            String line = null;
            while ( ( line = reader.readLine() ) != null )
            {
                final URL cpUrl = myCL.getResource( line.trim() );
                cpUrls.add( cpUrl );
            }
        }
        catch ( final IOException e )
        {
            System.err.printf( "Failed to read jarex boot classpath. Reason: %s", e.getMessage() );
            e.printStackTrace();
            System.exit( ERR_CANT_READ_JAREX_CLASSPATH );
            return;
        }
        finally
        {
            IOUtils.closeQuietly( reader );
        }

        System.out.println( "Constructed classpath: " + cpUrls );

        final URLClassLoader ucl = new URLClassLoader( cpUrls.toArray( new URL[cpUrls.size()] ) );
        Object booter;
        try
        {
            booter = ucl.loadClass( BootFinder.class.getName() )
                        .getMethod( "find" )
                        .invoke( null );
        }
        catch ( NoSuchMethodException | ClassNotFoundException | IllegalAccessException | InvocationTargetException e )
        {
            System.err.printf( "Failed to find BootInterface using service lookup. Reason: %s", e.getMessage() );
            e.printStackTrace();
            System.exit( ERR_CANT_LOOKUP_BOOTER_SERVICE );
            return;
        }

        BootOptions opts;
        try
        {
            opts = BootOptions.loadFromSysprops();
            opts.parseArgs( args );
        }
        catch ( final AproxBootException e )
        {
            System.err.printf( "ERROR: %s", e.getMessage() );
            System.exit( ERR_CANT_PARSE_ARGS );
            return;
        }

        try
        {
            new JarExBooter( booter, ucl ).runAndWait( opts );
        }
        catch ( final AproxBootException e )
        {
            System.err.printf( "ERROR INITIALIZING BOOTER: %s", e.getMessage() );
            System.exit( ERR_CANT_INIT_BOOTER );
        }
    }

    private final Object booter;

    private final URLClassLoader classloader;

    private Object bootOpts;

    private BootOptions realBootOptions;

    public JarExBooter( final Object booter, final URLClassLoader classloader )
    {
        this.booter = booter;
        this.classloader = classloader;
    }

    @Override
    public int runAndWait( final BootOptions bootOptions )
        throws AproxBootException
    {
        createBootOptions( bootOptions );

        try
        {
            final Method runAndWait = booter.getClass()
                                            .getMethod( "runAndWait", bootOpts.getClass() );

            return (Integer) runAndWait.invoke( booter, bootOpts );
        }
        catch ( IllegalAccessException | InvocationTargetException | NoSuchMethodException e )
        {
            throw new AproxBootException( "Cannot start: %s", e, e.getMessage() );
        }
    }

    private void createBootOptions( final BootOptions bootOptions )
        throws AproxBootException
    {
        this.realBootOptions = bootOptions;

        Map<String, String> properties;
        try
        {
            properties = BeanUtils.describe( bootOptions );

            final Class<?> bootOptsCls = classloader.loadClass( BootOptions.class.getName() );
            bootOpts = bootOptsCls.newInstance();

            BeanUtils.populate( bootOpts, properties );
        }
        catch ( IllegalAccessException | InvocationTargetException | InstantiationException | ClassNotFoundException
                        | NoSuchMethodException e )
        {
            throw new AproxBootException( "Cannot clone BootOptions inside delegated classloader: %s", e,
                                          e.getMessage() );
        }
    }

    @Override
    public BootStatus start( final BootOptions bootOptions )
        throws AproxBootException
    {
        try
        {
            final Method start = booter.getClass()
                                       .getMethod( "start", bootOpts.getClass() );

            final Object status = start.invoke( booter, bootOpts );

            final BootStatus result = new BootStatus();
            BeanUtils.populate( result, BeanUtils.describe( status ) );

            return result;
        }
        catch ( IllegalAccessException | InvocationTargetException | NoSuchMethodException e )
        {
            throw new AproxBootException( "Cannot start: ", e, e.getMessage() );
        }
    }

    @Override
    public BootOptions getBootOptions()
    {
        return realBootOptions;
    }

    @Override
    public void stop()
    {
        try
        {
            final Method stop = booter.getClass()
                                      .getMethod( "stop" );
            stop.invoke( booter );
        }
        catch ( IllegalAccessException | InvocationTargetException | NoSuchMethodException e )
        {
            throw new IllegalStateException( "Cannot stop: " + e.getMessage(), e );
        }
    }

}
