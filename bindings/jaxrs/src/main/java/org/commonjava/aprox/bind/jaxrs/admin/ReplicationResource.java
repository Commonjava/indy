/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.bind.jaxrs.admin;

import static org.apache.commons.lang.StringUtils.join;

import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.bind.jaxrs.util.AproxExceptionUtils;
import org.commonjava.aprox.core.dto.repl.ReplicationDTO;
import org.commonjava.aprox.core.rest.ReplicationController;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.JsonSerializer;
import org.commonjava.web.json.ser.ServletSerializerUtils;

@Path( "/admin/replicate" )
public class ReplicationResource
{

    private final Logger logger = new Logger( getClass() );

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

            return Response.ok( replicated.size() + " entries replicated:\n\n  " + join( replicated, "\n  " ) + "\n\n" )
                           .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Replication failed: %s", e, e.getMessage() );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

}
