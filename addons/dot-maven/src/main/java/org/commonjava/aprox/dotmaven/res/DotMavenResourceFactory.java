package org.commonjava.aprox.dotmaven.res;

import io.milton.common.Path;
import io.milton.http.ResourceFactory;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.Resource;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.dotmaven.webctl.DotMavenServlet;
import org.commonjava.aprox.rest.util.GroupContentManager;
import org.commonjava.util.logging.Logger;

@RequestScoped
public class DotMavenResourceFactory
    implements ResourceFactory
{

    public static final String SETTINGS_PATTERN = "\\/settings-(deploy|group|repository)-.+\\.xml";

    private final Logger logger = new Logger( getClass() );

    @Inject
    private GroupContentManager groupContentManager;

    @Inject
    @Named( "settings-folder" )
    private Resource settingsFolderResource;

    @Inject
    private SettingsResourceHandler settingsResourceHandler;

    @Override
    public Resource getResource( final String host, final String path )
        throws NotAuthorizedException, BadRequestException
    {
        Path pathObj = Path.path( path );
        String first = pathObj.getFirst();
        while ( first != null && first.trim()
                                      .length() > 0 && !DotMavenServlet.NAME.equals( first ) )
        {
            pathObj = pathObj.getStripFirst();
            first = pathObj.getFirst();
        }

        if ( pathObj == null || pathObj.getFirst()
                                       .trim()
                                       .length() < 1 )
        {
            throw new BadRequestException( "Invalid WebDAV path: " + path );
        }

        pathObj = pathObj.getStripFirst();

        final String realPath = pathObj.toPath();

        logger.info( "WebDAV real path: '%s'", realPath );

        Resource resource = null;
        if ( realPath.trim()
                     .length() < 1 )
        {
            resource = settingsFolderResource;
        }
        else if ( realPath.matches( SETTINGS_PATTERN ) )
        {
            resource = settingsResourceHandler.createResource( host, realPath );
        }

        return resource;
    }

}
