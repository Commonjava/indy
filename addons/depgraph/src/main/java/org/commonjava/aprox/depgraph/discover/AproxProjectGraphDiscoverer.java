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
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.io.ModelReader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.depgraph.maven.DepgraphModelCache;
import org.commonjava.aprox.depgraph.util.AproxDepgraphUtils;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.GroupContentManager;
import org.commonjava.maven.atlas.graph.model.EProjectKey;
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
import org.commonjava.maven.cartographer.util.MavenModelProcessor;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.util.logging.Logger;

@javax.enterprise.context.ApplicationScoped
//@Production
@Default
public class AproxProjectGraphDiscoverer
    implements ProjectRelationshipDiscoverer
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private GroupContentManager groupContentManager;

    @Inject
    private AproxModelDiscoverer discoverer;

    @Inject
    private FileManager fileManager;

    @Inject
    private CartoDataManager dataManager;

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private ModelReader modelReader;

    @Inject
    private ModelBuilder modelBuilder;

    @Inject
    private MavenModelProcessor modelProcessor;

    @Inject
    private DepgraphModelCache tensorModelCache;

    //  public TensorStorageListenerRunnable( final StoreDataManager aprox, final ModelReader modelReader,
    //  final ModelBuilder modelBuilder, final FileManager fileManager,
    //  final MavenModelProcessor modelProcessor,
    //  final CartoDataManager dataManager, final TensorModelCache tensorModelCache,
    //  final Transfer item )

    @Override
    public DiscoveryResult discoverRelationships( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
        if ( dataManager.hasErrors( ref ) )
        {
            return null;
        }

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
            final ArtifactStore store = storeManager.getArtifactStore( key );

            final String path = pomPath( specific );

            final Transfer retrieved;
            if ( store == null )
            {
                throw new CartoDataException( "Cannot discover %s from: %s. No such store.", key );
            }
            else if ( key == null )
            {
                retrieved = fileManager.retrieveFirst( storeManager.getAllConcreteArtifactStores(), path );
            }
            else if ( key.getType() == StoreType.group )
            {
                final List<ArtifactStore> concrete = storeManager.getOrderedConcreteStoresInGroup( key.getName() );
                retrieved = fileManager.retrieveFirst( concrete, path );
            }
            else
            {
                retrieved = fileManager.retrieve( store, path );
            }

            if ( retrieved != null )
            {
                return discoverer.discoverRelationships( retrieved );
            }
            else
            {
                return null;
            }
        }
        catch ( final AproxWorkflowException e )
        {
            throw new CartoDataException( "Discovery of project-relationships for: '%s' failed. Error: %s", e, ref,
                                          e.getMessage() );
        }
        catch ( final ProxyDataException e )
        {
            throw new CartoDataException( "Discovery of project-relationships for: '%s' failed. Error: %s", e, ref,
                                          e.getMessage() );
        }
    }

    @Override
    public ProjectVersionRef resolveSpecificVersion( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
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

        final String metadataPath = versionMetadataPath( projectId );
        Transfer item;
        try
        {
            if ( key == null )
            {
                item = fileManager.retrieveFirst( storeManager.getAllConcreteArtifactStores(), metadataPath );
            }
            else if ( !storeManager.hasArtifactStore( key ) )
            {
                throw new CartoDataException( "Cannot discover versions from: '%s'. No such store.", key );
            }
            else if ( key.getType() == StoreType.group )
            {
                item = groupContentManager.retrieve( key.getName(), metadataPath );
            }
            else
            {
                final ArtifactStore store = storeManager.getArtifactStore( key );
                item = fileManager.retrieve( store, metadataPath );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            throw new CartoDataException( "Failed to retrieve version metadata: %s from tensor group: %s. Reason: %s",
                                          e, metadataPath, key.getName(), e.getMessage() );
        }
        catch ( final ProxyDataException e )
        {
            throw new CartoDataException( "Failed to retrieve version metadata: %s from tensor group: %s. Reason: %s",
                                          e, metadataPath, key.getName(), e.getMessage() );
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
                            logger.error( "[SKIPPING] Invalid version: %s for project: %s. Reason: %s", spec,
                                          projectId, e.getMessage() );
                        }
                    }
                }
            }
            catch ( final IOException e )
            {
                throw new CartoDataException( "Failed to read version metadata: %s. Reason: %s", e, item.getPath(),
                                              e.getMessage() );
            }
            catch ( final XmlPullParserException e )
            {
                throw new CartoDataException( "Failed to parse version metadata: %s. Reason: %s", e, item.getPath(),
                                              e.getMessage() );
            }
        }

        return versions;
    }

    private String versionMetadataPath( final ProjectVersionRef projectId )
    {
        return artifactIdPath( projectId ) + "/maven-metadata.xml";
    }

    private String pomPath( final ProjectVersionRef projectId )
    {
        final String version = projectId.getVersionString();

        return artifactIdPath( projectId ) + '/' + version + "/" + projectId.getArtifactId() + "-" + version + ".pom";
    }

    private String artifactIdPath( final ProjectVersionRef projectId )
    {
        return projectId.getGroupId()
                        .replace( '.', '/' ) + '/' + projectId.getArtifactId();
    }

}
