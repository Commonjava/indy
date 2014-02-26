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
package org.commonjava.aprox.bind.vertx.admin;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.core.dto.repl.ReplicationDTO;
import org.commonjava.aprox.core.rest.ReplicationController;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.web.json.ser.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( prefix = "/admin/replicate" )
public class ReplicationResource
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ReplicationController controller;

    @Inject
    @AproxData
    private JsonSerializer serializer;

    @Routes( { @Route( method = Method.POST, contentType = ApplicationContent.application_json ) } )
    public void replicate( final Buffer buffer, final HttpServerRequest request )
    {
        final String json = buffer.getString( 0, buffer.length() );
        final ReplicationDTO dto = serializer.fromString( json, ReplicationDTO.class );

        try
        {
            final Set<StoreKey> replicated = controller.replicate( dto );

            final Map<String, Object> params = new LinkedHashMap<String, Object>();
            params.put( "replicationCount", replicated.size() );
            params.put( "items", replicated );
            formatOkResponseWithJsonEntity( request, serializer.toString( params ) );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Replication failed: {}", e, e.getMessage() );
            formatResponse( e, request );
        }
    }

}
