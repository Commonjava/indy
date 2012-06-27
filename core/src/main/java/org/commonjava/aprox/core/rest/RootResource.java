package org.commonjava.aprox.core.rest;

import java.io.IOException;

import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path( "/" )
@Singleton
public class RootResource
    extends AbstractURLAliasingResource
{

    @GET
    public void rootStats()
        throws ServletException, IOException
    {
        forward( "/stats/version-info" );
    }

}
