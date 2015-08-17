package org.commonjava.aprox.depgraph.impl;

import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.depgraph.client.DepgraphAproxClientModule;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.ops.MetadataOps;
import org.commonjava.cartographer.request.MetadataCollationRequest;
import org.commonjava.cartographer.request.MetadataExtractionRequest;
import org.commonjava.cartographer.request.MetadataUpdateRequest;
import org.commonjava.cartographer.request.ProjectGraphRequest;
import org.commonjava.cartographer.result.MetadataCollationResult;
import org.commonjava.cartographer.result.MetadataResult;
import org.commonjava.cartographer.result.ProjectListResult;

/**
 * Created by jdcasey on 8/17/15.
 */
public class ClientMetadataOps
        implements MetadataOps
{
    private ClientCartographer carto;

    private final DepgraphAproxClientModule module;

    public ClientMetadataOps( ClientCartographer carto, DepgraphAproxClientModule module )
    {
        this.carto = carto;
        this.module = module;
    }

    @Override
    public MetadataResult getMetadata( MetadataExtractionRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.getMetadata( carto.normalizeRequest( request ) );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public ProjectListResult updateMetadata( MetadataUpdateRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.updateMetadata( carto.normalizeRequest( request ) );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public ProjectListResult rescanMetadata( ProjectGraphRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.rescanMetadata( carto.normalizeRequest( request ) );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public MetadataCollationResult collate( MetadataCollationRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.collateMetadata( carto.normalizeRequest( request ) );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }
}
