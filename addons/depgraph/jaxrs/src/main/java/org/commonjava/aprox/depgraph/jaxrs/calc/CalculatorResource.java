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
package org.commonjava.aprox.depgraph.jaxrs.calc;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.throwError;
import static org.commonjava.aprox.util.ApplicationContent.application_json;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.depgraph.rest.CalculatorController;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.cartographer.request.GraphAnalysisRequest;
import org.commonjava.cartographer.request.GraphCalculation;
import org.commonjava.cartographer.request.MultiGraphRequest;
import org.commonjava.cartographer.result.GraphDifference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/api/depgraph/calc" )
@Consumes( { "application/json", "application/aprox*+json" } )
@Produces( { "application/json", "application/aprox*+json" } )
public class CalculatorResource
    implements AproxResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private CalculatorController controller;

    @Path( "/diff" )
    @POST
    @Produces( application_json )
    public GraphDifference<ProjectRelationship<?, ?>> difference( final GraphAnalysisRequest request )
    {
        Response response = null;
        try
        {
            return controller.difference( request );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }
        return null;
    }

    @Path( "/drift" )
    @POST
    @Produces( application_json )
    public GraphDifference<ProjectVersionRef> drift( final GraphAnalysisRequest request )
    {
        Response response = null;
        try
        {
            return controller.drift( request );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }
        return null;
    }

    @POST
    @Produces( application_json )
    public GraphCalculation calculate( MultiGraphRequest request )
    {
        try
        {
            return controller.calculate( request );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }
        return null;
    }
}
