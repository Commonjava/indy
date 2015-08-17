package org.commonjava.aprox.depgraph.impl;

import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.depgraph.client.DepgraphAproxClientModule;
import org.commonjava.aprox.depgraph.model.ArtifactRepoContent;
import org.commonjava.aprox.depgraph.model.ProjectRepoContent;
import org.commonjava.aprox.depgraph.model.RepoContentResult;
import org.commonjava.aprox.model.core.StoreKey;
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

    private final DepgraphAproxClientModule module;

    public ClientResolverOps( ClientCartographer carto, DepgraphAproxClientModule module )
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
                                        + "Encountered AProx store key: %s which is missing from the key-to-url map!",
                                sk );
                    }

                    ConcreteResource res = new ConcreteResource( new SimpleLocation( url ), path );
                    resources.put( artifactContent.getArtifact(), res );
                }

                result.put( ref, resources );
            }

            return result;
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }
}
