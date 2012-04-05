package org.commonjava.aprox.flat.data;

import java.util.Collection;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.StoreDataManager;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.ModelFactory;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.flat.conf.FlatFileConfiguration;
import org.commonjava.util.logging.Logger;

@Decorator
public abstract class FlatFileDataManagerDecorator
    implements StoreDataManager
{

    private final Logger logger = new Logger( getClass() );

    @Delegate
    @Any
    @Inject
    private StoreDataManager dataManager;

    @Inject
    private ModelFactory modelFactory;

    @Inject
    private FlatFileConfiguration config;

    @Override
    public void storeDeployPoints( final Collection<? extends DeployPoint> deploys )
        throws ProxyDataException
    {
        final long ts = System.currentTimeMillis();
        dataManager.storeDeployPoints( deploys );
    }

    @Override
    public boolean storeDeployPoint( final DeployPoint deploy )
        throws ProxyDataException
    {
        return dataManager.storeDeployPoint( deploy );
    }

    @Override
    public boolean storeDeployPoint( final DeployPoint deploy, final boolean skipIfExists )
        throws ProxyDataException
    {
        return dataManager.storeDeployPoint( deploy, skipIfExists );
    }

    @Override
    public void storeRepositories( final Collection<? extends Repository> repos )
        throws ProxyDataException
    {
        dataManager.storeRepositories( repos );
    }

    @Override
    public boolean storeRepository( final Repository proxy )
        throws ProxyDataException
    {
        return dataManager.storeRepository( proxy );
    }

    @Override
    public boolean storeRepository( final Repository repository, final boolean skipIfExists )
        throws ProxyDataException
    {
        return dataManager.storeRepository( repository, skipIfExists );
    }

    @Override
    public void storeGroups( final Collection<? extends Group> groups )
        throws ProxyDataException
    {
        dataManager.storeGroups( groups );
    }

    @Override
    public boolean storeGroup( final Group group )
        throws ProxyDataException
    {
        return dataManager.storeGroup( group );
    }

    @Override
    public boolean storeGroup( final Group group, final boolean skipIfExists )
        throws ProxyDataException
    {
        return dataManager.storeGroup( group, skipIfExists );
    }

    @Override
    public void deleteDeployPoint( final DeployPoint deploy )
        throws ProxyDataException
    {
        dataManager.deleteDeployPoint( deploy );
    }

    @Override
    public void deleteDeployPoint( final String name )
        throws ProxyDataException
    {
        dataManager.deleteDeployPoint( name );
    }

    @Override
    public void deleteRepository( final Repository repo )
        throws ProxyDataException
    {
        dataManager.deleteRepository( repo );
    }

    @Override
    public void deleteRepository( final String name )
        throws ProxyDataException
    {
        dataManager.deleteRepository( name );
    }

    @Override
    public void deleteGroup( final Group group )
        throws ProxyDataException
    {
        dataManager.deleteGroup( group );
    }

    @Override
    public void deleteGroup( final String name )
        throws ProxyDataException
    {
        dataManager.deleteGroup( name );
    }

}
