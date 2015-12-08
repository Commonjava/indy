/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.depgraph.impl;

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.depgraph.client.DepgraphIndyClientModule;
import org.commonjava.indy.depgraph.model.ArtifactRepoContent;
import org.commonjava.indy.depgraph.model.ProjectRepoContent;
import org.commonjava.indy.depgraph.model.RepoContentResult;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.ops.ResolveOps;
import org.commonjava.cartographer.request.RepositoryContentRequest;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.SimpleLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jdcasey on 8/17/15.
 */
public class ClientResolverOps
        implements ResolveOps
{
    private ClientCartographer carto;

    private final DepgraphIndyClientModule module;

    public ClientResolverOps( ClientCartographer carto, DepgraphIndyClientModule module )
    {
        this.carto = carto;
        this.module = module;
    }

    @Override
    public Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> resolveRepositoryContents(
            RepositoryContentRequest request )
            throws CartoDataException, CartoRequestException
    {
        // TODO: Cache somewhere so we can rebuild quickly if re-requested.
        try
        {
            RepoContentResult contentResult = module.repositoryContent( carto.normalizeRequest( request ) );

            Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> result = new HashMap<>();
            for ( ProjectVersionRef ref : contentResult )
            {
                Map<ArtifactRef, ConcreteResource> resources = new HashMap<>();

                ProjectRepoContent projectContent = contentResult.getProject( ref );
                for ( ArtifactRepoContent artifactContent : projectContent )
                {
                    StoreKey sk = artifactContent.getRepoKey();
                    String path = artifactContent.getPath();

                    String url = contentResult.getRepoUrl( sk );
                    if ( url == null )
                    {
                        throw new CartoDataException(
                                "Failed to deserialize repository content after graph resolution. "
                                        + "Encountered Indy store key: %s which is missing from the key-to-url map!",
                                sk );
                    }

                    ConcreteResource res = new ConcreteResource( new SimpleLocation( url ), path );
                    resources.put( artifactContent.getArtifact(), res );
                }

                result.put( ref, resources );
            }

            return result;
        }
        catch ( IndyClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }
}
