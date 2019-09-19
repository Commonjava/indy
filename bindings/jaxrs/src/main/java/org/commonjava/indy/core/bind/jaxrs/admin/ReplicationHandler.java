/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.core.bind.jaxrs.admin;

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
import javax.ws.rs.core.SecurityContext;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.SecurityManager;
import org.commonjava.indy.bind.jaxrs.util.REST;
import org.commonjava.indy.bind.jaxrs.util.ResponseHelper;
import org.commonjava.indy.core.ctl.ReplicationController;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.dto.ReplicationDTO;
import org.commonjava.indy.util.ApplicationContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@Api( description = "Replicate the artifact stores on a remote Indy instance, either by proxying the remote system's stores or by cloning the store definitions",
      value = "Indy Repository Replication" )
@Path( "/api/admin/replicate" )
@REST
public class ReplicationHandler
        implements IndyResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ReplicationController controller;

    @Inject
    private ObjectMapper serializer;

    @Inject
    private SecurityManager securityManager;

    @Inject
    private ResponseHelper responseHelper;

    @ApiOperation( "Replicate the stores of a remote Indy" )
    @ApiImplicitParams( { @ApiImplicitParam( paramType = "body", name = "body",
                                             dataType = "org.commonjava.indy.model.core.dto.ReplicationDTO",
                                             required = true,
                                             value = "The configuration determining how replication should be handled, and what remote site to replicate." ) } )
    @POST
    @Produces( ApplicationContent.application_json )
    public Response replicate( @Context final HttpServletRequest request,
                               final @Context SecurityContext securityContext )
    {
        Response response;
        try
        {
            String user = securityManager.getUser( securityContext, request );

            final ReplicationDTO dto = serializer.readValue( request.getInputStream(), ReplicationDTO.class );
            final Set<StoreKey> replicated = controller.replicate( dto, user );

            final Map<String, Object> params = new LinkedHashMap<String, Object>();
            params.put( "replicationCount", replicated.size() );
            params.put( "items", replicated );

            response = responseHelper.formatOkResponseWithJsonEntity( params );
        }
        catch ( final IndyWorkflowException | IOException e )
        {
            logger.error( String.format( "Replication failed: %s", e.getMessage() ), e );
            response = responseHelper.formatResponse( e );
        }

        return response;
    }

}
