package org.commonjava.aprox.core.rest.stats;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.StoreDataManager;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.rest.AbstractURLAliasingResource;
import org.commonjava.aprox.core.stats.AProxVersioning;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.JsonSerializer;

@Path( "/stats" )
@Singleton
public class BasicStatsResource
    extends AbstractURLAliasingResource
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private AProxVersioning versioning;

    @Inject
    private StoreDataManager dataManager;

    @Inject
    private JsonSerializer serializer;

    @Context
    private UriInfo uriInfo;

    @GET
    @Path( "/version-info" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getAProxVersion()
    {
        return Response.ok( serializer.toString( versioning ) )
                       .build();
    }

    @GET
    @Path( "/all-endpoints" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response getAllEndpoints()
    {
        final List<ArtifactStore> stores = new ArrayList<ArtifactStore>();
        try
        {
            stores.addAll( dataManager.getAllDeployPoints() );
            stores.addAll( dataManager.getAllRepositories() );
            stores.addAll( dataManager.getAllGroups() );
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to retrieve all endpoints: %s", e, e.getMessage() );
            return Response.serverError()
                           .build();
        }

        final EndpointViewListing listing = new EndpointViewListing( stores, uriInfo );
        final String json = serializer.toString( listing );

        return Response.ok( json )
                       .build();
    }

}
