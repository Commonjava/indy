package org.commonjava.aprox.depbase;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.rest.util.FileManager;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.layout.MavenDefaultLayout;
import org.sonatype.aether.util.layout.RepositoryLayout;

public class ArtifactStoreModelResolver
    implements ModelResolver
{

    private final RepositoryLayout layout = new MavenDefaultLayout();

    private final FileManager fileManager;

    private final List<ArtifactStore> stores;

    public ArtifactStoreModelResolver( final FileManager fileManager,
                                       final List<ArtifactStore> stores )
    {
        this.fileManager = fileManager;
        this.stores = stores;
    }

    @Override
    public ModelSource resolveModel( final String groupId, final String artifactId,
                                     final String version )
        throws UnresolvableModelException
    {
        Artifact a = new DefaultArtifact( groupId, artifactId, "pom", version );
        URI path = layout.getPath( a );

        File file = fileManager.downloadFirst( stores, path.getPath() );
        if ( file == null )
        {
            throw new UnresolvableModelException( "Cannot find POM in available repositories.",
                                                  groupId, artifactId, version );
        }

        return new FileModelSource( file );
    }

    @Override
    public void addRepository( final Repository repository )
        throws InvalidRepositoryException
    {}

    @Override
    public ModelResolver newCopy()
    {
        return this;
    }

}
