package org.commonjava.aprox.depgraph.client;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.client.core.AproxResponseErrorDetails;
import org.commonjava.aprox.client.core.helper.HttpResources;
import org.commonjava.aprox.client.core.util.UrlUtils;
import org.commonjava.aprox.depgraph.model.DownlogRequest;
import org.commonjava.aprox.depgraph.model.RepoContentResult;
import org.commonjava.aprox.depgraph.model.UrlMapResult;
import org.commonjava.aprox.depgraph.model.WorkspaceList;
import org.commonjava.aprox.depgraph.model.builder.DownlogRequestBuilder;
import org.commonjava.cartographer.request.*;
import org.commonjava.cartographer.request.build.*;
import org.commonjava.cartographer.result.*;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Created by jdcasey on 8/12/15.
 */
public class DepgraphAproxClientModule
        extends AproxClientModule
{
    public ProjectListResult list( ProjectGraphRequest request )
            throws AproxClientException
    {
        return getHttp().postWithResponse( "depgraph/project/list", request, ProjectListResult.class );
    }

    public MappedProjectResult parents( ProjectGraphRequest request )
            throws AproxClientException
    {
        return getHttp().postWithResponse( "depgraph/project/parents", request, MappedProjectResult.class );
    }

    public ProjectErrors errors( ProjectGraphRequest request )
            throws AproxClientException
    {
        return getHttp().postWithResponse( "depgraph/graph/errors", request, ProjectErrors.class );
    }

    public ProjectListResult reindex( ProjectGraphRequest request )
            throws AproxClientException
    {
        return getHttp().postWithResponse( "depgraph/graph/reindex", request, ProjectListResult.class );
    }

    public ProjectListResult incomplete( ProjectGraphRequest request )
            throws AproxClientException
    {
        return getHttp().postWithResponse( "depgraph/graph/incomplete", request, ProjectListResult.class );
    }

    public ProjectListResult missing( ProjectGraphRequest request )
            throws AproxClientException
    {
        return incomplete( request );
    }

    public ProjectListResult variable( ProjectGraphRequest request )
            throws AproxClientException
    {
        return getHttp().postWithResponse( "depgraph/graph/variable", request, ProjectListResult.class );
    }

    public ProjectPathsResult paths( PathsRequest request )
            throws AproxClientException
    {
        return getHttp().postWithResponse( "depgraph/graph/paths", request, ProjectPathsResult.class );
    }

    public MappedProjectsResult ancestors( ProjectGraphRequest request )
            throws AproxClientException
    {
        return getHttp().postWithResponse( "depgraph/graph/ancestry", request, MappedProjectsResult.class );
    }

    public BuildOrder buildOrder( ProjectGraphRequest request )
            throws AproxClientException
    {
        return getHttp().postWithResponse( "depgraph/graph/build-order", request, BuildOrder.class );
    }

    public GraphExport graph( SingleGraphRequest request )
            throws AproxClientException
    {
        return getHttp().postWithResponse( "depgraph/graph/export", request, GraphExport.class );
    }

    public GraphExport export( SingleGraphRequest request )
            throws AproxClientException
    {
        return graph( request );
    }

    public MappedProjectRelationshipsResult relationshipsDeclaredBy( ProjectGraphRelationshipsRequest request )
            throws AproxClientException
    {
        return getHttp().postWithResponse( "depgraph/project/relationships", request,
                                           MappedProjectRelationshipsResult.class );
    }

    public MappedProjectRelationshipsResult relationshipsTargeting( ProjectGraphRelationshipsRequest request )
            throws AproxClientException
    {
        return getHttp().postWithResponse( "depgraph/project/targeting", request,
                                           MappedProjectRelationshipsResult.class );
    }

    public MetadataResult getMetadata( MetadataExtractionRequest request )
            throws AproxClientException
    {
        return getHttp().postWithResponse( "depgraph/meta", request, MetadataResult.class );
    }

    public ProjectListResult updateMetadata( MetadataUpdateRequest request )
            throws AproxClientException
    {
        return getHttp().postWithResponse( "depgraph/meta/updates", request, ProjectListResult.class );
    }

    public ProjectListResult rescanMetadata( ProjectGraphRequest request )
            throws AproxClientException
    {
        return getHttp().postWithResponse( "depgraph/meta/rescan", request, ProjectListResult.class );
    }

    public MetadataCollationResult collateMetadata( MetadataCollationRequest request )
            throws AproxClientException
    {
        return getHttp().postWithResponse( "depgraph/meta/collation", request, MetadataCollationResult.class );
    }

    public void deleteWorkspace( String workspaceId )
            throws AproxClientException
    {
        getHttp().delete( UrlUtils.buildUrl( "depgraph/ws", workspaceId ) );
    }

    public WorkspaceList listWorkspaces()
            throws AproxClientException
    {
        return getHttp().get( "depgraph/ws", WorkspaceList.class );
    }

    public GraphDifference<ProjectVersionRef> calculateGraphDrift( GraphAnalysisRequest request )
            throws AproxClientException
    {
        return getHttp().postWithResponse( "depgraph/calc/drift", request,
                                           new TypeReference<GraphDifference<ProjectVersionRef>>()
                                           {
                                           } );
    }

    public GraphDifference<ProjectRelationship<?>> graphDiff( GraphAnalysisRequest request )
            throws AproxClientException
    {
        return getHttp().postWithResponse( "depgraph/calc/drift", request,
                                           new TypeReference<GraphDifference<ProjectRelationship<?>>>()
                                           {
                                           } );
    }

    public GraphCalculation calculate( MultiGraphRequest request )
            throws AproxClientException
    {
        return getHttp().postWithResponse( "depgraph/calc/drift", request, GraphCalculation.class );
    }

    public UrlMapResult repositoryUrlMap( RepositoryContentRequest request )
            throws AproxClientException
    {
        return getHttp().postWithResponse( "depgraph/repo/urlmap", request, UrlMapResult.class );
    }

    public RepoContentResult repositoryContent( RepositoryContentRequest request )
            throws AproxClientException
    {
        return getHttp().postWithResponse( "depgraph/repo/content", request, RepoContentResult.class );
    }

    public String repositoryDownloadLog( DownlogRequest request )
            throws AproxClientException
    {
        return postWithStringOutput( "depgraph/repo/downlog", request );
    }

    public InputStream repositoryZip( RepositoryContentRequest request )
            throws AproxClientException, IOException
    {
        HttpResources resources = getHttp().postRaw( "depgraph/repo/zip", request );
        if ( resources.getStatusCode() != HttpStatus.SC_OK )
        {
            throw new AproxClientException( resources.getStatusCode(), "Error retrieving downlog.\n%s",
                                            new AproxResponseErrorDetails( resources.getResponse() ) );
        }

        return resources.getResponseEntityContent();
    }

    public String pom( PomRequest request )
            throws AproxClientException
    {
        return postWithStringOutput( "depgraph/render/pom", request );
    }

    public String dotfile( MultiRenderRequest request )
            throws AproxClientException
    {
        Map<String, String> params = request.getRenderParams();
        if ( params != null && !params.containsKey( "name" ) && !params.containsKey( "coord" ) )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.warn(
                    "You have not specified a 'name' or 'coord' parameter to be used in naming your Graphviz dotfile!" );
        }

        return postWithStringOutput( "depgraph/render/dotfile", request );
    }

    public String depTree( RepositoryContentRequest request )
            throws AproxClientException
    {
        return postWithStringOutput( "depgraph/render/depTree", request );
    }

    public String depList( RepositoryContentRequest request )
            throws AproxClientException
    {
        return postWithStringOutput( "depgraph/render/depList", request );
    }

    public ProjectGraphRequestBuilder newProjectGraphRequest()
    {
        return ProjectGraphRequestBuilder.newProjectGraphRequestBuilder();
    }

    public ProjectGraphRelationshipsRequestBuilder newProjectGraphRelationshipsRequest()
    {
        return ProjectGraphRelationshipsRequestBuilder.newProjectGraphRelationshipsRequestBuilder();
    }

    public MultiGraphRequestBuilder newMultiGraphRequest()
    {
        return MultiGraphRequestBuilder.newMultiGraphResolverRequestBuilder();
    }

    public PomRequestBuilder newPomRequest()
    {
        return PomRequestBuilder.newPomRequestBuilder();
    }

    public DownlogRequestBuilder newDownlogRequest()
    {
        return DownlogRequestBuilder.newDownlogRequestBuilder();
    }

    public GraphAnalysisRequestBuilder newGraphAnalysisRequest()
    {
        return GraphAnalysisRequestBuilder.newAnalysisRequestBuilder();
    }

    public MetadataExtractionRequestBuilder newMetadataExtractionRequest()
    {
        return MetadataExtractionRequestBuilder.newMetadataRecipeBuilder();
    }

    public MetadataUpdateRequestBuilder newMetadataUpdateRequest()
    {
        return MetadataUpdateRequestBuilder.newMetadataRecipeBuilder();
    }

    public MetadataCollationRequestBuilder newMetadataCollationRequest()
    {
        return MetadataCollationRequestBuilder.newMetadataRecipeBuilder();
    }

    public MultiRenderRequestBuilder newMultiRenderRequest()
    {
        return MultiRenderRequestBuilder.newMultiRenderRecipeBuilder();
    }

    public PathsRequestBuilder newPathsRequest()
    {
        return PathsRequestBuilder.newPathsRecipeBuilder();
    }

    public RepositoryContentRequestBuilder newRepositoryContentRequest()
    {
        return RepositoryContentRequestBuilder.newRepositoryContentRecipeBuilder();
    }

    private String postWithStringOutput( String path, Object request )
            throws AproxClientException
    {
        String result = null;
        try (HttpResources resources = getHttp().postRaw( path, request ))
        {
            if ( resources.getStatusCode() != HttpStatus.SC_OK )
            {
                throw new AproxClientException( resources.getStatusCode(), "Error retrieving response string.\n%s",
                                                new AproxResponseErrorDetails( resources.getResponse() ) );
            }

            result = IOUtils.toString( resources.getResponseEntityContent() );
        }
        catch ( IOException e )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.warn( "Error closing response to path: " + path + ". Error: " + e.getMessage(), e );
        }

        return result;
    }

}
