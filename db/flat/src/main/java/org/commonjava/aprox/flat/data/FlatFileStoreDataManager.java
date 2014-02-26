package org.commonjava.aprox.flat.data;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.mem.data.MemoryStoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.subsys.flatfile.conf.FlatFileConfiguration;
import org.commonjava.web.json.ser.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Alternative
public class FlatFileStoreDataManager
    extends MemoryStoreDataManager
{
    private static final String APROX_STORE = "aprox";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private FlatFileConfiguration config;

    @Inject
    @AproxData
    private JsonSerializer serializer;

    protected FlatFileStoreDataManager()
    {
    }

    protected FlatFileStoreDataManager( final FlatFileConfiguration config, final JsonSerializer serializer )
    {
        this.config = config;
        this.serializer = serializer;
    }

    @PostConstruct
    public void readDefinitions()
        throws ProxyDataException
    {
        final File basedir = config.getStorageDir( APROX_STORE );
        final File ddir = new File( basedir, StoreType.deploy_point.name() );

        final String[] dFiles = ddir.list();
        if ( dFiles != null )
        {
            for ( final String file : dFiles )
            {
                final File f = new File( ddir, file );
                try
                {
                    final String json = FileUtils.readFileToString( f );
                    final DeployPoint dp = serializer.fromString( json, DeployPoint.class );
                    if ( dp == null )
                    {
                        f.delete();
                    }
                    else
                    {
                        storeDeployPoint( dp );
                    }
                }
                catch ( final IOException e )
                {
                    logger.error( "Failed to load deploy point: {}. Reason: {}", e, f, e.getMessage() );
                }
            }
        }

        final File rdir = new File( basedir, StoreType.repository.name() );
        final String[] rFiles = rdir.list();
        if ( rFiles != null )
        {
            for ( final String file : rFiles )
            {
                final File f = new File( rdir, file );
                try
                {
                    final String json = FileUtils.readFileToString( f );
                    final Repository r = serializer.fromString( json, Repository.class );
                    if ( r == null )
                    {
                        f.delete();
                    }
                    else
                    {
                        storeRepository( r );
                    }
                }
                catch ( final IOException e )
                {
                    logger.error( "Failed to load repository: {}. Reason: {}", e, f, e.getMessage() );
                }
            }
        }

        final File gdir = new File( basedir, StoreType.group.name() );
        final String[] gFiles = gdir.list();
        if ( gFiles != null )
        {
            for ( final String file : gFiles )
            {
                final File f = new File( gdir, file );
                try
                {
                    final String json = FileUtils.readFileToString( f );
                    final Group g = serializer.fromString( json, Group.class );
                    if ( g == null )
                    {
                        f.delete();
                    }
                    else
                    {
                        storeGroup( g );
                    }
                }
                catch ( final IOException e )
                {
                    logger.error( "Failed to load group: {}. Reason: {}", e, f, e.getMessage() );
                }
            }
        }
    }

    @Override
    public void storeDeployPoints( final Collection<DeployPoint> deploys )
        throws ProxyDataException
    {
        super.storeDeployPoints( deploys );
        store( false, deploys.toArray( new ArtifactStore[] {} ) );
    }

    @Override
    public boolean storeDeployPoint( final DeployPoint deploy )
        throws ProxyDataException
    {
        final boolean result = super.storeDeployPoint( deploy );
        if ( result )
        {
            store( false, deploy );
        }

        return result;
    }

    @Override
    public boolean storeDeployPoint( final DeployPoint deploy, final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = super.storeDeployPoint( deploy, skipIfExists );
        if ( result )
        {
            store( skipIfExists, deploy );
        }

        return result;
    }

    @Override
    public void storeRepositories( final Collection<Repository> repos )
        throws ProxyDataException
    {
        super.storeRepositories( repos );
        store( false, repos.toArray( new ArtifactStore[] {} ) );
    }

    @Override
    public boolean storeRepository( final Repository proxy )
        throws ProxyDataException
    {
        final boolean result = super.storeRepository( proxy );
        if ( result )
        {
            store( false, proxy );
        }

        return result;
    }

    @Override
    public boolean storeRepository( final Repository repository, final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = super.storeRepository( repository, skipIfExists );
        if ( result )
        {
            store( skipIfExists, repository );
        }

        return result;
    }

    @Override
    public void storeGroups( final Collection<Group> groups )
        throws ProxyDataException
    {
        super.storeGroups( groups );
        store( false, groups.toArray( new ArtifactStore[] {} ) );
    }

    @Override
    public boolean storeGroup( final Group group )
        throws ProxyDataException
    {
        final boolean result = super.storeGroup( group );
        if ( result )
        {
            store( false, group );
        }

        return result;
    }

    @Override
    public boolean storeGroup( final Group group, final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = super.storeGroup( group, skipIfExists );
        if ( result )
        {
            store( false, group );
        }

        return result;
    }

    @Override
    public void deleteDeployPoint( final DeployPoint deploy )
        throws ProxyDataException
    {
        super.deleteDeployPoint( deploy );
        delete( deploy );
    }

    @Override
    public void deleteDeployPoint( final String name )
        throws ProxyDataException
    {
        super.deleteDeployPoint( name );
        delete( StoreType.deploy_point, name );
    }

    @Override
    public void deleteRepository( final Repository repo )
        throws ProxyDataException
    {
        super.deleteRepository( repo );
        delete( repo );
    }

    @Override
    public void deleteRepository( final String name )
        throws ProxyDataException
    {
        super.deleteRepository( name );
        delete( StoreType.repository, name );
    }

    @Override
    public void deleteGroup( final Group group )
        throws ProxyDataException
    {
        super.deleteGroup( group );
        delete( group );
    }

    @Override
    public void deleteGroup( final String name )
        throws ProxyDataException
    {
        super.deleteGroup( name );
        delete( StoreType.group, name );
    }

    private void store( final boolean skipIfExists, final ArtifactStore... stores )
        throws ProxyDataException
    {
        final File basedir = config.getStorageDir( APROX_STORE );
        for ( final ArtifactStore store : stores )
        {
            final File dir = new File( basedir, store.getDoctype()
                                                     .name() );
            if ( !dir.isDirectory() && !dir.mkdirs() )
            {
                throw new ProxyDataException( "Cannot create storage directory: {} for definition: {}", dir, store );
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
                throw new ProxyDataException( "Cannot write definition: {} to: {}. Reason: {}", e, store, f, e.getMessage() );
            }
        }
    }

    private void delete( final ArtifactStore... stores )
    {
        final File basedir = config.getStorageDir( APROX_STORE );
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
        final File basedir = config.getStorageDir( APROX_STORE );
        final File dir = new File( basedir, type.name() );

        final File f = new File( dir, name + ".json" );
        if ( f.exists() )
        {
            f.delete();
        }
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore store )
        throws ProxyDataException
    {
        final boolean result = super.storeArtifactStore( store );
        if ( result )
        {
            store( false, store );
        }

        return result;
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore store, final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = super.storeArtifactStore( store );
        if ( result )
        {
            store( false, store );
        }

        return result;
    }

    @Override
    public void deleteArtifactStore( final StoreKey key )
        throws ProxyDataException
    {
        super.deleteArtifactStore( key );
        delete( key.getType(), key.getName() );
    }

    @Override
    public void clear()
        throws ProxyDataException
    {
        super.clear();

        final File basedir = config.getStorageDir( APROX_STORE );
        try
        {
            FileUtils.forceDelete( basedir );
        }
        catch ( final IOException e )
        {
            throw new ProxyDataException( "Failed to delete AProx storage files: {}", e, e.getMessage() );
        }
    }

    @Override
    public void install()
        throws ProxyDataException
    {
        if ( !config.getStorageDir( APROX_STORE )
                    .isDirectory() )
        {
            storeRepository( new Repository( "central", "http://repo1.maven.apache.org/maven2/" ), true );
            storeGroup( new Group( "public", new StoreKey( StoreType.repository, "central" ) ), true );
        }
    }

}
