package org.commonjava.aprox.core.bind.jaxrs;

import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.core.ctl.NfcController;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.dto.NotFoundCacheDTO;
import org.commonjava.aprox.util.ApplicationContent;

import com.fasterxml.jackson.databind.ObjectMapper;

@Path( "/api/nfc" )
public class NfcResource
    implements AproxResources
{

    @Inject
    private NfcController controller;

    @Inject
    private ObjectMapper serializer;

    @DELETE
    public Response clearAll()
    {
        controller.clear();
        return Response.ok()
                       .build();
    }

    @Path( "/{type: (hosted|group|remote)}/{name}{path: (/.+)?}" )
    @DELETE
    public Response clearStore( final @PathParam( "type" ) String t, final @PathParam( "name" ) String name,
                                final @PathParam( "path" ) String p )
    {
        Response response;
        final StoreType type = StoreType.get( t );
        final StoreKey key = new StoreKey( type, name );
        try
        {
            if ( isNotEmpty( p ) )
            {
                controller.clear( key );
            }
            else
            {
                controller.clear( key, p );
            }

            response = Response.ok()
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            response = formatResponse( e, true );
        }

        return response;
    }

    @GET
    @Produces( ApplicationContent.application_json )
    public Response getAll()
    {
        final NotFoundCacheDTO dto = controller.getAllMissing();

        return formatOkResponseWithJsonEntity( dto, serializer );
    }

    @Path( "/{type: (hosted|group|remote)}/{name}" )
    @GET
    @Produces( ApplicationContent.application_json )
    public Response getStore( final @PathParam( "type" ) String t, final @PathParam( "name" ) String name )
    {
        Response response;
        final StoreType type = StoreType.get( t );
        final StoreKey key = new StoreKey( type, name );
        try
        {
            final NotFoundCacheDTO dto = controller.getMissing( key );

            response = formatOkResponseWithJsonEntity( dto, serializer );
        }
        catch ( final AproxWorkflowException e )
        {
            response = formatResponse( e, true );
        }
        return response;
    }

}
