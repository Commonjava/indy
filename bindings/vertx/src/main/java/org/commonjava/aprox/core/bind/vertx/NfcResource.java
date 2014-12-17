package org.commonjava.aprox.core.bind.vertx;

import static org.commonjava.aprox.bind.vertx.util.PathParam.name;
import static org.commonjava.aprox.bind.vertx.util.PathParam.path;
import static org.commonjava.aprox.bind.vertx.util.PathParam.type;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.setStatus;
import static org.commonjava.aprox.util.ApplicationStatus.OK;
import static org.commonjava.vertx.vabr.types.Method.DELETE;
import static org.commonjava.vertx.vabr.types.Method.GET;

import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.core.ctl.NfcController;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.dto.NotFoundCacheDTO;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.vertx.vabr.util.Respond;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Handles( "/nfc" )
public class NfcResource
    implements RequestHandler
{

    @Inject
    private NfcController controller;

    @Inject
    private ObjectMapper serializer;

    @Route( method = DELETE )
    public void clearAll( final HttpServerRequest request )
    {
        controller.clear();
        setStatus( OK, request ).end();
    }

    @Route( path = "/:type/:name:?path=(/.+)", method = Method.DELETE )
    public void clearStore( final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final StoreType t = StoreType.get( params.get( type.key() ) );
        final StoreKey key = new StoreKey( t, params.get( name.key() ) );

        String p = params.get( path.key() );
        if ( p != null && p.startsWith( "//" ) )
        {
            p = p.substring( 1 );
        }

        try
        {
            if ( p == null )
            {
                controller.clear( key );
            }
            else
            {
                controller.clear( key, p );
            }

            setStatus( OK, request ).end();
        }
        catch ( final AproxWorkflowException e )
        {
            formatResponse( e, request );
        }
    }

    @Route( method = GET, contentType = ApplicationContent.application_json )
    public void getAll( final HttpServerRequest request )
    {
        final NotFoundCacheDTO dto = controller.getAllMissing();

        try
        {
            Respond.to( request )
                   .jsonEntity( dto, serializer )
                   .send();
        }
        catch ( final JsonProcessingException e )
        {
            Respond.to( request )
                   .serverError( e, "Failed to serialize to JSON.", true )
                   .send();
        }
    }

    @Route( path = "/:type/:name", method = Method.GET, contentType = ApplicationContent.application_json )
    public void getStore( final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final StoreType t = StoreType.get( params.get( type.key() ) );
        final StoreKey key = new StoreKey( t, params.get( name.key() ) );

        try
        {
            final NotFoundCacheDTO dto = controller.getMissing( key );

            Respond.to( request )
                   .jsonEntity( dto, serializer )
                   .send();
        }
        catch ( final AproxWorkflowException e )
        {
            Respond.to( request )
                   .serverError( e, true )
                   .send();
        }
        catch ( final JsonProcessingException e )
        {
            Respond.to( request )
                   .serverError( e, "Failed to serialize to JSON.", true )
                   .send();
        }
    }

}
