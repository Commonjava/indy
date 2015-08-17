package org.commonjava.aprox.depgraph.impl;

import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.depgraph.client.DepgraphAproxClientModule;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.ops.CalculationOps;
import org.commonjava.cartographer.request.GraphAnalysisRequest;
import org.commonjava.cartographer.request.GraphCalculation;
import org.commonjava.cartographer.request.MultiGraphRequest;
import org.commonjava.cartographer.result.GraphDifference;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

/**
 * Created by jdcasey on 8/17/15.
 */
public class ClientCalculatorOps
        implements CalculationOps
{
    private ClientCartographer carto;

    private DepgraphAproxClientModule module;

    public ClientCalculatorOps( ClientCartographer carto, DepgraphAproxClientModule module )
    {
        this.carto = carto;
        this.module = module;
    }

    @Override
    public GraphDifference<ProjectRelationship<?>> difference( GraphAnalysisRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.graphDiff( carto.normalizeRequests( request ) );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public GraphDifference<ProjectVersionRef> intersectingTargetDrift( GraphAnalysisRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.calculateGraphDrift( carto.normalizeRequests( request ) );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public GraphCalculation calculate( MultiGraphRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.calculate( carto.normalizeRequest( request ) );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }
}
