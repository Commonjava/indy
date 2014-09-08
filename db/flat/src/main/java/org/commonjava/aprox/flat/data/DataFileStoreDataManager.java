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
import java.security.PrivilegedAction;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import org.commonjava.aprox.audit.SecuritySystem;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.mem.data.MemoryStoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.subsys.flatfile.conf.DataFile;
import org.commonjava.aprox.subsys.flatfile.conf.DataFileManager;
import org.commonjava.web.json.ser.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;

@ApplicationScoped
@Alternative
public class DataFileStoreDataManager
    extends MemoryStoreDataManager
{
    public static final String APROX_STORE = "aprox";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private DataFileManager manager;

    @Inject
    @AproxData
    private JsonSerializer serializer;

    @Inject
    private SecuritySystem securitySystem;

    protected DataFileStoreDataManager()
    {
    }

    protected DataFileStoreDataManager( final DataFileManager manager, final JsonSerializer serializer,
                                        final SecuritySystem securitySystem )
    {
        this.manager = manager;
        this.serializer = serializer;
        this.securitySystem = securitySystem;
    }

    @PostConstruct
    public void readDefinitions()
        throws ProxyDataException
    {
        final ProxyDataException error = securitySystem.runAsSystemUser( new PrivilegedAction<ProxyDataException>()
        {
            @Override
            public ProxyDataException run()
            {
                DataFile dir = manager.getDataFile( APROX_STORE, StoreType.hosted.singularEndpointName() );
                final String summary = "Reading definitions from disk, culling invalid definition files.";

                String[] files = dir.list();
                if ( files != null )
                {
                    for ( final String file : files )
                    {
                        final DataFile f = dir.getChild( file );
                        try
                        {
                            final String json = f.readString();
                            final HostedRepository dp = serializer.fromString( json, HostedRepository.class );
                            if ( dp == null )
                            {
                                f.delete( summary );
                            }
                            else
                            {
                                storeHostedRepository( dp, summary );
                            }
                        }
                        catch ( final IOException e )
                        {
                            logger.error( String.format( "Failed to load deploy point: %s. Reason: %s", f,
                                                         e.getMessage() ), e );
                        }
                        catch ( final ProxyDataException e )
                        {
                            return e;
                        }
                    }
                }

                dir = manager.getDataFile( APROX_STORE, StoreType.remote.singularEndpointName() );
                files = dir.list();
                if ( files != null )
                {
                    for ( final String file : files )
                    {
                        final DataFile f = dir.getChild( file );
                        try
                        {
                            final String json = f.readString();
                            final RemoteRepository r = serializer.fromString( json, RemoteRepository.class );
                            if ( r == null )
                            {
                                f.delete( summary );
                            }
                            else
                            {
                                storeRemoteRepository( r, summary );
                            }
                        }
                        catch ( final IOException e )
                        {
                            logger.error( String.format( "Failed to load repository: %s. Reason: %s", f, e.getMessage() ),
                                          e );
                        }
                        catch ( final JsonSyntaxException e )
                        {
                            logger.error( String.format( "Failed to load repository: %s. Reason: %s", f, e.getMessage() ),
                                          e );
                        }
                        catch ( final ProxyDataException e )
                        {
                            return e;
                        }
                    }
                }

                dir = manager.getDataFile( APROX_STORE, StoreType.group.singularEndpointName() );
                files = dir.list();
                if ( files != null )
                {
                    for ( final String file : files )
                    {
                        final DataFile f = dir.getChild( file );
                        try
                        {
                            final String json = f.readString();
                            final Group g = serializer.fromString( json, Group.class );
                            if ( g == null )
                            {
                                f.delete( summary );
                            }
                            else
                            {
                                storeGroup( g, summary );
                            }
                        }
                        catch ( final IOException e )
                        {
                            logger.error( String.format( "Failed to load group: %s. Reason: %s", f, e.getMessage() ), e );
                        }
                        catch ( final ProxyDataException e )
                        {
                            return e;
                        }
                    }
                }

                return null;
            }
        } );

        if ( error != null )
        {
            throw error;
        }
    }

    @Override
    public boolean storeHostedRepository( final HostedRepository deploy, final String summary )
        throws ProxyDataException
    {
        final boolean result = super.storeHostedRepository( deploy, summary );
        if ( result )
        {
            store( false, summary, deploy );
        }

        return result;
    }

    @Override
    public boolean storeHostedRepository( final HostedRepository deploy, final String summary,
                                          final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = super.storeHostedRepository( deploy, summary, skipIfExists );
        if ( result )
        {
            store( skipIfExists, summary, deploy );
        }

        return result;
    }

    @Override
    public boolean storeRemoteRepository( final RemoteRepository proxy, final String summary )
        throws ProxyDataException
    {
        final boolean result = super.storeRemoteRepository( proxy, summary );
        if ( result )
        {
            store( false, summary, proxy );
        }

        return result;
    }

    @Override
    public boolean storeRemoteRepository( final RemoteRepository repository, final String summary,
                                          final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = super.storeRemoteRepository( repository, summary, skipIfExists );
        if ( result )
        {
            store( skipIfExists, summary, repository );
        }

        return result;
    }

    @Override
    public boolean storeGroup( final Group group, final String summary )
        throws ProxyDataException
    {
        final boolean result = super.storeGroup( group, summary );
        if ( result )
        {
            store( false, summary, group );
        }

        return result;
    }

    @Override
    public boolean storeGroup( final Group group, final String summary, final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = super.storeGroup( group, summary, skipIfExists );
        if ( result )
        {
            store( false, summary, group );
        }

        return result;
    }

    @Override
    public void deleteHostedRepository( final HostedRepository deploy, final String summary )
        throws ProxyDataException
    {
        super.deleteHostedRepository( deploy, summary );
        delete( summary, deploy );
    }

    @Override
    public void deleteHostedRepository( final String name, final String summary )
        throws ProxyDataException
    {
        super.deleteHostedRepository( name, summary );
        delete( StoreType.hosted, name, summary );
    }

    @Override
    public void deleteRemoteRepository( final RemoteRepository repo, final String summary )
        throws ProxyDataException
    {
        super.deleteRemoteRepository( repo, summary );
        delete( summary, repo );
    }

    @Override
    public void deleteRemoteRepository( final String name, final String summary )
        throws ProxyDataException
    {
        super.deleteRemoteRepository( name, summary );
        delete( StoreType.remote, name, summary );
    }

    @Override
    public void deleteGroup( final Group group, final String summary )
        throws ProxyDataException
    {
        super.deleteGroup( group, summary );
        delete( summary, group );
    }

    @Override
    public void deleteGroup( final String name, final String summary )
        throws ProxyDataException
    {
        super.deleteGroup( name, summary );
        delete( StoreType.group, name, summary );
    }

    private void store( final boolean skipIfExists, final String summary, final ArtifactStore... stores )
        throws ProxyDataException
    {
        for ( final ArtifactStore store : stores )
        {
            final DataFile f =
                manager.getDataFile( APROX_STORE, store.getKey()
                                                       .getType()
                                                       .singularEndpointName(), store.getName() + ".json" );
            if ( skipIfExists && f.exists() )
            {
                continue;
            }

            final DataFile d = f.getParent();
            if ( !d.mkdirs() )
            {
                throw new ProxyDataException( "Cannot create storage directory: {} for definition: {}", d, store );
            }

            try
            {
                f.writeString( serializer.toString( store ), "UTF-8", summary );
            }
            catch ( final IOException e )
            {
                throw new ProxyDataException( "Cannot write definition: {} to: {}. Reason: {}", e, store, f,
                                              e.getMessage() );
            }
        }
    }

    private void delete( final String summary, final ArtifactStore... stores )
        throws ProxyDataException
    {
        for ( final ArtifactStore store : stores )
        {
            final DataFile f =
                manager.getDataFile( APROX_STORE, store.getKey()
                                                       .getType()
                                                       .singularEndpointName(), store.getName() + ".json" );
            try
            {
                f.delete( summary );
            }
            catch ( final IOException e )
            {
                throw new ProxyDataException( "Cannot delete store definition: {} in file: {}. Reason: {}", e, store,
                                              f, e.getMessage() );
            }
        }
    }

    private void delete( final StoreType type, final String name, final String summary )
        throws ProxyDataException
    {
        final DataFile f = manager.getDataFile( APROX_STORE, type.singularEndpointName(), name + ".json" );
        try
        {
            f.delete( summary );
        }
        catch ( final IOException e )
        {
            throw new ProxyDataException( "Cannot delete store definition: {}:{} in file: {}. Reason: {}", e, type,
                                          name, f, e.getMessage() );
        }
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore store, final String summary )
        throws ProxyDataException
    {
        final boolean result = super.storeArtifactStore( store, summary );
        if ( result )
        {
            store( false, summary, store );
        }

        return result;
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore store, final String summary, final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = super.storeArtifactStore( store, summary );
        if ( result )
        {
            store( false, summary, store );
        }

        return result;
    }

    @Override
    public void deleteArtifactStore( final StoreKey key, final String summary )
        throws ProxyDataException
    {
        super.deleteArtifactStore( key, summary );
        delete( key.getType(), key.getName(), summary );
    }

    @Override
    public void clear( final String summary )
        throws ProxyDataException
    {
        super.clear( summary );

        final DataFile basedir = manager.getDataFile( APROX_STORE );
        try
        {
            basedir.delete( summary );
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
            final ProxyDataException error = securitySystem.runAsSystemUser( new PrivilegedAction<ProxyDataException>()
            {
                @Override
                public ProxyDataException run()
                {
                    final String summary = "Initializing defaults";

                    try
                    {
                        storeRemoteRepository( new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" ),
                                               summary, true );

                        storeGroup( new Group( "public", new StoreKey( StoreType.remote, "central" ) ), summary, true );
                    }
                    catch ( final ProxyDataException e )
                    {
                        return e;
                    }

                    return null;
                }
            } );

            if ( error != null )
            {
                throw error;
            }
        }
    }

    @Override
    public void reload()
        throws ProxyDataException
    {
        // NOTE: Call to super for this, because the local implementation DELETES THE DB DIR!!!
        super.clear( "Reloading from storage" );
        readDefinitions();
    }

}
