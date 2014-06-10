package org.commonjava.aprox.autoprox.fixture;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.commonjava.aprox.autoprox.data.AutoProxDataManagerDecorator;
import org.commonjava.aprox.autoprox.model.AutoProxCatalog;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.mem.data.MemoryStoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.subsys.http.AproxHttpProvider;

public class TestAutoProxyDataManager
    extends AutoProxDataManagerDecorator
    implements StoreDataManager
{

    private final StoreDataManager delegate;

    public TestAutoProxyDataManager( final AutoProxCatalog catalog, final AproxHttpProvider http )
    {
        super( new MemoryStoreDataManager(), catalog, http );
        delegate = getDelegate();
    }

    @Override
    public List<ArtifactStore> getAllArtifactStores()
        throws ProxyDataException
    {
        return delegate.getAllArtifactStores();
    }

    @Override
    public List<? extends ArtifactStore> getAllArtifactStores( final StoreType type )
        throws ProxyDataException
    {
        return delegate.getAllArtifactStores( type );
    }

    @Override
    public List<Group> getAllGroups()
        throws ProxyDataException
    {
        return delegate.getAllGroups();
    }

    @Override
    public List<RemoteRepository> getAllRemoteRepositories()
        throws ProxyDataException
    {
        return delegate.getAllRemoteRepositories();
    }

    @Override
    public List<HostedRepository> getAllHostedRepositories()
        throws ProxyDataException
    {
        return delegate.getAllHostedRepositories();
    }

    @Override
    public List<ArtifactStore> getAllConcreteArtifactStores()
        throws ProxyDataException
    {
        return delegate.getAllConcreteArtifactStores();
    }

    @Override
    public List<ArtifactStore> getOrderedConcreteStoresInGroup( final String groupName )
        throws ProxyDataException
    {
        return delegate.getOrderedConcreteStoresInGroup( groupName );
    }

    @Override
    public List<ArtifactStore> getOrderedStoresInGroup( final String groupName )
        throws ProxyDataException
    {
        return delegate.getOrderedStoresInGroup( groupName );
    }

    @Override
    public Set<Group> getGroupsContaining( final StoreKey repo )
        throws ProxyDataException
    {
        return delegate.getGroupsContaining( repo );
    }

    @Override
    public void storeHostedRepositories( final Collection<HostedRepository> deploys )
        throws ProxyDataException
    {
        delegate.storeHostedRepositories( deploys );
    }

    @Override
    public boolean storeHostedRepository( final HostedRepository deploy )
        throws ProxyDataException
    {
        return delegate.storeHostedRepository( deploy );
    }

    @Override
    public boolean storeHostedRepository( final HostedRepository deploy, final boolean skipIfExists )
        throws ProxyDataException
    {
        return delegate.storeHostedRepository( deploy, skipIfExists );
    }

    @Override
    public void storeRemoteRepositories( final Collection<RemoteRepository> repos )
        throws ProxyDataException
    {
        delegate.storeRemoteRepositories( repos );
    }

    @Override
    public boolean storeRemoteRepository( final RemoteRepository proxy )
        throws ProxyDataException
    {
        return delegate.storeRemoteRepository( proxy );
    }

    @Override
    public boolean storeRemoteRepository( final RemoteRepository repository, final boolean skipIfExists )
        throws ProxyDataException
    {
        return delegate.storeRemoteRepository( repository, skipIfExists );
    }

    @Override
    public void storeGroups( final Collection<Group> groups )
        throws ProxyDataException
    {
        delegate.storeGroups( groups );
    }

    @Override
    public boolean storeGroup( final Group group )
        throws ProxyDataException
    {
        return delegate.storeGroup( group );
    }

    @Override
    public boolean storeGroup( final Group group, final boolean skipIfExists )
        throws ProxyDataException
    {
        return delegate.storeGroup( group, skipIfExists );
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore key )
        throws ProxyDataException
    {
        return delegate.storeArtifactStore( key );
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore key, final boolean skipIfExists )
        throws ProxyDataException
    {
        return delegate.storeArtifactStore( key, skipIfExists );
    }

    @Override
    public void deleteHostedRepository( final HostedRepository deploy )
        throws ProxyDataException
    {
        delegate.deleteHostedRepository( deploy );
    }

    @Override
    public void deleteHostedRepository( final String name )
        throws ProxyDataException
    {
        delegate.deleteHostedRepository( name );
    }

    @Override
    public void deleteRemoteRepository( final RemoteRepository repo )
        throws ProxyDataException
    {
        delegate.deleteRemoteRepository( repo );
    }

    @Override
    public void deleteRemoteRepository( final String name )
        throws ProxyDataException
    {
        delegate.deleteRemoteRepository( name );
    }

    @Override
    public void deleteGroup( final Group group )
        throws ProxyDataException
    {
        delegate.deleteGroup( group );
    }

    @Override
    public void deleteGroup( final String name )
        throws ProxyDataException
    {
        delegate.deleteGroup( name );
    }

    @Override
    public void deleteArtifactStore( final StoreKey key )
        throws ProxyDataException
    {
        delegate.deleteArtifactStore( key );
    }

    @Override
    public void clear()
        throws ProxyDataException
    {
        delegate.clear();
    }

    @Override
    public void install()
        throws ProxyDataException
    {
        delegate.install();
    }

    @Override
    public void reload()
        throws ProxyDataException
    {
        delegate.reload();
    }

}
