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
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.subsys.flatfile.conf.FlatFileConfiguration;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.JsonSerializer;

@ApplicationScoped
@Alternative
public class FlatFileStoreDataManager
    extends MemoryStoreDataManager
{
    private static final String APROX_STORE = "aprox";

    private final Logger logger = new Logger( getClass() );

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
        final File ddir = new File( basedir, StoreType.hosted.name() );

        final String[] dFiles = ddir.list();
        if ( dFiles != null )
        {
            for ( final String file : dFiles )
            {
                final File f = new File( ddir, file );
                try
                {
                    final String json = FileUtils.readFileToString( f );
                    final HostedRepository dp = serializer.fromString( json, HostedRepository.class );
                    if ( dp == null )
                    {
                        f.delete();
                    }
                    else
                    {
                        storeHostedRepository( dp );
                    }
                }
                catch ( final IOException e )
                {
                    logger.error( "Failed to load deploy point: %s. Reason: %s", e, f, e.getMessage() );
                }
            }
        }

        final File rdir = new File( basedir, StoreType.remote.name() );
        final String[] rFiles = rdir.list();
        if ( rFiles != null )
        {
            for ( final String file : rFiles )
            {
                final File f = new File( rdir, file );
                try
                {
                    final String json = FileUtils.readFileToString( f );
                    final RemoteRepository r = serializer.fromString( json, RemoteRepository.class );
                    if ( r == null )
                    {
                        f.delete();
                    }
                    else
                    {
                        storeRemoteRepository( r );
                    }
                }
                catch ( final IOException e )
                {
                    logger.error( "Failed to load repository: %s. Reason: %s", e, f, e.getMessage() );
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
                    logger.error( "Failed to load group: %s. Reason: %s", e, f, e.getMessage() );
                }
            }
        }
    }

    @Override
    public void storeHostedRepositories( final Collection<HostedRepository> deploys )
        throws ProxyDataException
    {
        super.storeHostedRepositories( deploys );
        store( false, deploys.toArray( new ArtifactStore[] {} ) );
    }

    @Override
    public boolean storeHostedRepository( final HostedRepository deploy )
        throws ProxyDataException
    {
        final boolean result = super.storeHostedRepository( deploy );
        if ( result )
        {
            store( false, deploy );
        }

        return result;
    }

    @Override
    public boolean storeHostedRepository( final HostedRepository deploy, final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = super.storeHostedRepository( deploy, skipIfExists );
        if ( result )
        {
            store( skipIfExists, deploy );
        }

        return result;
    }

    @Override
    public void storeRemoteRepositories( final Collection<RemoteRepository> repos )
        throws ProxyDataException
    {
        super.storeRemoteRepositories( repos );
        store( false, repos.toArray( new ArtifactStore[] {} ) );
    }

    @Override
    public boolean storeRemoteRepository( final RemoteRepository proxy )
        throws ProxyDataException
    {
        final boolean result = super.storeRemoteRepository( proxy );
        if ( result )
        {
            store( false, proxy );
        }

        return result;
    }

    @Override
    public boolean storeRemoteRepository( final RemoteRepository repository, final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = super.storeRemoteRepository( repository, skipIfExists );
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
    public void deleteHostedRepository( final HostedRepository deploy )
        throws ProxyDataException
    {
        super.deleteHostedRepository( deploy );
        delete( deploy );
    }

    @Override
    public void deleteHostedRepository( final String name )
        throws ProxyDataException
    {
        super.deleteHostedRepository( name );
        delete( StoreType.hosted, name );
    }

    @Override
    public void deleteRemoteRepository( final RemoteRepository repo )
        throws ProxyDataException
    {
        super.deleteRemoteRepository( repo );
        delete( repo );
    }

    @Override
    public void deleteRemoteRepository( final String name )
        throws ProxyDataException
    {
        super.deleteRemoteRepository( name );
        delete( StoreType.remote, name );
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
                throw new ProxyDataException( "Cannot write definition: %s to: %s. Reason: %s", e, store, f, e.getMessage() );
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
            throw new ProxyDataException( "Failed to delete AProx storage files: %s", e, e.getMessage() );
        }
    }

    @Override
    public void install()
        throws ProxyDataException
    {
        if ( !config.getStorageDir( APROX_STORE )
                    .isDirectory() )
        {
            storeRemoteRepository( new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" ), true );
            storeGroup( new Group( "public", new StoreKey( StoreType.remote, "central" ) ), true );
        }
    }

}
