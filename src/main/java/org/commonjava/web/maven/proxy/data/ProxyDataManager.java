package org.commonjava.web.maven.proxy.data;

import static org.commonjava.couch.util.IdUtils.namespaceId;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.auth.couch.data.UserDataException;
import org.commonjava.auth.couch.data.UserDataManager;
import org.commonjava.auth.couch.model.Permission;
import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.couch.db.CouchDBException;
import org.commonjava.couch.db.CouchManager;
import org.commonjava.couch.db.model.ViewRequest;
import org.commonjava.couch.model.CouchDocRef;
import org.commonjava.couch.model.DenormalizationException;
import org.commonjava.couch.util.JoinString;
import org.commonjava.web.maven.proxy.change.event.GroupUpdateEvent;
import org.commonjava.web.maven.proxy.change.event.ProxyManagerDeleteEvent;
import org.commonjava.web.maven.proxy.change.event.ProxyManagerUpdateType;
import org.commonjava.web.maven.proxy.change.event.RepositoryUpdateEvent;
import org.commonjava.web.maven.proxy.conf.ProxyConfiguration;
import org.commonjava.web.maven.proxy.data.ProxyAppDescription.View;
import org.commonjava.web.maven.proxy.model.Group;
import org.commonjava.web.maven.proxy.model.Repository;

@Singleton
public class ProxyDataManager
{

    @Inject
    private UserDataManager userMgr;

    @Inject
    private CouchManager couch;

    @Inject
    private ProxyConfiguration config;

    @Inject
    private CouchDBConfiguration couchConfig;

    @Inject
    private Event<RepositoryUpdateEvent> repoEvent;

    @Inject
    private Event<GroupUpdateEvent> groupEvent;

    @Inject
    private Event<ProxyManagerDeleteEvent> delEvent;

    public ProxyDataManager()
    {}

    public ProxyDataManager( final ProxyConfiguration config, final UserDataManager userMgr,
                             final CouchDBConfiguration couchConfig, final CouchManager couch )
    {
        this.config = config;
        this.userMgr = userMgr;
        this.couchConfig = couchConfig;
        this.couch = couch;
    }

    public Repository getRepository( final String name )
        throws ProxyDataException
    {
        try
        {
            return couch.getDocument( new CouchDocRef( namespaceId( Repository.NAMESPACE, name ) ),
                                      Repository.class );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to retrieve proxy: %s. Reason: %s", e, name,
                                          e.getMessage() );
        }
    }

    public Group getGroup( final String name )
        throws ProxyDataException
    {
        try
        {
            return couch.getDocument( new CouchDocRef( namespaceId( Group.NAMESPACE, name ) ),
                                      Group.class );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to retrieve proxy-group: %s. Reason: %s", e,
                                          name, e.getMessage() );
        }
    }

    public List<Group> getAllGroups()
        throws ProxyDataException
    {
        try
        {
            return couch.getViewListing( new ProxyViewRequest( config, View.ALL_GROUPS ),
                                         Group.class );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to retrieve proxy-group listing. Reason: %s", e,
                                          e.getMessage() );
        }
    }

    public List<Repository> getAllRepositories()
        throws ProxyDataException
    {
        try
        {
            return couch.getViewListing( new ProxyViewRequest( config, View.ALL_REPOSITORIES ),
                                         Repository.class );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to retrieve proxy listing. Reason: %s", e,
                                          e.getMessage() );
        }
    }

    public List<Repository> getRepositoriesForGroup( final String groupName )
        throws ProxyDataException
    {
        try
        {
            return couch.getViewListing( new ProxyViewRequest( config, View.GROUP_REPOSITORIES,
                                                               groupName ), Repository.class );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to retrieve proxies in group: %s. Reason: %s", e,
                                          groupName, e.getMessage() );
        }
    }

    public Set<Group> getGroupsForRepository( final String repo )
        throws ProxyDataException
    {
        try
        {
            ProxyViewRequest req = new ProxyViewRequest( config, View.REPOSITORY_GROUPS );
            req.setParameter( ViewRequest.KEY, repo );

            List<Group> groups = couch.getViewListing( req, Group.class );

            return new HashSet<Group>( groups );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException(
                                          "Failed to lookup groups containing repository: %s. Reason: %s",
                                          e, repo, e.getMessage() );
        }
    }

    public void storeRepositories( final Collection<Repository> repos )
        throws ProxyDataException
    {
        try
        {
            couch.store( repos, false, false );
            fireRepositoryEvent( ProxyManagerUpdateType.ADD_OR_UPDATE, repos );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to update %d repositories. Reason: %s", e,
                                          repos.size(), e.getMessage() );
        }
    }

    public boolean storeRepository( final Repository proxy )
        throws ProxyDataException
    {
        return storeRepository( proxy, false );
    }

    public boolean storeRepository( final Repository repository, final boolean skipIfExists )
        throws ProxyDataException
    {
        try
        {
            repository.calculateDenormalizedFields();
            boolean result = couch.store( repository, skipIfExists );

            fireRepositoryEvent( skipIfExists ? ProxyManagerUpdateType.ADD
                            : ProxyManagerUpdateType.ADD_OR_UPDATE, repository );

            userMgr.createPermissions( Repository.NAMESPACE, repository.getName(),
                                       Permission.ADMIN, Permission.READ );

            return result;
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException(
                                          "Failed to store repository configuration: %s. Reason: %s",
                                          e, repository.getName(), e.getMessage() );
        }
        catch ( DenormalizationException e )
        {
            throw new ProxyDataException(
                                          "Failed to store repository configuration: %s. Reason: %s",
                                          e, repository.getName(), e.getMessage() );
        }
        catch ( UserDataException e )
        {
            throw new ProxyDataException(
                                          "Failed to create permissions for repository: %s. Reason: %s",
                                          e, repository.getName(), e.getMessage() );
        }
    }

    public void storeGroups( final Collection<Group> groups )
        throws ProxyDataException
    {
        try
        {
            couch.store( groups, false, false );
            fireGroupEvent( ProxyManagerUpdateType.ADD_OR_UPDATE, groups );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to update %d repository groups. Reason: %s", e,
                                          groups.size(), e.getMessage() );
        }
    }

    public boolean storeGroup( final Group group )
        throws ProxyDataException
    {
        return storeGroup( group, false );
    }

    public boolean storeGroup( final Group group, final boolean skipIfExists )
        throws ProxyDataException
    {
        try
        {
            group.calculateDenormalizedFields();

            Set<String> missing = new HashSet<String>();
            for ( String repoName : group.getConstituents() )
            {
                if ( !couch.exists( new CouchDocRef( namespaceId( Repository.NAMESPACE, repoName ) ) ) )
                {
                    missing.add( repoName );
                }
            }

            if ( !missing.isEmpty() )
            {
                throw new ProxyDataException(
                                              "Invalid repository-group configuration: %s. Reason: One or more constituent repositories are missing: %s",
                                              group.getName(), new JoinString( ", ", missing ) );
            }

            boolean result = couch.store( group, skipIfExists );

            fireGroupEvent( skipIfExists ? ProxyManagerUpdateType.ADD
                            : ProxyManagerUpdateType.ADD_OR_UPDATE, group );

            userMgr.createPermissions( Group.NAMESPACE, group.getName(), Permission.ADMIN,
                                       Permission.READ );

            return result;
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException(
                                          "Failed to store proxy-group configuration: %s. Reason: %s",
                                          e, group.getName(), e.getMessage() );
        }
        catch ( DenormalizationException e )
        {
            throw new ProxyDataException( "Failed to store group configuration: %s. Reason: %s", e,
                                          group.getName(), e.getMessage() );
        }
        catch ( UserDataException e )
        {
            throw new ProxyDataException( "Failed to create permissions for group: %s. Reason: %s",
                                          e, group.getName(), e.getMessage() );
        }
    }

    public void deleteRepository( final Repository repo )
        throws ProxyDataException
    {
        try
        {
            couch.delete( repo );
            fireDeleteEvent( ProxyManagerDeleteEvent.Type.REPOSITORY, repo.getName() );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to delete proxy configuration: %s. Reason: %s",
                                          e, repo.getName(), e.getMessage() );
        }
    }

    public void deleteRepository( final String name )
        throws ProxyDataException
    {
        try
        {
            couch.delete( new CouchDocRef( namespaceId( Repository.NAMESPACE, name ) ) );
            fireDeleteEvent( ProxyManagerDeleteEvent.Type.REPOSITORY, name );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException( "Failed to delete proxy configuration: %s. Reason: %s",
                                          e, name, e.getMessage() );
        }
    }

    public void deleteGroup( final Group group )
        throws ProxyDataException
    {
        try
        {
            couch.delete( group );
            fireDeleteEvent( ProxyManagerDeleteEvent.Type.GROUP, group.getName() );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException(
                                          "Failed to delete proxy-group configuration: %s. Reason: %s",
                                          e, group.getName(), e.getMessage() );
        }
    }

    public void deleteGroup( final String name )
        throws ProxyDataException
    {
        try
        {
            couch.delete( new CouchDocRef( namespaceId( Group.NAMESPACE, name ) ) );
            fireDeleteEvent( ProxyManagerDeleteEvent.Type.GROUP, name );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException(
                                          "Failed to delete proxy-group configuration: %s. Reason: %s",
                                          e, name, e.getMessage() );
        }
    }

    public void install()
        throws ProxyDataException
    {
        ProxyAppDescription description = new ProxyAppDescription();

        try
        {
            couch.initialize( description );

            userMgr.install();
            userMgr.setupAdminInformation();

            userMgr.storePermission( new Permission( Repository.NAMESPACE, Permission.ADMIN ) );
            userMgr.storePermission( new Permission( Group.NAMESPACE, Permission.ADMIN ) );
            userMgr.storePermission( new Permission( Repository.NAMESPACE, Permission.READ ) );
            userMgr.storePermission( new Permission( Group.NAMESPACE, Permission.READ ) );
        }
        catch ( CouchDBException e )
        {
            throw new ProxyDataException(
                                          "Failed to initialize proxy-management database: %s (application: %s). Reason: %s",
                                          e, couchConfig.getDatabaseUrl(),
                                          description.getAppName(), e.getMessage() );
        }
        catch ( UserDataException e )
        {
            throw new ProxyDataException(
                                          "Failed to initialize admin user/privilege information in proxy-management database: %s. Reason: %s",
                                          e, couchConfig.getDatabaseUrl(), e.getMessage() );
        }
    }

    private void fireDeleteEvent( final ProxyManagerDeleteEvent.Type type, final String... names )
    {
        if ( delEvent != null )
        {
            delEvent.fire( new ProxyManagerDeleteEvent( type, names ) );
        }
    }

    private void fireRepositoryEvent( final ProxyManagerUpdateType type, final Repository... repos )
    {
        if ( repoEvent != null )
        {
            repoEvent.fire( new RepositoryUpdateEvent( type, repos ) );
        }
    }

    private void fireGroupEvent( final ProxyManagerUpdateType type, final Group... groups )
    {
        if ( groupEvent != null )
        {
            groupEvent.fire( new GroupUpdateEvent( type, groups ) );
        }
    }

    @SuppressWarnings( "unused" )
    private void fireDeleteEvent( final ProxyManagerDeleteEvent.Type type,
                                  final Collection<String> names )
    {
        if ( delEvent != null )
        {
            delEvent.fire( new ProxyManagerDeleteEvent( type, names ) );
        }
    }

    private void fireRepositoryEvent( final ProxyManagerUpdateType type,
                                      final Collection<Repository> repos )
    {
        if ( repoEvent != null )
        {
            repoEvent.fire( new RepositoryUpdateEvent( type, repos ) );
        }
    }

    private void fireGroupEvent( final ProxyManagerUpdateType type, final Collection<Group> groups )
    {
        if ( groupEvent != null )
        {
            groupEvent.fire( new GroupUpdateEvent( type, groups ) );
        }
    }
}
