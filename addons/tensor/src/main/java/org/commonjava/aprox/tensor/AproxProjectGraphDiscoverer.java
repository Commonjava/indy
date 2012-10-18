package org.commonjava.aprox.tensor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.apache.maven.graph.common.version.RangeVersionSpec;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.common.version.VersionUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.aprox.io.StorageItem;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.GroupContentManager;
import org.commonjava.aprox.tensor.conf.AproxTensorConfig;
import org.commonjava.tensor.data.TensorDataException;
import org.commonjava.tensor.discovery.ProjectRelationshipDiscoverer;
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
    private AproxTensorConfig config;

    private final Map<ProjectVersionRef, Object> notifiers = new HashMap<ProjectVersionRef, Object>();

    @Override
    public void discoverEffectiveProjectRelationships( final ProjectVersionRef projectId, final boolean wait )
        throws TensorDataException
    {
        ProjectVersionRef specific = projectId;
        if ( !projectId.isSpecificVersion() )
        {
            specific = resolveSpecificVersion( projectId );
        }

        try
        {
            groupContentManager.retrieve( config.getDiscoveryGroup(), pomPath( specific ) );
        }
        catch ( final AproxWorkflowException e )
        {
            throw new TensorDataException( "Discovery of project-relationships for: '%s' failed. Error: %s", e,
                                           projectId, e.getMessage() );
        }

        Object lock;
        synchronized ( this )
        {
            lock = notifiers.get( specific );
            if ( lock == null )
            {
                lock = new Object();
                notifiers.put( specific, lock );
            }
        }

        if ( wait )
        {
            final long timeout = TimeUnit.MILLISECONDS.convert( config.getDiscoveryTimeoutSeconds(), TimeUnit.SECONDS );
            synchronized ( lock )
            {
                try
                {
                    lock.wait( timeout );
                }
                catch ( final InterruptedException e )
                {
                    logger.warn( "Lock interrupted waiting for relationships of: %s", specific );
                }
            }
        }
    }

    public void newRelationshipsNotifier( @Observes final NewRelationshipsEvent event )
    {
        final Object lock = notifiers.get( event.getRelationships()
                                                .getProjectRef() );
        if ( lock != null )
        {
            synchronized ( lock )
            {
                lock.notifyAll();
            }
        }
    }

    private ProjectVersionRef resolveSpecificVersion( final ProjectVersionRef projectId )
        throws TensorDataException
    {
        final List<SingleVersion> versions = getVersions( projectId );

        Collections.sort( versions );
        Collections.reverse( versions );

        if ( projectId.isSnapshot() )
        {
            while ( !versions.isEmpty() )
            {
                final SingleVersion ver = versions.remove( 0 );
                if ( !ver.isRelease() )
                {
                    return new ProjectVersionRef( projectId.getGroupId(), projectId.getArtifactId(), ver );
                }
            }
        }
        else if ( projectId.isCompound() )
        {
            final RangeVersionSpec range = (RangeVersionSpec) projectId.getVersionSpec();

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
                    return new ProjectVersionRef( projectId.getGroupId(), projectId.getArtifactId(), ver );
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
