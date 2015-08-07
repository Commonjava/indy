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
package org.commonjava.aprox.depgraph.jaxrs;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.throwError;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.depgraph.rest.MetadataController;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.dto.MetadataCollation;
import org.commonjava.maven.cartographer.recipe.MetadataCollationRecipe;
import org.commonjava.maven.cartographer.recipe.MetadataExtractionRecipe;
import org.commonjava.maven.cartographer.recipe.MetadataUpdateRecipe;

@Path( "/api/depgraph/meta" )
@Consumes( { "application/json", "application/aprox*+json" } )
@Produces( { "applicaiton/json", "application/aprox*+json" } )
public class MetadataResource
    implements AproxResources
{

    @Inject
    private MetadataController controller;

    @Path( "/updates" )
    @POST
    @Consumes( ApplicationContent.application_json )
    public List<ProjectVersionRef> batchUpdate( final MetadataUpdateRecipe recipe )
    {
        try
        {
            return controller.batchUpdate( recipe );
        }
        catch ( final AproxWorkflowException e )
        {
            throwError( e );
        }
        return null;
    }

    @POST
    public Map<ProjectVersionRef, Map<String, String>> getMetadata( final MetadataExtractionRecipe recipe )
    {
        try
        {
            return controller.getMetadata( recipe );
        }
        catch ( final AproxWorkflowException e )
        {
            throwError( e );
        }

        return null;
    }

    @Path( "/collation" )
    @POST
    public MetadataCollation getCollation( final MetadataCollationRecipe recipe )
    {
        try
        {
            return controller.getCollation( recipe );
        }
        catch ( final AproxWorkflowException e )
        {
            throwError( e );
        }

        return null;
    }
}
