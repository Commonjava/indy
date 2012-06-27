package org.commonjava.aprox.core.rest.stats;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.core.rest.AbstractURLAliasingResource;
import org.commonjava.aprox.core.stats.AProxVersioning;
import org.commonjava.web.json.ser.JsonSerializer;

@Path( "/stats" )
@Singleton
public class BasicStatsResource
    extends AbstractURLAliasingResource
{

    @Inject
    private AProxVersioning versioning;

    @Inject
    private JsonSerializer serializer;

    @GET
    @Path( "/version-info" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getAProxVersion()
    {
        return Response.ok( serializer.toString( versioning ) )
                       .build();
    }

}
