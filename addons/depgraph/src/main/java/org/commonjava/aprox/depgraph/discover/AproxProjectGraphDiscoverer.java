package org.commonjava.aprox.depgraph.discover;

import static org.apache.commons.lang.StringUtils.join;

import java.net.URI;
import java.util.List;

import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.depgraph.util.AproxDepgraphUtils;
import org.commonjava.aprox.inject.Production;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.atlas.graph.model.EProjectKey;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.maven.ArtifactManager;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.util.logging.Logger;

@javax.enterprise.context.ApplicationScoped
@Production
@Default
public class AproxProjectGraphDiscoverer
    implements ProjectRelationshipDiscoverer
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private AproxModelDiscoverer discoverer;

    @Inject
    private ArtifactManager artifactManager;

    @Inject
    private CartoDataManager dataManager;

    @Inject
    private StoreDataManager storeManager;

    protected AproxProjectGraphDiscoverer()
    {
    }

    public AproxProjectGraphDiscoverer( final AproxModelDiscoverer discoverer, final ArtifactManager artifactManager,
                                        final CartoDataManager dataManager, final StoreDataManager storeManager )
    {
        this.discoverer = discoverer;
        this.artifactManager = artifactManager;
        this.dataManager = dataManager;
        this.storeManager = storeManager;
    }

    @Override
    public DiscoveryResult discoverRelationships( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig, final boolean storeRelationships )
        throws CartoDataException
    {
        final URI source = discoveryConfig.getDiscoverySource();

        ProjectVersionRef specific = ref;
        try
        {
            if ( !ref.isSpecificVersion() )
            {
                specific = resolveSpecificVersion( ref, discoveryConfig );
                if ( specific == null || specific.equals( ref ) )
                {
                    logger.warn( "Cannot resolve specific version of: '%s'.", ref );
                    return null;
                }
            }
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Invalid version for: %s. Reason: %s", e, ref, e.getMessage() );
            dataManager.addError( new EProjectKey( source, ref ), e );
            specific = null;
        }

        if ( specific == null )
        {
            logger.info( "Specific version NOT resolved. Skipping discovery: %s", ref );
            return null;
        }

        final StoreKey key = AproxDepgraphUtils.getDiscoveryStore( discoveryConfig.getDiscoverySource() );

        try
        {
            final ArtifactRef pomRef = specific.asPomArtifact();

            final Transfer retrieved;
            final List<? extends KeyedLocation> locations = getLocations( key );
            if ( locations == null || locations.isEmpty() )
            {
                logger.info( "NO LOCATIONS given for resolving: %s", pomRef );
                return null;
            }

            retrieved = artifactManager.retrieveFirst( locations, pomRef );

            if ( retrieved != null )
            {
                return discoverer.discoverRelationships( retrieved, locations, discoveryConfig.getEnabledPatchers(), storeRelationships );
            }
            else
            {
                logger.info( "%s NOT FOUND in:\n  %s", pomRef, join( locations, "\n  " ) );
                return null;
            }
        }
        catch ( final TransferException e )
        {
            throw new CartoDataException( "Discovery of project-relationships for: '%s' failed. Error: %s", e, ref, e.getMessage() );
        }
    }

    private List<? extends KeyedLocation> getLocations( final StoreKey key )
        throws CartoDataException
    {
        ArtifactStore store;
        try
        {
            store = storeManager.getArtifactStore( key );
        }
        catch ( final ProxyDataException e )
        {
            throw new CartoDataException( "Failed to lookup ArtifactStore for key: %s. Reason: %s", e, key, e.getMessage() );
        }

        List<? extends KeyedLocation> locations;
        if ( key == null )
        {
            locations = LocationUtils.toLocations( storeManager.getAllConcreteArtifactStores() );
        }
        else if ( store == null )
        {
            throw new CartoDataException( "Cannot discover %s from: %s. No such store.", key );
        }
        else if ( key.getType() == StoreType.group )
        {
            List<ArtifactStore> concrete;
            try
            {
                concrete = storeManager.getOrderedConcreteStoresInGroup( key.getName() );
            }
            catch ( final ProxyDataException e )
            {
                throw new CartoDataException( "Failed to lookup ordered list of concrete ArtifactStores for group: %s. Reason: %s", e, key,
                                              e.getMessage() );
            }

            locations = LocationUtils.toLocations( concrete );
        }
        else
        {
            locations = LocationUtils.toLocations( store );
        }

        return locations;
    }

    @Override
    public ProjectVersionRef resolveSpecificVersion( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
        final StoreKey key = AproxDepgraphUtils.getDiscoveryStore( discoveryConfig.getDiscoverySource() );
        final List<? extends KeyedLocation> locations = getLocations( key );

        try
        {
            return artifactManager.resolveVariableVersion( locations, ref );
        }
        catch ( final TransferException e )
        {
            throw new CartoDataException( "Failed to resolve variable version for: %s. Reason: %s", e, ref, e.getMessage() );
        }
    }

}
