package org.commonjava.aprox.bind.jaxrs;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.util.AproxExceptionUtils;
import org.commonjava.aprox.core.dto.NotFoundCacheDTO;
import org.commonjava.aprox.core.rest.NfcController;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.web.json.ser.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/nfc" )
public class NfcResource
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private NfcController controller;

    @Inject
    @AproxData
    private JsonSerializer serializer;

    @DELETE
    public Response clearAll()
    {
        controller.clear();
        return Response.ok()
                       .build();
    }

    @Path( "/{type}/{name}{path: (/.+)?}" )
    @DELETE
    public Response clearStore( @PathParam( "type" ) final String type, @PathParam( "name" ) final String name,
                                @PathParam( "path" ) final String path )
    {
        final StoreType t = StoreType.get( type );
        final StoreKey key = new StoreKey( t, name );

        Response response;
        try
        {
            if ( path == null )
            {
                controller.clear( key );
            }
            else
            {
                controller.clear( key, path );
            }

            response = Response.ok()
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to clear NFC: %s (path: %s). Reason: %s", key, path, e.getMessage() ),
                          e );
            response = AproxExceptionUtils.formatResponse( e );
        }

        return response;
    }

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response getAll()
    {
        final NotFoundCacheDTO dto = controller.getAllMissing();

        final String json = serializer.toString( dto );
        return Response.ok( json )
                       .build();
    }

    @GET
    @Path( "/{type}/{name}" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getStore( final @PathParam( "type" ) String type, @PathParam( "name" ) final String name )
    {
        final StoreType t = StoreType.get( type );
        final StoreKey key = new StoreKey( t, name );

        Response response;
        try
        {
            final NotFoundCacheDTO dto = controller.getMissing( key );

            final String json = serializer.toString( dto );
            response = Response.ok( json )
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to retrieve cached NFC paths for: %s (path: %s). Reason: %s", key,
                                         e.getMessage() ), e );
            response = AproxExceptionUtils.formatResponse( e );
        }

        return response;
    }

}
