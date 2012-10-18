package org.commonjava.aprox.dotmaven.res;

import static org.commonjava.aprox.model.StoreType.deploy_point;
import io.milton.http.exceptions.BadRequestException;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.dotmaven.DotMavenException;
import org.commonjava.aprox.dotmaven.webctl.RequestInfo;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.util.logging.Logger;

@RequestScoped
public class SettingsResourceHandler
{

    private static final String SETTINGS_PREFIX = "settings-";

    private static final String SETTINGS_SUFFIX = ".xml";

    private final Logger logger = new Logger( getClass() );

    @Inject
    private StoreDataManager aprox;

    @Inject
    private RequestInfo requestInfo;

    @SuppressWarnings( "incomplete-switch" )
    public SettingsResource createResource( final String host, final String storeName, final ArtifactStore store )
        throws ProxyDataException, DotMavenException
    {
        final StoreType type = store.getKey()
                                    .getType();
        boolean deployable = false;
        boolean releases = true;
        boolean snapshots = false;

        all: switch ( type )
        {
            case group:
            {
                final List<ArtifactStore> constituents = aprox.getOrderedConcreteStoresInGroup( store.getName() );
                for ( final ArtifactStore as : constituents )
                {
                    if ( as.getKey()
                           .getType() == deploy_point )
                    {
                        deployable = true;

                        final DeployPoint dp = (DeployPoint) as;

                        // TODO: If we have two deploy points with different settings, only the first will be used here!
                        snapshots = dp.isAllowSnapshots();
                        releases = dp.isAllowReleases();

                        logger.info( "\n\n\n\nDeploy point: %s allows releases? %s Releases boolean set to: %s\n\n\n\n",
                                     dp.getName(), dp.isAllowReleases(), releases );

                        break all;
                    }
                }
                break;
            }
            case deploy_point:
            {
                deployable = true;

                final DeployPoint dp = (DeployPoint) store;
                snapshots = dp.isAllowSnapshots();
                releases = dp.isAllowReleases();

                logger.info( "Deploy point: %s allows releases? %s", dp.getName(), dp.isAllowReleases() );
                break;
            }
        }

        return new SettingsResource( store.getKey()
                                          .getType(), store.getName(), deployable, releases, snapshots, requestInfo );
    }

    public SettingsResource createResource( final String host, final String storeName )
        throws BadRequestException
    {
        String raw = storeName;
        int idx = raw.indexOf( SETTINGS_PREFIX );
        if ( idx < 0 )
        {
            throw new BadRequestException( "Invalid settings name: " + storeName
                + "; should be of the form: 'settings-[d|r|g]-[name].xml'" );
        }

        raw = raw.substring( idx + SETTINGS_PREFIX.length() );

        idx = raw.indexOf( SETTINGS_SUFFIX );
        if ( idx < 0 )
        {
            throw new BadRequestException( "Invalid settings name: " + storeName
                + "; should be of the form: 'settings-[deploy|repository|group]-[name].xml'" );
        }

        raw = raw.substring( 0, raw.length() - SETTINGS_SUFFIX.length() );

        idx = raw.indexOf( '-' );
        if ( idx < 0 )
        {
            throw new BadRequestException( "Invalid settings name: " + storeName
                + "; should be of the form: 'settings-[deploy|repository|group]-[name].xml'" );
        }

        idx = raw.indexOf( '-' );
        if ( idx < 0 )
        {
            throw new BadRequestException( "Invalid settings name: " + storeName
                + "; should be of the form: 'settings-[deploy|repository|group]-[name].xml'" );
        }

        final String typePart = raw.substring( 0, idx );
        final String name = raw.substring( idx + 1 );

        logger.info( "Type part of name is: '%s'", typePart );
        logger.info( "Store part of name is: '%s'", name );

        final StoreType type = StoreType.get( typePart );
        logger.info( "StoreType is: %s", type );

        if ( type == null )
        {
            throw new BadRequestException( "Invalid settings name: " + storeName
                + "; should be of the form: 'settings-[deploy|repository|group]-[name].xml'" );
        }

        final StoreKey key = new StoreKey( type, name );
        try
        {
            final ArtifactStore store = aprox.getArtifactStore( key );
            return createResource( host, storeName, store );
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to retrieve artifact store for: %s. Reason: %s", e, key, e.getMessage() );
            throw new BadRequestException( "Cannot retrieve settings for: " + storeName );
        }
        catch ( final DotMavenException e )
        {
            logger.error( "Failed to format settings for: %s. Reason: %s", e, key, e.getMessage() );
            throw new BadRequestException( "Cannot retrieve settings for: " + storeName );
        }
    }

}
