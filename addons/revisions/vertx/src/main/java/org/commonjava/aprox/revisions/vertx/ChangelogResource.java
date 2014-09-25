package org.commonjava.aprox.revisions.vertx;

import static org.commonjava.aprox.bind.vertx.util.PathParam.name;
import static org.commonjava.aprox.bind.vertx.util.PathParam.type;

import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.revisions.RevisionsManager;
import org.commonjava.aprox.revisions.vertx.dto.ChangeSummaryDTO;
import org.commonjava.aprox.subsys.git.GitSubsystemException;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.util.Query;
import org.commonjava.vertx.vabr.util.Respond;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServerRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Handles( "/revisions/changelog" )
@ApplicationScoped
public class ChangelogResource
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String START_PARAM = "start";

    private static final String COUNT_PARAM = "count";

    private static final int DEFAULT_CHANGELOG_COUNT = 10;

    @Inject
    private RevisionsManager revisions;

    @Inject
    private ObjectMapper objectMapper;

    @Route( "/store/:type/:name" )
    public void getStoreChangelog( final HttpServerRequest request )
    {
        final String t = request.params()
                                .get( type.name() );

        final StoreType storeType = StoreType.get( t );
        if ( storeType == null )
        {
            Respond.to( request )
                   .badRequest( "Invalid store type: '" + t + "'" )
                   .send();
        }

        final String storeName = request.params()
                                        .get( name.name() );
        final StoreKey key = new StoreKey( storeType, storeName );

        final Query query = Query.from( request );
        final int start = query.getInt( START_PARAM, 0 );
        final int count = query.getInt( COUNT_PARAM, DEFAULT_CHANGELOG_COUNT );

        try
        {
            final List<ChangeSummary> dataChangeLog = revisions.getDataChangeLog( key, start, count );
            Respond.to( request )
                   .ok()
                   .jsonEntity( new ChangeSummaryDTO( dataChangeLog ), objectMapper )
                   .send();

            logger.info( "\n\n\n\n\n\n{} Sent changelog for: {}\n\n{}\n\n\n\n\n\n\n", new Date(), key, dataChangeLog );
        }
        catch ( final GitSubsystemException e )
        {
            final String message =
                String.format( "Failed to lookup changelog for: %s. Reason: %s", key, e.getMessage() );
            logger.error( message, e );

            Respond.to( request )
                   .serverError( e, true )
                   .send();
        }
        catch ( final JsonProcessingException e )
        {
            final String message =
                String.format( "Failed to format changelog response for: %s. Reason: %s", key, e.getMessage() );
            logger.error( message, e );

            Respond.to( request )
                   .serverError( e, true )
                   .send();
        }
    }

}
