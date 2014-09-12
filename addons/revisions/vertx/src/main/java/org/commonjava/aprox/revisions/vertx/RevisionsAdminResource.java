package org.commonjava.aprox.revisions.vertx;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.revisions.RevisionsManager;
import org.commonjava.aprox.subsys.git.GitSubsystemException;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.util.Respond;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( "/revisions/admin" )
@ApplicationScoped
public class RevisionsAdminResource
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private RevisionsManager revisionsManager;

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

}
