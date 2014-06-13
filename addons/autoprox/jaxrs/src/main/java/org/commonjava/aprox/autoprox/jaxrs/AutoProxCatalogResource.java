package org.commonjava.aprox.autoprox.jaxrs;

import java.util.Collections;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.autoprox.rest.AutoProxAdminController;
import org.commonjava.aprox.autoprox.rest.dto.CatalogDTO;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.web.json.ser.JsonSerializer;

@Path( "/autoprox/catalog" )
public class AutoProxCatalogResource
{

    @Inject
    @AproxData
    private JsonSerializer serializer;

    @Inject
    private AutoProxAdminController controller;

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response getCatalog()
    {
        final CatalogDTO dto = controller.getCatalog();

        return Response.ok( serializer.toString( dto == null ? Collections.singletonMap( "error",
                                                                                         "Rule catalog is unavailable!" )
                                            : dto ) )
                       .build();
    }
}
