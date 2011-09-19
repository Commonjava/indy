package org.commonjava.web.maven.proxy.change;

import static org.commonjava.couch.util.IdUtils.nonNamespaceId;

import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.auth.couch.data.UserDataException;
import org.commonjava.auth.couch.data.UserDataManager;
import org.commonjava.auth.couch.model.Permission;
import org.commonjava.couch.change.CouchDocChange;
import org.commonjava.couch.change.dispatch.CouchChangeJ2EEEvent;
import org.commonjava.couch.change.dispatch.ThreadableListener;
import org.commonjava.couch.util.ChangeSynchronizer;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.maven.proxy.data.ProxyDataException;
import org.commonjava.web.maven.proxy.data.ProxyDataManager;
import org.commonjava.web.maven.proxy.model.Group;
import org.commonjava.web.maven.proxy.model.Repository;

@Singleton
public class RepositoryDeletionListener
    implements ThreadableListener
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private ProxyDataManager proxyDataManager;

    @Inject
    private UserDataManager userDataManager;

    private final ChangeSynchronizer changeSync = new ChangeSynchronizer();

    @Override
    public boolean canProcess( final String id, final boolean deleted )
    {
        return deleted && id.startsWith( Repository.NAMESPACE );
    }

    @Override
    public void documentChanged( final CouchDocChange change )
    {
        String repo = nonNamespaceId( Repository.NAMESPACE, change.getId() );
        try
        {
            Set<Group> groups = proxyDataManager.getGroupsForRepository( repo );
            for ( Group group : groups )
            {
                group.removeConstituent( repo );
            }

            proxyDataManager.storeGroups( groups );

            userDataManager.deletePermission( Permission.name( change.getId(), Permission.ADMIN ) );
            userDataManager.deletePermission( Permission.name( change.getId(), Permission.READ ) );

            changeSync.setChanged();
        }
        catch ( ProxyDataException e )
        {
            logger.error( "Failed to remove group constituent listings for repository: %s. Error: %s",
                          e, repo, e.getMessage() );
        }
        catch ( UserDataException e )
        {
            logger.error( "Failed to remove permissions for deleted repository: %s. Error: %s", e,
                          repo, e.getMessage() );
        }
    }

    public void repositoryDeleted( @Observes final CouchChangeJ2EEEvent event )
    {
        CouchDocChange change = event.getChange();
        if ( canProcess( change.getId(), change.isDeleted() ) )
        {
            documentChanged( change );
        }
    }

    @Override
    public void waitForChange( final long totalMillis, final long pollingMillis )
    {
        changeSync.waitForChange( totalMillis, pollingMillis );
    }

}
