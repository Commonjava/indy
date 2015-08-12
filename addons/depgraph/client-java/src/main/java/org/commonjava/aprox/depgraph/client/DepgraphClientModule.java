package org.commonjava.aprox.depgraph.client;

import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.depgraph.model.DownlogRequest;
import org.commonjava.aprox.depgraph.model.UrlMapResult;
import org.commonjava.aprox.depgraph.model.WorkspaceList;
import org.commonjava.aprox.depgraph.model.builder.DownlogRequestBuilder;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.request.*;
import org.commonjava.maven.cartographer.request.build.*;
import org.commonjava.maven.cartographer.result.*;

import java.io.InputStream;

/**
 * Created by jdcasey on 8/12/15.
 */
public class DepgraphClientModule
                extends AproxClientModule
{
    public ProjectListResult list( ProjectGraphRequest request )
    {
        //TODO: Projects
        return null;
    }

    public MappedProjectResult parents( ProjectGraphRequest request )
    {
        //TODO: Projects
        return null;
    }

    public ProjectErrors errors( ProjectGraphRequest request )
    {
        // TODO: Graphs
        return null;
    }

    public ProjectListResult reindex( ProjectGraphRequest request )
    {
        // TODO: Graphs
        return null;
    }

    public ProjectListResult incomplete( ProjectGraphRequest request )
    {
        // TODO: Graphs
        return null;
    }

    public ProjectListResult missing( ProjectGraphRequest request )
    {
        return incomplete( request );
    }

    public ProjectListResult variable( ProjectGraphRequest request )
    {
        // TODO: Graphs
        return null;
    }

    public ProjectPathsResult paths( PathsRequest request )
    {
        // TODO: Graphs
        return null;
    }

    public MappedProjectsResult ancestors( ProjectGraphRequest request )
    {
        // TODO: Graphs
        return null;
    }

    public BuildOrder buildOrder( ProjectGraphRequest request )
    {
        // TODO: Graphs
        return null;
    }

    public GraphExport graph( ProjectGraphRequest request )
    {
        // TODO: Graphs
        return null;
    }

    public GraphExport export( ProjectGraphRequest request )
    {
        return graph( request );
    }

    public MappedProjectRelationshipsResult relationshipsDeclaredBy( ProjectGraphRelationshipsRequest request )
    {
        // TODO: Projects
        return null;
    }

    public MappedProjectRelationshipsResult relationshipsTargeting( ProjectGraphRelationshipsRequest request )
    {
        // TODO: Projects
        return null;
    }

    public MetadataResult getMetadata( MetadataExtractionRequest request )
    {
        // TODO: Metadata
        return null;
    }

    public ProjectListResult updateMetadata(MetadataUpdateRequest request)
    {
        // TODO: Metadata
        return null;
    }

    public MetadataCollationResult collateMetadata( MetadataCollationRequest request )
    {
        // TODO: Metadata
        return null;
    }

    public boolean deleteWorkspace( String workspaceId )
    {
        // TODO: Workspaces
        return false;
    }

    public WorkspaceList listWorkspaces()
    {
        // TODO: Workspaces
        return null;
    }

    public GraphDifference<ProjectVersionRef> calculateGraphDrift( GraphAnalysisRequest request )
    {
        // TODO: Calculator
        return null;
    }

    public GraphDifference<ProjectRelationship<?>> graphDiff(GraphAnalysisRequest request)
    {
        // TODO: Calculator
        return null;
    }

    public GraphCalculation calculate(MultiGraphRequest request)
    {
        // TODO: Calculator
        return null;
    }

    public UrlMapResult repositoryUrlMap(RepositoryContentRequest request)
    {
        // TODO: Repository
        return null;
    }

    public String repositoryDownloadLog( DownlogRequest request)
    {
        // TODO: Repository
        return null;
    }

    public InputStream repositoryZip( RepositoryContentRequest request)
    {
        // TODO: Repository
        return null;
    }

    public String pom( PomRequest request )
    {
        // TODO: Rendering
        return null;
    }

    public String dotfile( MultiRenderRequest request )
    {
        // TODO: Rendering
        return null;
    }

    public String depTree( RepositoryContentRequest request )
    {
        // TODO: Rendering
        return null;
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

}
