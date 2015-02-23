package org.commonjava.aprox.autoprox.jaxrs;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;

import java.util.Collections;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.autoprox.rest.AutoProxAdminController;
import org.commonjava.aprox.autoprox.rest.dto.CatalogDTO;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.util.ApplicationContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path( "/api/autoprox/catalog" )
public class AutoProxCatalogResource
    implements AproxResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ObjectMapper serializer;

    @Inject
    private AutoProxAdminController controller;

    @GET
    @Consumes( ApplicationContent.application_json )
    public Response getCatalog()
    {
        final CatalogDTO dto = controller.getCatalog();

        Response response;
        try
        {
            response =
                formatOkResponseWithJsonEntity( serializer.writeValueAsString( dto == null ? Collections.singletonMap( "error",
                                                                                                                   "Rule catalog is unavailable!" )
                                                            : dto ) );
        }
        catch ( final JsonProcessingException e )
        {
            logger.error( String.format( "Failed to retrieve rule catalog. Reason: %s", e.getMessage() ), e );
            response = formatResponse( e );
        }

        return response;
    }
}
