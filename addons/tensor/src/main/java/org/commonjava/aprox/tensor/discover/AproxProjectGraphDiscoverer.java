package org.commonjava.aprox.tensor.discover;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.apache.maven.graph.common.version.MultiVersionSpec;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.common.version.VersionUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.aprox.io.StorageItem;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.GroupContentManager;
import org.commonjava.aprox.tensor.conf.AproxTensorConfig;
import org.commonjava.aprox.tensor.data.AproxTensorDataManager;
import org.commonjava.tensor.data.TensorDataException;
import org.commonjava.tensor.data.TensorDataManager;
import org.commonjava.tensor.discover.DiscoveryConfig;
import org.commonjava.tensor.discover.ProjectRelationshipDiscoverer;
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
    private TensorDataManager dataManager;

    @Inject
    private AproxTensorDataManager errorDataManager;

    @Inject
    private AproxTensorConfig config;

    @Override
    public ProjectVersionRef discoverRelationships( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig )
        throws TensorDataException
    {
        if ( errorDataManager.hasErrors( ref.getGroupId(), ref.getArtifactId(), ref.getVersionString() ) )
        {
            return ref;
        }

        ProjectVersionRef specific = ref;
        try
        {
            if ( !ref.isSpecificVersion() )
            {
                specific = resolveSpecificVersion( ref );
                if ( specific == null )
                {
                    logger.warn( "Cannot resolve specific version of: '%s'.", ref );
                    return null;
                }
            }
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            errorDataManager.addError( ref.getGroupId(), ref.getArtifactId(), ref.getVersionString(), e );
            specific = null;
        }

        if ( specific == null )
        {
            return ref;
        }

        InputStream stream = null;
        try
        {
            //FIXME: Verify the discovery group exists, or else use getAll() to check all locations.
            final String path = pomPath( specific );
            final StorageItem retrieved = groupContentManager.retrieve( config.getDiscoveryGroup(), path );
            if ( retrieved != null )
            {
                stream = retrieved.openInputStream();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            throw new TensorDataException( "Discovery of project-relationships for: '%s' failed. Error: %s", e, ref,
                                           e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new TensorDataException( "Discovery of project-relationships for: '%s' failed. Error: %s", e, ref,
                                           e.getMessage() );
        }
        finally
        {
            closeQuietly( stream );
        }

        return specific;
    }

    @Override
    public ProjectVersionRef resolveSpecificVersion( final ProjectVersionRef ref )
        throws TensorDataException
    {
        final List<SingleVersion> versions = getVersions( ref );

        Collections.sort( versions );
        Collections.reverse( versions );

        if ( !ref.isCompound() && ref.isSnapshot() )
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
            final MultiVersionSpec multi = (MultiVersionSpec) ref.getVersionSpec();

            if ( multi.isPinned() )
            {
                return new ProjectVersionRef( ref.getGroupId(), ref.getArtifactId(), multi.getPinnedVersion() );
            }

            final boolean snapshots = multi.isSnapshot();

            while ( !versions.isEmpty() )
            {
                final SingleVersion ver = versions.remove( 0 );
                if ( ( snapshots || ver.isRelease() ) && multi.contains( ver ) )
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
        final String version = projectId.getVersionString();

        return artifactIdPath( projectId ) + '/' + version + "/" + projectId.getArtifactId() + "-" + version + ".pom";
    }

    private String artifactIdPath( final ProjectVersionRef projectId )
    {
        return projectId.getGroupId()
                        .replace( '.', '/' ) + '/' + projectId.getArtifactId();
    }

}
