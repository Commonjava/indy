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
