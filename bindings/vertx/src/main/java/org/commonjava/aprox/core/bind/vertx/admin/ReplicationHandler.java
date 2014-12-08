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
package org.commonjava.aprox.core.bind.vertx.admin;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.vertx.util.SecurityParam;
import org.commonjava.aprox.core.ctl.ReplicationController;
import org.commonjava.aprox.core.dto.ReplicationDTO;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.vertx.vabr.util.Respond;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Handles( prefix = "/admin/replicate" )
public class ReplicationHandler
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ReplicationController controller;

    @Inject
    private ObjectMapper serializer;

    @Routes( { @Route( method = Method.POST, contentType = ApplicationContent.application_json ) } )
    public void replicate( final Buffer buffer, final HttpServerRequest request )
    {
        final String json = buffer.getString( 0, buffer.length() );

        try
        {
            final ReplicationDTO dto = serializer.readValue( json, ReplicationDTO.class );
            final Set<StoreKey> replicated = controller.replicate( dto, request.params()
                                                                               .get( SecurityParam.user.key() ) );

            final Map<String, Object> params = new LinkedHashMap<String, Object>();
            params.put( "replicationCount", replicated.size() );
            params.put( "items", replicated );
            Respond.to( request )
                   .jsonEntity( params, serializer )
                   .send();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Replication failed: %s", e.getMessage() ), e );
            formatResponse( e, request );
        }
        catch ( final JsonProcessingException e )
        {
            Respond.to( request )
                   .serverError( e, "Failed to deserialize/serialize JSON.", true )
                   .send();
        }
        catch ( final IOException e )
        {
            Respond.to( request )
                   .serverError( e, "Failed to read JSON content from request body.", true )
                   .send();
        }
    }

}
