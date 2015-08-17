package org.commonjava.aprox.depgraph.impl;

import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.depgraph.client.DepgraphAproxClientModule;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.ops.GraphOps;
import org.commonjava.cartographer.request.PathsRequest;
import org.commonjava.cartographer.request.ProjectGraphRelationshipsRequest;
import org.commonjava.cartographer.request.ProjectGraphRequest;
import org.commonjava.cartographer.request.SingleGraphRequest;
import org.commonjava.cartographer.result.*;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;

/**
 * Created by jdcasey on 8/17/15.
 */
public class ClientGraphOps
        implements GraphOps
{
    private ClientCartographer carto;

    private DepgraphAproxClientModule module;

    public ClientGraphOps( ClientCartographer carto, DepgraphAproxClientModule module )
    {
        this.carto = carto;
        this.module = module;
    }

    @Override
    public ProjectListResult listProjects( ProjectGraphRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.list( carto.normalizeRequest( request ) );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public ProjectPathsResult getPaths( PathsRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.paths( carto.normalizeRequest( request ) );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public ProjectErrors getProjectErrors( ProjectGraphRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.errors( carto.normalizeRequest( request ) );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public MappedProjectResult getProjectParent( ProjectGraphRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.parents( carto.normalizeRequest( request ) );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public MappedProjectRelationshipsResult getDirectRelationshipsFrom( ProjectGraphRelationshipsRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.relationshipsDeclaredBy( carto.normalizeRequest( request ) );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public MappedProjectRelationshipsResult getDirectRelationshipsTo( ProjectGraphRelationshipsRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.relationshipsTargeting( carto.normalizeRequest( request ) );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public ProjectListResult reindex( ProjectGraphRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.reindex( carto.normalizeRequest( request ) );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public ProjectListResult getIncomplete( ProjectGraphRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.incomplete( carto.normalizeRequest( request ) );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public ProjectListResult getVariable( ProjectGraphRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.variable( carto.normalizeRequest( request ) );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public MappedProjectsResult getAncestry( ProjectGraphRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.ancestors( carto.normalizeRequest( request ) );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public BuildOrder getBuildOrder( ProjectGraphRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.buildOrder( carto.normalizeRequest( request ) );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public GraphExport exportGraph( SingleGraphRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.export( carto.normalizeRequest( request ) );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }
}
