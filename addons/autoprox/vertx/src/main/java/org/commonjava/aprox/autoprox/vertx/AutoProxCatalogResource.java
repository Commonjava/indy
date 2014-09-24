package org.commonjava.aprox.autoprox.vertx;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;

import java.util.Collections;

import javax.inject.Inject;

import org.commonjava.aprox.autoprox.rest.AutoProxAdminController;
import org.commonjava.aprox.autoprox.rest.dto.CatalogDTO;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Handles( "/autoprox/catalog" )
public class AutoProxCatalogResource
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ObjectMapper serializer;

    @Inject
    private AutoProxAdminController controller;

    @Routes( { @Route( method = Method.GET, contentType = ApplicationContent.application_json ) } )
    public void getCatalog( final Buffer buffer, final HttpServerRequest request )
    {
        final CatalogDTO dto = controller.getCatalog();

        try
        {
            formatOkResponseWithJsonEntity( request,
                                            serializer.writeValueAsString( dto == null ? Collections.singletonMap( "error",
                                                                                                                   "Rule catalog is unavailable!" )
                                                            : dto ) );
        }
        catch ( final JsonProcessingException e )
        {
            logger.error( String.format( "Failed to retrieve rule catalog. Reason: %s", e.getMessage() ), e );
            formatResponse( e, request );
        }
    }
}
