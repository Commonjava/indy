/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.flat.data;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.mem.data.MemoryStoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.subsys.flatfile.conf.FlatFile;
import org.commonjava.aprox.subsys.flatfile.conf.FlatFileManager;
import org.commonjava.web.json.ser.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;

@ApplicationScoped
@Alternative
public class FlatFileStoreDataManager
    extends MemoryStoreDataManager
{
    public static final String APROX_STORE = "aprox";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private FlatFileManager manager;

    @Inject
    @AproxData
    private JsonSerializer serializer;

    protected FlatFileStoreDataManager()
    {
    }

    protected FlatFileStoreDataManager( final FlatFileManager manager, final JsonSerializer serializer )
    {
        this.manager = manager;
        this.serializer = serializer;
    }

    @PostConstruct
    public void readDefinitions()
        throws ProxyDataException
    {
        FlatFile dir = manager.getDataFile( APROX_STORE, StoreType.hosted.singularEndpointName() );

        String[] files = dir.list();
        if ( files != null )
        {
            for ( final String file : files )
            {
                final FlatFile f = dir.getChild( file );
                try
                {
                    final String json = f.readString();
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
                    logger.error( String.format( "Failed to load deploy point: %s. Reason: %s", f, e.getMessage() ), e );
                }
            }
        }

        dir = manager.getDataFile( APROX_STORE, StoreType.remote.singularEndpointName() );
        files = dir.list();
        if ( files != null )
        {
            for ( final String file : files )
            {
                final FlatFile f = dir.getChild( file );
                try
                {
                    final String json = f.readString();
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
                    logger.error( String.format( "Failed to load repository: %s. Reason: %s", f, e.getMessage() ), e );
                }
                catch ( final JsonSyntaxException e )
                {
                    logger.error( String.format( "Failed to load repository: %s. Reason: %s", f, e.getMessage() ), e );
                }
            }
        }

        dir = manager.getDataFile( APROX_STORE, StoreType.group.singularEndpointName() );
        files = dir.list();
        if ( files != null )
        {
            for ( final String file : files )
            {
                final FlatFile f = dir.getChild( file );
                try
                {
                    final String json = f.readString();
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
                    logger.error( String.format( "Failed to load group: %s. Reason: %s", f, e.getMessage() ), e );
                }
            }
        }
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
        for ( final ArtifactStore store : stores )
        {
            final FlatFile f =
                manager.getDataFile( APROX_STORE, store.getKey()
                                                       .getType()
                                                       .singularEndpointName(), store.getName() + ".json" );
            if ( skipIfExists && f.exists() )
            {
                continue;
            }

            final FlatFile d = f.getParent();
            if ( !d.mkdirs() )
            {
                throw new ProxyDataException( "Cannot create storage directory: {} for definition: {}", d, store );
            }

            try
            {
                f.writeString( serializer.toString( store ), "UTF-8" );
            }
            catch ( final IOException e )
            {
                throw new ProxyDataException( "Cannot write definition: {} to: {}. Reason: {}", e, store, f,
                                              e.getMessage() );
            }
        }
    }

    private void delete( final ArtifactStore... stores )
        throws ProxyDataException
    {
        for ( final ArtifactStore store : stores )
        {
            final FlatFile f =
                manager.getDataFile( APROX_STORE, store.getKey()
                                                       .getType()
                                                       .singularEndpointName(), store.getName() + ".json" );
            try
            {
                f.delete();
            }
            catch ( final IOException e )
            {
                throw new ProxyDataException( "Cannot delete store definition: {} in file: {}. Reason: {}", e, store,
                                              f, e.getMessage() );
            }
        }
    }

    private void delete( final StoreType type, final String name )
        throws ProxyDataException
    {
        final FlatFile f = manager.getDataFile( APROX_STORE, type.singularEndpointName(), name + ".json" );
        try
        {
            f.delete();
        }
        catch ( final IOException e )
        {
            throw new ProxyDataException( "Cannot delete store definition: {}:{} in file: {}. Reason: {}", e, type,
                                          name, f, e.getMessage() );
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

        final FlatFile basedir = manager.getDataFile( APROX_STORE );
        try
        {
            basedir.delete();
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
        if ( !manager.getDataFile( APROX_STORE )
                    .isDirectory() )
        {
            storeRemoteRepository( new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" ), true );
            storeGroup( new Group( "public", new StoreKey( StoreType.remote, "central" ) ), true );
        }
    }

    @Override
    public void reload()
        throws ProxyDataException
    {
        // NOTE: Call to super for this, because the local implementation DELETES THE DB DIR!!!
        super.clear();
        readDefinitions();
    }

}
