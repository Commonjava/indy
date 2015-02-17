package org.commonjava.aprox.revisions.vertx;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.bind.jaxrs.util.ResponseUtils;
import org.commonjava.aprox.revisions.RevisionsManager;
import org.commonjava.aprox.revisions.vertx.dto.ChangeSummaryDTO;
import org.commonjava.aprox.subsys.git.GitSubsystemException;
import org.commonjava.aprox.util.ApplicationContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@Path( "/admin/revisions" )
@ApplicationScoped
public class RevisionsAdminResource
    implements AproxResources
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
                ResponseUtils.formatResponse( e, "Failed to pull git updates for data dir: " + e.getMessage(), true );
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
                ResponseUtils.formatResponse( e, "Failed to push git updates for data dir: " + e.getMessage(), true );
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
            response = formatResponse( e, "Failed to read git changelog from data dir.", true );
        }

        return response;
    }

}
