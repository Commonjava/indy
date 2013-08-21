package org.commonjava.aprox.core.rest;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

@Path( "/" )
@javax.enterprise.context.ApplicationScoped
public class RootResource
{

    @GET
    public void rootStats( @Context final HttpServletResponse response, @Context final UriInfo info )
        throws ServletException, IOException
    {
        response.sendRedirect( info.getBaseUriBuilder()
                                   .path( "/stats/version-info" )
                                   .build()
                                   .toURL()
                                   .toExternalForm() );
    }

}
