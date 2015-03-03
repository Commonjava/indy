package org.commonjava.aprox.autoprox.fixture;

import java.util.List;
import java.util.Set;

import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.autoprox.data.AutoProxCatalogManager;
import org.commonjava.aprox.autoprox.data.AutoProxDataManagerDecorator;
import org.commonjava.aprox.core.data.DefaultStoreEventDispatcher;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.mem.data.MemoryStoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.subsys.http.AproxHttpProvider;

public class TestAutoProxyDataManager
    extends AutoProxDataManagerDecorator
    implements StoreDataManager
{

    private final StoreDataManager delegate;

    public TestAutoProxyDataManager( final AutoProxCatalogManager catalog, final AproxHttpProvider http )
    {
        super( new MemoryStoreDataManager( new DefaultStoreEventDispatcher() ), catalog, http );
        delegate = getDelegate();
    }

    @Override
    public List<ArtifactStore> getAllArtifactStores()
        throws AproxDataException
    {
        return delegate.getAllArtifactStores();
    }

    @Override
    public List<? extends ArtifactStore> getAllArtifactStores( final StoreType type )
        throws AproxDataException
    {
        return delegate.getAllArtifactStores( type );
    }

    @Override
    public List<Group> getAllGroups()
        throws AproxDataException
    {
        return delegate.getAllGroups();
    }

    @Override
    public List<RemoteRepository> getAllRemoteRepositories()
        throws AproxDataException
    {
        return delegate.getAllRemoteRepositories();
    }

    @Override
    public List<HostedRepository> getAllHostedRepositories()
        throws AproxDataException
    {
        return delegate.getAllHostedRepositories();
    }

    @Override
    public List<ArtifactStore> getAllConcreteArtifactStores()
        throws AproxDataException
    {
        return delegate.getAllConcreteArtifactStores();
    }

    @Override
    public List<ArtifactStore> getOrderedConcreteStoresInGroup( final String groupName )
        throws AproxDataException
    {
        return delegate.getOrderedConcreteStoresInGroup( groupName );
    }

    @Override
    public List<ArtifactStore> getOrderedStoresInGroup( final String groupName )
        throws AproxDataException
    {
        return delegate.getOrderedStoresInGroup( groupName );
    }

    @Override
    public Set<Group> getGroupsContaining( final StoreKey repo )
        throws AproxDataException
    {
        return delegate.getGroupsContaining( repo );
    }

    @Override
    public void install()
        throws AproxDataException
    {
        delegate.install();
    }

    @Override
    public void reload()
        throws AproxDataException
    {
        delegate.reload();
    }

    @Override
    public boolean storeHostedRepository( final HostedRepository deploy, final ChangeSummary summary )
        throws AproxDataException
    {
        return delegate.storeHostedRepository( deploy, summary );
    }

    @Override
    public boolean storeHostedRepository( final HostedRepository deploy, final ChangeSummary summary,
                                          final boolean skipIfExists )
        throws AproxDataException
    {
        return delegate.storeHostedRepository( deploy, summary, skipIfExists );
    }

    @Override
    public boolean storeRemoteRepository( final RemoteRepository proxy, final ChangeSummary summary )
        throws AproxDataException
    {
        return delegate.storeRemoteRepository( proxy, summary );
    }

    @Override
    public boolean storeRemoteRepository( final RemoteRepository repository, final ChangeSummary summary,
                                          final boolean skipIfExists )
        throws AproxDataException
    {
        return delegate.storeRemoteRepository( repository, summary, skipIfExists );
    }

    @Override
    public boolean storeGroup( final Group group, final ChangeSummary summary )
        throws AproxDataException
    {
        return delegate.storeGroup( group, summary );
    }

    @Override
    public boolean storeGroup( final Group group, final ChangeSummary summary, final boolean skipIfExists )
        throws AproxDataException
    {
        return delegate.storeGroup( group, summary, skipIfExists );
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore key, final ChangeSummary summary )
        throws AproxDataException
    {
        return delegate.storeArtifactStore( key, summary );
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore key, final ChangeSummary summary, final boolean skipIfExists )
        throws AproxDataException
    {
        return delegate.storeArtifactStore( key, summary, skipIfExists );
    }

    @Override
    public void deleteHostedRepository( final HostedRepository deploy, final ChangeSummary summary )
        throws AproxDataException
    {
        delegate.deleteHostedRepository( deploy, summary );
    }

    @Override
    public void deleteHostedRepository( final String name, final ChangeSummary summary )
        throws AproxDataException
    {
        delegate.deleteHostedRepository( name, summary );
    }

    @Override
    public void deleteRemoteRepository( final RemoteRepository repo, final ChangeSummary summary )
        throws AproxDataException
    {
        delegate.deleteRemoteRepository( repo, summary );
    }

    @Override
    public void deleteRemoteRepository( final String name, final ChangeSummary summary )
        throws AproxDataException
    {
        delegate.deleteRemoteRepository( name, summary );
    }

    @Override
    public void deleteGroup( final Group group, final ChangeSummary summary )
        throws AproxDataException
    {
        delegate.deleteGroup( group, summary );
    }

    @Override
    public void deleteGroup( final String name, final ChangeSummary summary )
        throws AproxDataException
    {
        delegate.deleteGroup( name, summary );
    }

    @Override
    public void deleteArtifactStore( final StoreKey key, final ChangeSummary summary )
        throws AproxDataException
    {
        delegate.deleteArtifactStore( key, summary );
    }

    @Override
    public void clear( final ChangeSummary summary )
        throws AproxDataException
    {
        delegate.clear( summary );
    }

    @Override
    public boolean hasRemoteRepository( final String name )
    {
        return delegate.hasRemoteRepository( name );
    }

    @Override
    public boolean hasGroup( final String name )
    {
        return delegate.hasGroup( name );
    }

    @Override
    public boolean hasHostedRepository( final String name )
    {
        return delegate.hasHostedRepository( name );
    }

    @Override
    public boolean hasArtifactStore( final StoreKey key )
    {
        return delegate.hasArtifactStore( key );
    }

}
