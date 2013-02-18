package org.commonjava.aprox.dotmaven.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.dotmaven.data.StorageAdvice;
import org.commonjava.aprox.dotmaven.webctl.RequestInfo;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;

public class SettingsTemplate
{

    private static final String NAME_PATTERN = Pattern.quote( "${name}" );

    private static final String URL_PATTERN = Pattern.quote( "${url}" );

    private static final String RELEASES_PATTERN = Pattern.quote( "${releases}" );

    private static final String SNAPSHOTS_PATTERN = Pattern.quote( "${snapshots}" );

    private static final String DEPLOYABLE_TEMPLATE = "settings-deploy.xml";

    private static final String NON_DEPLOYABLE_TEMPLATE = "settings-no-deploy.xml";

    //    private final Logger logger = new Logger( getClass() );

    private final RequestInfo requestInfo;

    private final StoreKey key;

    private final StorageAdvice advice;

    private transient byte[] content;

    public SettingsTemplate( final StoreKey key, final StorageAdvice advice, final RequestInfo requestInfo )
    {
        this.key = key;
        this.advice = advice;
        this.requestInfo = requestInfo;
    }

    public synchronized byte[] getContent()
    {
        formatSettings();
        return content;
    }

    private void formatSettings()
    {
        if ( content != null )
        {
            return;
        }

        final String name = key.getName();
        final StoreType type = key.getType();

        final String template;
        if ( advice.isDeployable() )
        {
            //            logger.info( "Loading deployable template for: %s", name );
            template = load( DEPLOYABLE_TEMPLATE );
        }
        else
        {
            //            logger.info( "Loading non-deployable template for: %s", name );
            template = load( NON_DEPLOYABLE_TEMPLATE );
        }

        if ( template == null )
        {
            content = new byte[] {};
        }

        final StringBuilder url = new StringBuilder();
        url.append( requestInfo.getBaseUrl() );
        if ( url.charAt( url.length() - 1 ) != '/' )
        {
            url.append( '/' );
        }

        url.append( "api/1.0/" );
        url.append( type.singularEndpointName() )
           .append( '/' )
           .append( name );

        try
        {
            content = template.replaceAll( NAME_PATTERN, name )
                              .replaceAll( URL_PATTERN, url.toString() )
                              .replaceAll( RELEASES_PATTERN, Boolean.toString( advice.isReleasesAllowed() ) )
                              .replaceAll( SNAPSHOTS_PATTERN, Boolean.toString( advice.isSnapshotsAllowed() ) )
                              .getBytes( "UTF-8" );
        }
        catch ( final UnsupportedEncodingException e )
        {
            throw new IllegalStateException( "Cannot find encoding for UTF-8!", e );
        }
    }

    private String load( final String res )
    {
        final InputStream stream = Thread.currentThread()
                                         .getContextClassLoader()
                                         .getResourceAsStream( res );
        if ( stream == null )
        {
            return null;
        }

        try
        {
            return IOUtils.toString( stream );
        }
        catch ( final IOException e )
        {
            return null;
        }
    }

    public long getLength()
    {
        formatSettings();
        return content.length;
    }
}
