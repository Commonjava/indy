/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.depbase.maven;

import java.net.URI;
import java.util.List;

import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.codehaus.plexus.component.annotations.Component;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.rest.RESTWorkflowException;
import org.commonjava.aprox.core.rest.StoreInputStream;
import org.commonjava.aprox.core.rest.util.PathRetriever;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.layout.MavenDefaultLayout;
import org.sonatype.aether.util.layout.RepositoryLayout;

@Component( role = ModelResolver.class, hint = "aprox" )
public class ArtifactStoreModelResolver
    implements ModelResolver
{

    private final RepositoryLayout layout = new MavenDefaultLayout();

    private final PathRetriever fileManager;

    private final List<ArtifactStore> stores;

    public ArtifactStoreModelResolver( final PathRetriever fileManager, final List<ArtifactStore> stores )
    {
        this.fileManager = fileManager;
        this.stores = stores;
    }

    @Override
    public ModelSource resolveModel( final String groupId, final String artifactId, final String version )
        throws UnresolvableModelException
    {
        final Artifact a = new DefaultArtifact( groupId, artifactId, "pom", version );
        final URI path = layout.getPath( a );

        StoreInputStream stream;
        try
        {
            stream = fileManager.retrieveFirst( stores, path.getPath() );
        }
        catch ( final RESTWorkflowException e )
        {
            throw new UnresolvableModelException( "Cannot resolve POM from available repositories: " + e.getMessage(),
                                                  groupId, artifactId, version, e );
        }

        if ( stream == null )
        {
            throw new UnresolvableModelException( "Cannot find POM in available repositories.", groupId, artifactId,
                                                  version );
        }

        return new StoreModelSource( stream );
    }

    @Override
    public void addRepository( final Repository repository )
        throws InvalidRepositoryException
    {
    }

    @Override
    public ModelResolver newCopy()
    {
        return this;
    }

}
