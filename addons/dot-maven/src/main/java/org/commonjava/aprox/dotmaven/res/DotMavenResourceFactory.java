package org.commonjava.aprox.dotmaven.res;

import static org.commonjava.aprox.dotmaven.util.NameUtils.makePath;
import static org.commonjava.aprox.dotmaven.util.NameUtils.trimLeadingSlash;
import io.milton.common.Path;
import io.milton.http.ResourceFactory;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.Resource;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.dotmaven.res.settings.SettingsFolderResource;
import org.commonjava.aprox.dotmaven.res.storage.AllStoresFolderResource;
import org.commonjava.aprox.dotmaven.res.storage.StoreFolderResource;
import org.commonjava.aprox.dotmaven.res.storage.StoreTypeFolderResource;
import org.commonjava.aprox.dotmaven.webctl.DotMavenServlet;
import org.commonjava.util.logging.Logger;

@RequestScoped
public class DotMavenResourceFactory
    implements ResourceFactory
{

    public static final String SETTINGS_BASE = "/settings";

    public static final String STORES_BASE = "/stores";

    private final Logger logger = new Logger( getClass() );

    @Inject
    @Named( "settings-folder" )
    private SettingsFolderResource settingsFolderResource;

    @Inject
    @Named( "stores-folder" )
    private AllStoresFolderResource storesFolderResource;

    @Inject
    @Named( "everything-folder" )
    private EverythingResource everythingResource;

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
            resource = everythingResource;
        }
        else if ( SETTINGS_BASE.equals( realPath ) )
        {
            resource = settingsFolderResource;
        }
        else if ( realPath.startsWith( SETTINGS_BASE ) )
        {
            final String name = trimLeadingSlash( realPath );
            final String[] parts = name.split( "/" );
            final String settingsPath = makePath( parts, 1 );

            logger.info( "Loading settings resource: %s", settingsPath );

            resource = settingsFolderResource.createSettingsResource( settingsPath );
        }
        else if ( STORES_BASE.equals( realPath ) )
        {
            resource = storesFolderResource;
        }
        else if ( realPath.startsWith( STORES_BASE ) )
        {
            final String name = trimLeadingSlash( realPath );
            final String[] parts = name.split( "/" );

            final StoreTypeFolderResource typeRes = storesFolderResource.getChild( parts[1] );
            if ( parts.length > 2 )
            {
                final String storeName = parts[2];
                final StoreFolderResource storeRes = typeRes.getChild( storeName );
                if ( parts.length > 3 )
                {
                    final String storePath = makePath( parts, 3 );
                    resource = storeRes.child( storePath );
                }
                else
                {
                    resource = storeRes;
                }
            }
            else
            {
                resource = typeRes;
            }
        }

        return resource;
    }

}
