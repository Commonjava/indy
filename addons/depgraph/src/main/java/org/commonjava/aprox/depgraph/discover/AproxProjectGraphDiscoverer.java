package org.commonjava.aprox.depgraph.discover;

import static org.commonjava.maven.cartographer.discover.DiscoveryUtils.selectSingle;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
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
import org.commonjava.maven.atlas.ident.version.MultiVersionSpec;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.maven.atlas.ident.version.VersionUtils;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.maven.galley.ArtifactManager;
import org.commonjava.maven.galley.ArtifactMetadataManager;
import org.commonjava.maven.galley.TransferException;
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
    private ArtifactMetadataManager artifactMetaManager;

    @Inject
    private CartoDataManager dataManager;

    @Inject
    private StoreDataManager storeManager;

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
                if ( specific.equals( ref ) )
                {
                    logger.warn( "Cannot resolve specific version of: '%s'.", ref );
                    return null;
                }
            }
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            dataManager.addError( new EProjectKey( source, ref ), e );
            specific = null;
        }

        if ( specific == null )
        {
            return null;
        }

        final StoreKey key = AproxDepgraphUtils.getDiscoveryStore( discoveryConfig.getDiscoverySource() );

        try
        {
            final ArtifactRef pomRef = specific.asPomArtifact();

            final Transfer retrieved;
            final List<? extends KeyedLocation> locations = getLocations( key );

            retrieved = artifactManager.retrieveFirst( locations, pomRef );

            if ( retrieved != null )
            {
                return discoverer.discoverRelationships( retrieved, locations, storeRelationships );
            }
            else
            {
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

        List<? extends KeyedLocation> locations = LocationUtils.toLocations( store );
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

        return locations;
    }

    @Override
    public ProjectVersionRef resolveSpecificVersion( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
        if ( ref.isRelease() )
        {
            return ref;
        }

        final List<SingleVersion> versions = getVersions( ref, discoveryConfig );

        Collections.sort( versions );
        Collections.reverse( versions );

        if ( ref.isCompound() )
        {
            final MultiVersionSpec multi = (MultiVersionSpec) ref.getVersionSpec();

            if ( multi.isPinned() )
            {
                if ( multi.getPinnedVersion()
                          .isConcrete() )
                {
                    selectSingle( multi.getPinnedVersion(), ref, dataManager );
                }

                return new ProjectVersionRef( ref.getGroupId(), ref.getArtifactId(), multi.getPinnedVersion() );
            }

            final boolean snapshots = multi.isSnapshot();

            while ( !versions.isEmpty() )
            {
                final SingleVersion ver = versions.remove( 0 );
                if ( ( snapshots || ver.isRelease() ) && multi.contains( ver ) )
                {
                    if ( ver.isConcrete() )
                    {
                        selectSingle( ver, ref, dataManager );
                    }
                    else
                    {
                        // FIXME: This is NASTY...
                    }

                    return new ProjectVersionRef( ref.getGroupId(), ref.getArtifactId(), ver );
                }
            }
        }
        else if ( ref.isSnapshot() )
        {
            while ( !versions.isEmpty() )
            {
                final SingleVersion ver = versions.remove( 0 );
                if ( !ver.isRelease() && !ver.isLocalSnapshot() )
                {
                    // FIXME: This is NASTY...
                    return new ProjectVersionRef( ref.getGroupId(), ref.getArtifactId(), ver );
                }
            }
        }

        return ref;
    }

    private List<SingleVersion> getVersions( final ProjectVersionRef projectId, final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
        final StoreKey key = AproxDepgraphUtils.getDiscoveryStore( discoveryConfig.getDiscoverySource() );

        Transfer item;
        try
        {
            final List<? extends KeyedLocation> locations = getLocations( key );
            item = artifactMetaManager.retrieveFirst( locations, projectId, "maven-metadata.xml" );
        }
        catch ( final TransferException e )
        {
            throw new CartoDataException( "Failed to retrieve version metadata for: %s from: %s. Reason: %s", e, projectId, key, e.getMessage() );
        }

        final List<SingleVersion> versions = new ArrayList<SingleVersion>();
        if ( item != null )
        {
            try
            {
                final Metadata metadata = new MetadataXpp3Reader().read( item.openInputStream( false ) );
                if ( metadata.getVersioning() != null && metadata.getVersioning()
                                                                 .getVersions() != null )
                {
                    for ( final String spec : metadata.getVersioning()
                                                      .getVersions() )
                    {
                        try
                        {
                            versions.add( VersionUtils.createSingleVersion( spec ) );
                        }
                        catch ( final InvalidVersionSpecificationException e )
                        {
                            logger.error( "[SKIPPING] Invalid version: %s for project: %s. Reason: %s", spec, projectId, e.getMessage() );
                        }
                    }
                }
            }
            catch ( final IOException e )
            {
                throw new CartoDataException( "Failed to read version metadata: %s. Reason: %s", e, item.getPath(), e.getMessage() );
            }
            catch ( final XmlPullParserException e )
            {
                throw new CartoDataException( "Failed to parse version metadata: %s. Reason: %s", e, item.getPath(), e.getMessage() );
            }
        }

        return versions;
    }

}
