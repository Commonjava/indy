/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.bind.jaxrs.admin;

import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.util.AproxExceptionUtils;
import org.commonjava.aprox.core.dto.repl.ReplicationDTO;
import org.commonjava.aprox.core.rest.ReplicationController;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.web.json.ser.JsonSerializer;
import org.commonjava.web.json.ser.ServletSerializerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/admin/replicate" )
public class ReplicationResource
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ReplicationController controller;

    @Inject
    @AproxData
    private JsonSerializer serializer;

    @POST
    @Consumes( "application/json" )
    @Produces( "text/plain" )
    public Response replicate( @Context final HttpServletRequest req )
    {
        final ReplicationDTO dto = ServletSerializerUtils.fromRequestBody( req, serializer, ReplicationDTO.class );
        try
        {
            final Set<StoreKey> replicated = controller.replicate( dto );

            return Response.ok( replicated.size() + " entries replicated:\n\n  " + new JoinString( "\n  ", replicated ) + "\n\n" )
                           .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Replication failed: %s", e.getMessage() ), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

}
