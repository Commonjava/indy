package org.commonjava.aprox.autoprox.vertx;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatOkResponseWithJsonEntity;

import java.util.Collections;

import javax.inject.Inject;

import org.commonjava.aprox.autoprox.rest.AutoProxAdminController;
import org.commonjava.aprox.autoprox.rest.dto.CatalogDTO;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.web.json.ser.JsonSerializer;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( "/autoprox/catalog" )
public class AutoProxCatalogResource
    implements RequestHandler
{

    @Inject
    @AproxData
    private JsonSerializer serializer;

    @Inject
    private AutoProxAdminController controller;

    @Routes( { @Route( method = Method.GET, contentType = ApplicationContent.application_json ) } )
    public void getCatalog( final Buffer buffer, final HttpServerRequest request )
    {
        final CatalogDTO dto = controller.getCatalog();

        formatOkResponseWithJsonEntity( request,
                                        serializer.toString( dto == null ? Collections.singletonMap( "error",
                                                                                                     "Rule catalog is unavailable!" )
                                                        : dto ) );
    }
}
