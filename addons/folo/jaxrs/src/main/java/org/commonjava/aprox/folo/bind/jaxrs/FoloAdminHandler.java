package org.commonjava.aprox.folo.bind.jaxrs;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.folo.ctl.FoloAdminController;
import org.commonjava.aprox.folo.dto.TrackedContentDTO;
import org.commonjava.aprox.folo.model.TrackedContentRecord;
import org.commonjava.aprox.folo.model.TrackingKey;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/api/folo/admin" )
@ApplicationScoped
public class FoloAdminHandler
    implements AproxResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private AproxObjectMapper objectMapper;

    @Inject
    private FoloAdminController controller;

    @Path( "/{id}/report" )
    @GET
    public Response getReport( final @PathParam( "id" ) String id, @Context final UriInfo uriInfo )
    {
        Response response;
        try
        {
            //            final String baseUrl = uriInfo.getAbsolutePathBuilder()
            final String baseUrl = uriInfo.getBaseUriBuilder()
                                          .path( "api" )
                                          .build()
                                          .toString();
            //                                          .path( ContentAccessHandler.class )
            //                                          .build( st.singularEndpointName(), name )
            //                                          .toString();
            
            final TrackedContentDTO report = controller.renderReport( id, baseUrl );

            if ( report == null )
            {
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }
            else
            {
                response = formatOkResponseWithJsonEntity( report, objectMapper );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to serialize tracking report for: %s. Reason: %s", id,
                                         e.getMessage() ),
                          e );

            response = formatResponse( e, true );
        }

        return response;
    }

    @Path( "/{id}/record" )
    @GET
    public Response getRecord( final @PathParam( "id" ) String id )
    {
        final TrackingKey tk = new TrackingKey( id );

        Response response;
        try
        {
            final TrackedContentRecord record = controller.getRecord( id );
            if ( record == null )
            {
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }
            else
            {
                response = formatOkResponseWithJsonEntity( record, objectMapper );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to retrieve tracking report for: %s. Reason: %s", tk, e.getMessage() ),
                          e );

            response = formatResponse( e, true );
        }

        return response;
    }

    @Path( "/{id}/record" )
    @DELETE
    public Response clearRecord( final @PathParam( "id" ) String id )
    {
        controller.clearRecord( id );
        return Response.status( Status.NO_CONTENT )
                       .build();
    }

}
