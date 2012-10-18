package org.commonjava.aprox.autoprox.live.fixture;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path( "/target" )
@javax.enterprise.context.ApplicationScoped
public class TargetUrlResponder
{

    private final List<String> approvedTargets = new ArrayList<String>();

    public TargetUrlResponder()
    {
        System.out.println( "\n\n\n\n\n\n\nstarting target responder\n\n\n\n\n\n" );
    }

    public void approveTargets( final String... names )
    {
        for ( final String name : names )
        {
            System.out.println( this + ": Approving: '" + name + "'" );
            approvedTargets.add( name );
        }
    }

    @Path( "{name}" )
    @GET
    public Response get( @PathParam( "name" ) final String name )
    {
        System.out.println( this + ": GET: '" + name + "'" );
        if ( approvedTargets.contains( name ) )
        {
            System.out.println( this + ": APPROVED" );
            return Response.ok()
                           .build();
        }

        System.out.println( this + ": NOT APPROVED" );
        return Response.status( Status.NOT_FOUND )
                       .entity( "NOT APPROVED" )
                       .build();
    }

    public void clearTargets()
    {
        approvedTargets.clear();
    }

}
