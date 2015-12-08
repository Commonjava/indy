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
package org.commonjava.indy.revisions.jaxrs;

import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatResponse;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.util.ResponseUtils;
import org.commonjava.indy.revisions.RevisionsManager;
import org.commonjava.indy.revisions.jaxrs.dto.ChangeSummaryDTO;
import org.commonjava.indy.subsys.git.GitSubsystemException;
import org.commonjava.indy.util.ApplicationContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@Path( "/api/admin/revisions" )
@ApplicationScoped
public class RevisionsAdminResource
    implements IndyResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private RevisionsManager revisionsManager;

    @Inject
    private ObjectMapper objectMapper;

    @Path( "/data/pull" )
    @GET
    public Response pullDataGitUpdates()
    {
        Response response;
        try
        {
            revisionsManager.pullDataUpdates();

            // FIXME: Return some status
            response = Response.ok()
                               .build();
        }
        catch ( final GitSubsystemException e )
        {
            logger.error( "Failed to pull git updates for data dir: " + e.getMessage(), e );
            response =
                ResponseUtils.formatResponse( e, "Failed to pull git updates for data dir: " + e.getMessage() );
        }

        return response;
    }

    @Path( "/data/push" )
    @GET
    public Response pushDataGitUpdates()
    {
        Response response;
        try
        {
            revisionsManager.pushDataUpdates();

            // FIXME: Return some status
            response = Response.ok()
                               .build();
        }
        catch ( final GitSubsystemException e )
        {
            logger.error( "Failed to push git updates for data dir: " + e.getMessage(), e );
            response =
                ResponseUtils.formatResponse( e, "Failed to push git updates for data dir: " + e.getMessage() );
        }

        return response;
    }

    @Path( "/data/changelog{path: /.*}" )
    @GET
    @Produces( ApplicationContent.application_json )
    public Response doGet( final @PathParam( "path" ) String path, final @QueryParam( "start" ) int start,
                           final @QueryParam( "count" ) int count )
    {
        Response response;
        try
        {
            final List<ChangeSummary> listing = revisionsManager.getDataChangeLog( path, start, count );

            response = formatOkResponseWithJsonEntity( new ChangeSummaryDTO( listing ), objectMapper );
        }
        catch ( final GitSubsystemException e )
        {
            logger.error( "Failed to read git changelog from data dir: " + e.getMessage(), e );
            response = formatResponse( e, "Failed to read git changelog from data dir." );
        }

        return response;
    }

}
