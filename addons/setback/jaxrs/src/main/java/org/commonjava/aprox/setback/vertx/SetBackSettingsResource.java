package org.commonjava.aprox.setback.vertx;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.bind.jaxrs.util.ResponseUtils;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.setback.rest.SetBackController;
import org.commonjava.aprox.subsys.datafile.DataFile;
import org.commonjava.aprox.util.ApplicationContent;

@Path( "/setback" )
public class SetBackSettingsResource
    implements AproxResources
{

    @Inject
    private SetBackController controller;

    @Path( "/{type: (remote|group)}/{name}" )
    @GET
    @Produces( ApplicationContent.application_xml )
    public Response get( final @PathParam( "type" ) String t, final @PathParam( "name" ) String n )
    {
        final StoreType type = StoreType.get( t );

        if ( StoreType.hosted == type )
        {
            return Response.status( Status.BAD_REQUEST )
                           .build();
        }

        Response response;

        final StoreKey key = new StoreKey( type, n );
        DataFile settingsXml = null;
        try
        {
            settingsXml = controller.getSetBackSettings( key );
        }
        catch ( final AproxWorkflowException e )
        {
            response = ResponseUtils.formatResponse( e );
        }

        if ( settingsXml != null && settingsXml.exists() )
        {
            response = Response.ok( settingsXml )
                               .type( ApplicationContent.application_xml )
                               .build();
        }
        else
        {
            response = Response.status( Status.NOT_FOUND )
                               .build();
        }

        return response;
    }

    @Path( "/{type: (remote|group)}/{name}" )
    @DELETE
    public Response delete( final @PathParam( "type" ) String t, final @PathParam( "name" ) String n )
    {
        final StoreType type = StoreType.get( t );

        if ( StoreType.hosted == type )
        {
            return Response.status( Status.BAD_REQUEST )
                           .build();
        }

        Response response;

        final StoreKey key = new StoreKey( type, n );
        try
        {
            final boolean found = controller.deleteSetBackSettings( key );

            if ( found )
            {
                response = Response.status( Status.NO_CONTENT )
                                   .build();
            }
            else
            {
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            response = ResponseUtils.formatResponse( e );
        }

        return response;
    }

}
