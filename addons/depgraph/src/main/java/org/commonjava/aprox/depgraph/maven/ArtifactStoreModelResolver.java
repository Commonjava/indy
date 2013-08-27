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
package org.commonjava.aprox.depgraph.maven;

import java.util.List;

import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.codehaus.plexus.component.annotations.Component;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.galley.ArtifactManager;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.model.Transfer;

@Component( role = ModelResolver.class, hint = "aprox" )
public class ArtifactStoreModelResolver
    implements ModelResolver
{

    private final List<? extends KeyedLocation> locations;

    private final boolean fireEvents;

    private final ArtifactManager artifactManager;

    public ArtifactStoreModelResolver( final ArtifactManager artifactManager, final List<? extends KeyedLocation> locations, final boolean fireEvents )
    {
        this.artifactManager = artifactManager;
        this.locations = locations;
        this.fireEvents = fireEvents;
    }

    @Override
    public ModelSource resolveModel( final String groupId, final String artifactId, final String version )
        throws UnresolvableModelException
    {
        Transfer stream;
        try
        {
            stream = artifactManager.retrieveFirst( locations, new ProjectVersionRef( groupId, artifactId, version ).asPomArtifact() );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            throw new UnresolvableModelException( "Cannot resolve POM from available repositories: " + e.getMessage(), groupId, artifactId, version,
                                                  e );
        }
        catch ( final TransferException e )
        {
            throw new UnresolvableModelException( "Cannot resolve POM from available repositories: " + e.getMessage(), groupId, artifactId, version,
                                                  e );
        }

        if ( stream == null )
        {
            throw new UnresolvableModelException( "Cannot find POM in available repositories.", groupId, artifactId, version );
        }

        return new StoreModelSource( stream, fireEvents );
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
