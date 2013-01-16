package org.commonjava.aprox.tensor.discover;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.apache.maven.graph.common.version.RangeVersionSpec;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.common.version.VersionUtils;
import org.apache.maven.graph.effective.EProjectRelationships;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.aprox.io.StorageItem;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.GroupContentManager;
import org.commonjava.aprox.tensor.conf.AproxTensorConfig;
import org.commonjava.aprox.tensor.data.ProjectRelationshipsErrorEvent;
import org.commonjava.tensor.data.TensorDataException;
import org.commonjava.tensor.data.TensorDataManager;
import org.commonjava.tensor.discover.DiscoveryConfig;
import org.commonjava.tensor.discover.ProjectRelationshipDiscoverer;
import org.commonjava.tensor.event.NewRelationshipsEvent;
import org.commonjava.util.logging.Logger;

@javax.enterprise.context.ApplicationScoped
public class AproxProjectGraphDiscoverer
    implements ProjectRelationshipDiscoverer
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private GroupContentManager groupContentManager;

    @Inject
    private TensorDataManager dataManager;

    @Inject
    private AproxTensorConfig config;

    private final Map<String, RelationshipDiscoveryToken> dataHolders = new HashMap<String, RelationshipDiscoveryToken>();

    @Override
    public EProjectRelationships discoverRelationships( final ProjectVersionRef projectId,
                                                        final DiscoveryConfig discoveryConfig )
        throws TensorDataException
    {
        ProjectVersionRef specific = projectId;
        if ( !projectId.isSpecificVersion() )
        {
            specific = resolveSpecificVersion( projectId );
        }

        RelationshipDiscoveryToken holder = dataHolders.get( specific );
        if ( holder == null )
        {
            holder = new RelationshipDiscoveryToken( specific );
            dataHolders.put( specific.toString(), holder );

            try
            {
                //FIXME: Verify the discovery group exists, or else use getAll() to check all locations.
                groupContentManager.retrieve( config.getDiscoveryGroup(), pomPath( specific ) );
            }
            catch ( final AproxWorkflowException e )
            {
                throw new TensorDataException( "Discovery of project-relationships for: '%s' failed. Error: %s", e,
                                               projectId, e.getMessage() );
            }
        }

        synchronized ( holder )
        {
            try
            {
                holder.wait( config.getDiscoveryTimeoutMillis() );
            }
            catch ( final InterruptedException e )
            {
                logger.info( "Interrupted while waiting for discovery of: %s", specific );
                return null;
            }
        }

        final Throwable e = holder.getError();
        if ( e != null )
        {
            throw new TensorDataException( "Error discovering relationships for '%s': %s", e, specific, e.getMessage() );
        }

        return holder.getRelationships();
    }

    public void newRelationshipsNotifier( @Observes final NewRelationshipsEvent event )
    {
        final EProjectRelationships relationships = event.getRelationships();
        final ProjectVersionRef ref = relationships.getProjectRef();

        final RelationshipDiscoveryToken holder = dataHolders.remove( ref.toString() );
        if ( holder != null )
        {
            holder.setRelationships( relationships );
        }
    }

    public void relationshipsError( @Observes final ProjectRelationshipsErrorEvent event )
    {
        final RelationshipDiscoveryToken holder = dataHolders.remove( event.getKey()
                                                           .toString() );
        if ( holder != null )
        {
            holder.setError( event.getError() );
        }
    }

    @Override
    public ProjectVersionRef resolveSpecificVersion( final ProjectVersionRef ref )
        throws TensorDataException
    {
        final List<SingleVersion> versions = getVersions( ref );

        Collections.sort( versions );
        Collections.reverse( versions );

        if ( ref.isSnapshot() )
        {
            while ( !versions.isEmpty() )
            {
                final SingleVersion ver = versions.remove( 0 );
                if ( !ver.isRelease() )
                {
                    return new ProjectVersionRef( ref.getGroupId(), ref.getArtifactId(), ver );
                }
            }
        }
        else if ( ref.isCompound() )
        {
            final RangeVersionSpec range = (RangeVersionSpec) ref.getVersionSpec();

            final boolean snapshots =
                ( range.getLowerBound() != null && !range.getLowerBound()
                                                         .isRelease() )
                    || ( range.getUpperBound() != null && !range.getUpperBound()
                                                                .isRelease() );

            while ( !versions.isEmpty() )
            {
                final SingleVersion ver = versions.remove( 0 );
                if ( ( snapshots || ver.isRelease() ) && range.contains( ver ) )
                {
                    return new ProjectVersionRef( ref.getGroupId(), ref.getArtifactId(), ver );
                }
            }
        }

        return null;
    }

    private List<SingleVersion> getVersions( final ProjectVersionRef projectId )
        throws TensorDataException
    {
        final String metadataPath = versionMetadataPath( projectId );
        StorageItem item;
        try
        {
            //FIXME: Verify the discovery group exists, or else use getAll() to check all locations.
            item = groupContentManager.retrieve( config.getDiscoveryGroup(), metadataPath );
        }
        catch ( final AproxWorkflowException e )
        {
            throw new TensorDataException( "Failed to retrieve version metadata: %s from tensor group: %s. Reason: %s",
                                           e, metadataPath, config.getDiscoveryGroup(), e.getMessage() );
        }

        final List<SingleVersion> versions = new ArrayList<SingleVersion>();
        try
        {
            final Metadata metadata = new MetadataXpp3Reader().read( item.openInputStream() );
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
                        logger.error( "[SKIPPING] Invalid version: %s for project: %s. Reason: %s", spec, projectId,
                                      e.getMessage() );
                    }
                }
            }
        }
        catch ( final IOException e )
        {
            throw new TensorDataException( "Failed to read version metadata: %s. Reason: %s", e, item.getPath(),
                                           e.getMessage() );
        }
        catch ( final XmlPullParserException e )
        {
            throw new TensorDataException( "Failed to parse version metadata: %s. Reason: %s", e, item.getPath(),
                                           e.getMessage() );
        }

        return versions;
    }

    private String versionMetadataPath( final ProjectVersionRef projectId )
    {
        return artifactIdPath( projectId ) + "/maven-metadata.xml";
    }

    private String pomPath( final ProjectVersionRef projectId )
    {
        return artifactIdPath( projectId ) + '/' + projectId.getVersionSpec() + "/" + projectId.getArtifactId() + "-"
            + projectId.getVersionSpec()
                       .renderStandard() + ".pom";
    }

    private String artifactIdPath( final ProjectVersionRef projectId )
    {
        return projectId.getGroupId()
                        .replace( '.', '/' ) + '/' + projectId.getArtifactId();
    }

}
