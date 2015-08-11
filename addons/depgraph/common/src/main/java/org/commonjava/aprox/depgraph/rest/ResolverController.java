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
package org.commonjava.aprox.depgraph.rest;

import java.io.InputStream;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.conf.AproxDepgraphConfig;
import org.commonjava.aprox.depgraph.util.RecipeHelper;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.maven.cartographer.CartoRequestException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.ops.ResolveOps;
import org.commonjava.maven.cartographer.request.MultiGraphRequest;

public class ResolverController
{

    @Inject
    private ResolveOps ops;

    @Inject
    private AproxDepgraphConfig config;

    @Inject
    private RecipeHelper configHelper;

    public void resolve( final InputStream stream )
        throws AproxWorkflowException
    {
        final MultiGraphRequest recipe = configHelper.readRecipe( stream, MultiGraphRequest.class );

        resolve( recipe );
    }

    public void resolve( final MultiGraphRequest recipe )
        throws AproxWorkflowException
    {
        recipe.setDefaultPreset( config.getDefaultWebFilterPreset() );

        try
        {
            // TODO: we could provide some feedback to the user based on the graph...
            ops.resolveGraphs( recipe, ( graph ) -> {
                IOUtils.closeQuietly( graph );
            } );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup incomplete subgraphs for: {}. Reason: {}", e, recipe,
                                              e.getMessage() );
        }
        catch ( CartoRequestException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }

}
