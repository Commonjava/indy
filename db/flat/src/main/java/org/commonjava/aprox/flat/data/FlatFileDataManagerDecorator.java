package org.commonjava.aprox.flat.data;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.flat.conf.FlatFileConfiguration;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.JsonSerializer;

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
    private FlatFileConfiguration config;

    @Inject
    @AproxData
    private JsonSerializer serializer;

    protected FlatFileDataManagerDecorator()
    {
    }

    protected FlatFileDataManagerDecorator( final StoreDataManager dataManager, final FlatFileConfiguration config,
                                            final JsonSerializer serializer )
    {
        this.dataManager = dataManager;
        this.config = config;
        this.serializer = serializer;
    }

    protected final StoreDataManager getDataManager()
    {
        return dataManager;
    }

    @SuppressWarnings( "unchecked" )
    @PostConstruct
    public void readDefinitions()
        throws ProxyDataException
    {
        final File basedir = config.getDefinitionsDir();
        final File ddir = new File( basedir, StoreType.deploy_point.name() );

        String[] files = ddir.list();
        for ( final String file : files )
        {
            final File f = new File( ddir, file );
            try
            {
                final String json = FileUtils.readFileToString( f );
                final DeployPoint dp = serializer.fromString( json, DeployPoint.class );
                dataManager.storeDeployPoint( dp );
            }
            catch ( final IOException e )
            {
                throw new ProxyDataException( "Cannot read definition file: %s. Error: %s", e, f, e.getMessage() );
            }
        }

        final File rdir = new File( basedir, StoreType.repository.name() );
        files = rdir.list();
        for ( final String file : files )
        {
            final File f = new File( ddir, file );
            try
            {
                final String json = FileUtils.readFileToString( f );
                final Repository r = serializer.fromString( json, Repository.class );
                dataManager.storeRepository( r );
            }
            catch ( final IOException e )
            {
                throw new ProxyDataException( "Cannot read definition file: %s. Error: %s", e, f, e.getMessage() );
            }
        }

        final File gdir = new File( basedir, StoreType.group.name() );
        files = gdir.list();
        for ( final String file : files )
        {
            final File f = new File( ddir, file );
            try
            {
                final String json = FileUtils.readFileToString( f );
                final Group g = serializer.fromString( json, Group.class );
                dataManager.storeGroup( g );
            }
            catch ( final IOException e )
            {
                throw new ProxyDataException( "Cannot read definition file: %s. Error: %s", e, f, e.getMessage() );
            }
        }
    }

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

            logger.info( "Writing definition file: %s for store: %s", f, store );
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
                logger.info( "Deleting definition file: %s for store: %s", f, store );
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
            logger.info( "Deleting definition file: %s for store: %s", f, new StoreKey( type, name ) );
            f.delete();
        }
    }

}
