package org.commonjava.aprox.folo.bind.vertx;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.folo.FoloAddOn.TRACKING_ID_PATH_PARAM;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.bind.vertx.util.PathParam;
import org.commonjava.aprox.folo.data.FoloContentException;
import org.commonjava.aprox.folo.data.FoloRecordManager;
import org.commonjava.aprox.folo.model.TrackedContentRecord;
import org.commonjava.aprox.folo.model.TrackingKey;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.ApplicationStatus;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.vertx.vabr.util.Respond;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServerRequest;

import com.fasterxml.jackson.core.JsonProcessingException;

@Handles( "/folo/admin" )
@ApplicationScoped
public class FoloAdminHandler
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private FoloRecordManager recordManager;

    @Inject
    private AproxObjectMapper objectMapper;

    @Routes( { @Route( path = "/report/:type/:name/:id", method = Method.GET ) } )
    public void getReport( final HttpServerRequest request )
    {
        request.pause();

        final String name = request.params()
                                   .get( PathParam.name.key() );

        final String type = request.params()
                                   .get( PathParam.type.key() );

        final String id = request.params()
                                 .get( TRACKING_ID_PATH_PARAM );

        final StoreType st = StoreType.get( type );

        final TrackingKey tk = new TrackingKey( id, new StoreKey( st, name ) );

        try
        {
            final TrackedContentRecord record = recordManager.getRecord( tk );
            if ( record == null )
            {
                Respond.to( request )
                       .status( ApplicationStatus.NOT_FOUND )
                       .send();
            }
            else
            {
                Respond.to( request )
                       .jsonEntity( record, objectMapper )
                       .ok()
                       .send();
            }
        }
        catch ( final FoloContentException e )
        {
            logger.error( String.format( "Failed to retrieve tracking report for: %s. Reason: %s", tk, e.getMessage() ),
                          e );
            formatResponse( e, request );
        }
        catch ( final JsonProcessingException e )
        {
            logger.error( String.format( "Failed to serialize tracking report for: %s. Reason: %s", tk, e.getMessage() ),
                          e );

            formatResponse( e, request );
        }
    }

    @Routes( { @Route( path = "/report/:type/:name/:id", method = Method.DELETE ) } )
    public void clearReport( final HttpServerRequest request )
    {
        request.pause();

        final String name = request.params()
                                   .get( PathParam.name.key() );

        final String type = request.params()
                                   .get( PathParam.type.key() );

        final String id = request.params()
                                 .get( TRACKING_ID_PATH_PARAM );

        final StoreType st = StoreType.get( type );

        final TrackingKey tk = new TrackingKey( id, new StoreKey( st, name ) );

        recordManager.clearRecord( tk );
        Respond.to( request )
               .status( ApplicationStatus.NO_CONTENT )
               .send();
    }

}
