package org.commonjava.aprox.flat.data;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.StoreDataManager;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.aprox.flat.conf.FlatFileConfiguration;
import org.commonjava.web.json.ser.JsonSerializer;

@Decorator
public abstract class FlatFileDataManagerDecorator
    implements StoreDataManager
{

    @Delegate
    @Any
    @Inject
    private StoreDataManager dataManager;

    @Inject
    private FlatFileConfiguration config;

    @Inject
    private JsonSerializer serializer;

    @Override
    public void storeDeployPoints( final Collection<DeployPoint> deploys )
        throws ProxyDataException
    {
        dataManager.storeDeployPoints( deploys );
        store( false, deploys.toArray( new ArtifactStore[] {} ) );
    }

    @Override
    public boolean storeDeployPoint( final DeployPoint deploy )
        throws ProxyDataException
    {
        final boolean result = dataManager.storeDeployPoint( deploy );
        store( false, deploy );

        return result;
    }

    @Override
    public boolean storeDeployPoint( final DeployPoint deploy, final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = dataManager.storeDeployPoint( deploy, skipIfExists );
        store( skipIfExists, deploy );

        return result;
    }

    @Override
    public void storeRepositories( final Collection<Repository> repos )
        throws ProxyDataException
    {
        dataManager.storeRepositories( repos );
        store( false, repos.toArray( new ArtifactStore[] {} ) );
    }

    @Override
    public boolean storeRepository( final Repository proxy )
        throws ProxyDataException
    {
        final boolean result = dataManager.storeRepository( proxy );
        store( false, proxy );

        return result;
    }

    @Override
    public boolean storeRepository( final Repository repository, final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = dataManager.storeRepository( repository, skipIfExists );
        store( skipIfExists, repository );

        return result;
    }

    @Override
    public void storeGroups( final Collection<Group> groups )
        throws ProxyDataException
    {
        dataManager.storeGroups( groups );
        store( false, groups.toArray( new ArtifactStore[] {} ) );
    }

    @Override
    public boolean storeGroup( final Group group )
        throws ProxyDataException
    {
        final boolean result = dataManager.storeGroup( group );
        store( false, group );

        return result;
    }

    @Override
    public boolean storeGroup( final Group group, final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = dataManager.storeGroup( group, skipIfExists );
        store( false, group );

        return result;
    }

    @Override
    public void deleteDeployPoint( final DeployPoint deploy )
        throws ProxyDataException
    {
        dataManager.deleteDeployPoint( deploy );
        delete( deploy );
    }

    @Override
    public void deleteDeployPoint( final String name )
        throws ProxyDataException
    {
        dataManager.deleteDeployPoint( name );
        delete( StoreType.deploy_point, name );
    }

    @Override
    public void deleteRepository( final Repository repo )
        throws ProxyDataException
    {
        dataManager.deleteRepository( repo );
        delete( repo );
    }

    @Override
    public void deleteRepository( final String name )
        throws ProxyDataException
    {
        dataManager.deleteRepository( name );
        delete( StoreType.repository, name );
    }

    @Override
    public void deleteGroup( final Group group )
        throws ProxyDataException
    {
        dataManager.deleteGroup( group );
        delete( group );
    }

    @Override
    public void deleteGroup( final String name )
        throws ProxyDataException
    {
        dataManager.deleteGroup( name );
        delete( StoreType.group, name );
    }

    private void store( final boolean skipIfExists, final ArtifactStore... stores )
        throws ProxyDataException
    {
        final File basedir = config.getDefinitionsDir();
        for ( final ArtifactStore store : stores )
        {
            final File dir = new File( basedir, store.getDoctype()
                                                     .name() );
            if ( !dir.isDirectory() && !dir.mkdirs() )
            {
                throw new ProxyDataException( "Cannot create storage directory: %s for definition: %s", dir, store );
            }

            final File f = new File( dir, store.getName() + ".json" );
            if ( skipIfExists && f.exists() )
            {
                continue;
            }

            try
            {
                FileUtils.write( f, serializer.toString( store ), "UTF-8" );
            }
            catch ( final IOException e )
            {
                throw new ProxyDataException( "Cannot write definition: %s to: %s. Reason: %s", e, store, f,
                                              e.getMessage() );
            }
        }
    }

    private void delete( final ArtifactStore... stores )
    {
        final File basedir = config.getDefinitionsDir();
        for ( final ArtifactStore store : stores )
        {
            final File dir = new File( basedir, store.getDoctype()
                                                     .name() );

            final File f = new File( dir, store.getName() + ".json" );
            if ( f.exists() )
            {
                f.delete();
            }

        }
    }

    private void delete( final StoreType type, final String name )
    {
        final File basedir = config.getDefinitionsDir();
        final File dir = new File( basedir, type.name() );

        final File f = new File( dir, name + ".json" );
        if ( f.exists() )
        {
            f.delete();
        }
    }

}
