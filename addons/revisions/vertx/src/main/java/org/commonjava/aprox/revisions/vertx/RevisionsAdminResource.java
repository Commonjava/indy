package org.commonjava.aprox.revisions.vertx;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.bind.vertx.util.PathParam;
import org.commonjava.aprox.revisions.RevisionsManager;
import org.commonjava.aprox.revisions.vertx.dto.ChangeSummaryDTO;
import org.commonjava.aprox.subsys.git.GitSubsystemException;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.vertx.vabr.util.Query;
import org.commonjava.vertx.vabr.util.Respond;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServerRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Handles( "/admin/revisions" )
@ApplicationScoped
public class RevisionsAdminResource
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private RevisionsManager revisionsManager;

    @Inject
    private ObjectMapper objectMapper;

    @Route( "/data/pull" )
    public void pullDataGitUpdates( final HttpServerRequest request )
    {
        try
        {
            revisionsManager.pullDataUpdates();

            // FIXME: Return some status
            Respond.to( request )
                   .ok()
                   .send();
        }
        catch ( final GitSubsystemException e )
        {
            logger.error( "Failed to pull git updates for data dir: " + e.getMessage(), e );
            Respond.to( request )
                   .serverError( e, "Failed to pull git updates for data dir.", true )
                   .send();
        }
    }

    @Route( "/data/push" )
    public void pushDataGitUpdates( final HttpServerRequest request )
    {
        try
        {
            revisionsManager.pushDataUpdates();

            // FIXME: Return some status
            Respond.to( request )
                   .ok()
                   .send();
        }
        catch ( final GitSubsystemException e )
        {
            logger.error( "Failed to push git updates for data dir: " + e.getMessage(), e );
            Respond.to( request )
                   .serverError( e, "Failed to push git updates for data dir.", true )
                   .send();
        }
    }

    @Routes( { @Route( path = "/data/changelog:path=(/.*)", method = Method.GET ) } )
    public void doGet( final HttpServerRequest request )
    {
        final String path = request.params()
                                   .get( PathParam.path.key() );

        final Query query = Query.from( request );
        final int start = query.getInt( "start", 0 );
        final int count = query.getInt( "count", 25 );

        try
        {
            final List<ChangeSummary> listing = revisionsManager.getDataChangeLog( path, start, count );

            Respond.to( request )
                   .ok()
                   .jsonEntity( new ChangeSummaryDTO( listing ), objectMapper )
                   .send();
        }
        catch ( final GitSubsystemException e )
        {
            logger.error( "Failed to read git changelog from data dir: " + e.getMessage(), e );
            Respond.to( request )
                   .serverError( e, "Failed to read git changelog from data dir.", true )
                   .send();
        }
        catch ( final JsonProcessingException e )
        {
            logger.error( "Failed to serialize changelog: " + e.getMessage(), e );
            Respond.to( request )
                   .serverError( e, "Failed to serialize changelog.", true )
                   .send();
        }
    }

}
