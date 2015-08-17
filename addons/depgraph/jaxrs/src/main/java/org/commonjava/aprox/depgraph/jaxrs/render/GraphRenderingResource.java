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
package org.commonjava.aprox.depgraph.jaxrs.render;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.depgraph.rest.RenderingController;
import org.commonjava.cartographer.request.MultiRenderRequest;
import org.commonjava.cartographer.request.PomRequest;
import org.commonjava.cartographer.request.RepositoryContentRequest;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.io.File;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.throwError;
import static org.commonjava.aprox.util.ApplicationContent.application_aprox_star_json;
import static org.commonjava.aprox.util.ApplicationContent.application_json;
import static org.commonjava.aprox.util.ApplicationContent.application_xml;
import static org.commonjava.aprox.util.ApplicationContent.text_plain;

@Path( "/api/depgraph/render" )
@Consumes( { application_json, application_aprox_star_json } )
public class GraphRenderingResource
                implements AproxResources
{

    private static final String TYPE_GRAPHVIZ = "text/x-graphviz";

    @Inject
    private RenderingController controller;

    @Path( "/pom" )
    @POST
    @Produces( application_xml )
    public String pom( PomRequest recipe )
    {
        try
        {
            return controller.pomFor( recipe );
        }
        catch ( final AproxWorkflowException e )
        {
            throwError( e );
        }
        return null;
    }

    @Path( "/dotfile" )
    @POST
    @Produces( TYPE_GRAPHVIZ )
    public String dotfile( final MultiRenderRequest recipe )
    {
        try
        {
            return controller.dotfile( recipe );
        }
        catch ( final AproxWorkflowException e )
        {
            throwError( e );
        }
        return null;
    }

    @Path( "/depTree" )
    @POST
    @Produces( text_plain )
    public File tree( final RepositoryContentRequest recipe )
    {
        try
        {
            return controller.tree( recipe );
        }
        catch ( final AproxWorkflowException e )
        {
            throwError( e );
        }
        return null;
    }

    @Path( "/depList" )
    @POST
    @Produces( text_plain )
    public File list( final RepositoryContentRequest recipe )
    {
        try
        {
            return controller.list( recipe );
        }
        catch ( final AproxWorkflowException e )
        {
            throwError( e );
        }
        return null;
    }
}
