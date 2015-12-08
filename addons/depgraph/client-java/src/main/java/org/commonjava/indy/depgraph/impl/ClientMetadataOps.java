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

    private final DepgraphIndyClientModule module;

    public ClientMetadataOps( ClientCartographer carto, DepgraphIndyClientModule module )
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
        catch ( IndyClientException e )
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
        catch ( IndyClientException e )
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
        catch ( IndyClientException e )
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
        catch ( IndyClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }
}
