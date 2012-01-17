package org.commonjava.aprox.autoprox.live.fixture;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path( "/target" )
@Singleton
public class TargetUrlResponder
{

    private final List<String> approvedTargets = new ArrayList<String>();

    public void approveTargets( final String... names )
    {
        for ( final String name : names )
        {
            approvedTargets.add( name );
        }
    }

    @Path( "{name}" )
    @GET
    public Response get( @PathParam( "name" ) final String name )
    {
        if ( approvedTargets.contains( name ) )
        {
            return Response.ok()
                           .build();
        }

        return Response.status( Status.NOT_FOUND )
                       .build();
    }

    public void clearTargets()
    {
        approvedTargets.clear();
    }

}
