package org.commonjava.aprox.dotmaven.res;

import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.GetableResource;
import io.milton.resource.PropFindableResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.dotmaven.webctl.RequestInfo;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.util.logging.Logger;

public class SettingsResource
    implements GetableResource, PropFindableResource
{

    private final Logger logger = new Logger( getClass() );

    private static final String NAME_PATTERN = Pattern.quote( "${name}" );

    private static final String URL_PATTERN = Pattern.quote( "${url}" );

    private static final String RELEASES_PATTERN = Pattern.quote( "${snapshots}" );

    private static final String SNAPSHOTS_PATTERN = Pattern.quote( "${releases}" );

    private static final String DEPLOYABLE_TEMPLATE = "settings-deploy.xml";

    private static final String NON_DEPLOYABLE_TEMPLATE = "settings-no-deploy.xml";

    private final String name;

    private final StoreType type;

    private final boolean deployable;

    private final boolean releases;

    private final boolean snapshots;

    private byte[] content;

    private final RequestInfo requestInfo;

    public SettingsResource( final StoreType type, final String name, final boolean deployable, final boolean releases,
                             final boolean snapshots, final RequestInfo requestInfo )
    {
        this.type = type;
        this.name = name;
        this.deployable = deployable;
        this.releases = releases;
        this.snapshots = snapshots;
        this.requestInfo = requestInfo;
    }

    @Override
    public String getUniqueId()
    {
        return getName();
    }

    @Override
    public String getName()
    {
        return "settings-" + type.singularEndpointName() + "-" + name + ".xml";
    }

    @Override
    public Object authenticate( final String user, final String password )
    {
        // FIXME: Integrate with BADGR
        return "ok";
    }

    @Override
    public boolean authorise( final Request request, final Method method, final Auth auth )
    {
        // FIXME: Integrate with BADGR
        return true;
    }

    @Override
    public String getRealm()
    {
        return requestInfo.getRealm();
    }

    @Override
    public Date getModifiedDate()
    {
        return new Date();
    }

    @Override
    public String checkRedirect( final Request request )
        throws NotAuthorizedException, BadRequestException
    {
        return null;
    }

    @Override
    public Date getCreateDate()
    {
        return new Date();
    }

    @Override
    public void sendContent( final OutputStream out, final Range range, final Map<String, String> params,
                             final String contentType )
        throws IOException, NotAuthorizedException, BadRequestException, NotFoundException
    {
        formatSettings();
        if ( content.length < 1 )
        {
            throw new NotFoundException( "Cannot find settings template" );
        }

        logger.info( "Sending settings content:\n\n%s", new String( content ) );

        IOUtils.copy( new ByteArrayInputStream( content ), out );
    }

    private void formatSettings()
    {
        if ( content != null )
        {
            return;
        }

        final String template;
        if ( deployable )
        {
            logger.info( "Loading deployable template for: %s", name );
            template = load( DEPLOYABLE_TEMPLATE );
        }
        else
        {
            logger.info( "Loading non-deployable template for: %s", name );
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
                              .replaceAll( RELEASES_PATTERN, Boolean.toString( releases ) )
                              .replaceAll( SNAPSHOTS_PATTERN, Boolean.toString( snapshots ) )
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

    @Override
    public Long getMaxAgeSeconds( final Auth auth )
    {
        return 1L;
    }

    @Override
    public String getContentType( final String accepts )
    {
        return "text/xml";
    }

    @Override
    public Long getContentLength()
    {
        formatSettings();
        return (long) content.length;
    }

}
