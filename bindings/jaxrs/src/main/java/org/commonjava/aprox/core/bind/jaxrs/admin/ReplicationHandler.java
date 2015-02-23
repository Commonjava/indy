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
package org.commonjava.aprox.core.bind.jaxrs.admin;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.bind.jaxrs.util.SecurityParam;
import org.commonjava.aprox.core.ctl.ReplicationController;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.dto.ReplicationDTO;
import org.commonjava.aprox.util.ApplicationContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@Path( "/api/admin/replicate" )
public class ReplicationHandler
    implements AproxResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ReplicationController controller;

    @Inject
    private ObjectMapper serializer;

    @POST
    @Produces( ApplicationContent.application_json )
    public Response replicate( @Context final HttpServletRequest request )
    {
        Response response;
        try
        {
            final String user = (String) request.getSession( true )
                                                .getAttribute( SecurityParam.user.key() );

            final ReplicationDTO dto = serializer.readValue( request.getInputStream(), ReplicationDTO.class );
            final Set<StoreKey> replicated = controller.replicate( dto, user );

            final Map<String, Object> params = new LinkedHashMap<String, Object>();
            params.put( "replicationCount", replicated.size() );
            params.put( "items", replicated );

            response = formatOkResponseWithJsonEntity( params, serializer );
        }
        catch ( final AproxWorkflowException | IOException e )
        {
            logger.error( String.format( "Replication failed: %s", e.getMessage() ), e );
            response = formatResponse( e, true );
        }

        return response;
    }

}
