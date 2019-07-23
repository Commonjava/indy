/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.dotmaven.store.sub;

import net.sf.webdav.StoredObject;
import net.sf.webdav.exceptions.WebdavException;
import net.sf.webdav.spi.ITransaction;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.dotmaven.DotMavenException;
import org.commonjava.indy.dotmaven.data.StorageAdvice;
import org.commonjava.indy.dotmaven.data.StorageAdvisor;
import org.commonjava.indy.dotmaven.store.SubStore;
import org.commonjava.indy.dotmaven.util.StoreURIMatcher;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.lang.StringUtils.isEmpty;

@ApplicationScoped
@Named( "stores" )
public class ArtifactStoreSubStore
    implements SubStore
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager indy;

    @Inject
    private StorageAdvisor advisor;

    @Inject
    private DownloadManager fileManager;

    @Override
    public String[] getRootResourceNames()
    {
        return new String[] { "storage" };
    }

    @Override
    public boolean matchesUri( final String uri )
    {
        return new StoreURIMatcher( uri ).matches();
    }

    @Override
    public void createFolder( final ITransaction transaction, final String folderUri )
        throws WebdavException
    {
        final StoreURIMatcher matcher = new StoreURIMatcher( folderUri );
        if ( !matcher.hasStorePath() )
        {
            throw new WebdavException( "No store-level path specified: '" + folderUri
                + "'. This URI references either a list of stores, a root store directory, or something else equally read-only." );
        }

        final StorageAdvice advice = getStorageAdviceFor( matcher );

        final String path = matcher.getStorePath();
        final Transfer item = fileManager.getStorageReference( advice.getHostedStore(), path );
        try
        {
            item.mkdirs();
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to create folder: {} in store: {}. Reason: {}", e, path, advice.getStore()
                                                                                                 .getKey(), e.getMessage() );
            throw new WebdavException( "Failed to create folder: " + folderUri );
        }
    }

    @Override
    public void createResource( final ITransaction transaction, final String resourceUri )
        throws WebdavException
    {
        final StoreURIMatcher matcher = new StoreURIMatcher( resourceUri );
        if ( !matcher.hasStorePath() )
        {
            throw new WebdavException( "No store-level path specified: '" + resourceUri
                + "'. This URI references either a list of stores, a root store directory, or something else equally read-only." );
        }

        final StorageAdvice advice = getStorageAdviceFor( matcher );

        final String path = matcher.getStorePath();
        final Transfer item = fileManager.getStorageReference( advice.getHostedStore(), path );
        try
        {
            item.createFile();
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to create file: {} in store: {}. Reason: {}", e, path, advice.getStore()
                                                                                               .getKey(), e.getMessage() );
            throw new WebdavException( "Failed to create file: " + resourceUri );
        }
    }

    @Override
    public InputStream getResourceContent( final ITransaction transaction, final String resourceUri )
        throws WebdavException
    {
        final StoreURIMatcher matcher = new StoreURIMatcher( resourceUri );
        final Transfer item = getTransfer( matcher );
        if ( item == null )
        {
            throw new WebdavException( "Cannot read content: " + resourceUri );
        }

        final String path = item.getPath();
        final StoreKey key = LocationUtils.getKey( item );

        try
        {
            return item.openInputStream();
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Failed to open InputStream for: %s in store: %s. Reason: %s", path, key, e.getMessage() ), e );
            throw new WebdavException( "Failed to get content for: " + resourceUri );
        }
    }

    private Transfer getTransfer( final StoreURIMatcher matcher )
        throws WebdavException
    {
        final String resourceUri = matcher.getURI();

        if ( !matcher.hasStorePath() )
        {
            throw new WebdavException( "No store-level path specified: '" + resourceUri
                + "'. This URI references either a list of stores, a root store directory, or something else that cannot be read as a file." );
        }

        final String path = matcher.getStorePath();
        final StoreKey key = matcher.getStoreKey();

        Transfer item = null;
        try
        {
            if ( key != null && StoreType.group == key.getType() )
            {
                final List<ArtifactStore> stores = indy.query().packageType( key.getPackageType() ).enabledState( true ).getOrderedStoresInGroup( key.getName() );
                for ( final ArtifactStore store : stores )
                {
                    //                    logger.info( "Getting Transfer for: {} from: {}", path, store );
                    final Transfer si = fileManager.getStorageReference( store, path );
                    if ( si.exists() )
                    {
                        //                        logger.info( "Using Transfer: {} for path: {}", si, path );
                        item = si;
                        break;
                    }
                }
            }
            else
            {
                final ArtifactStore store = indy.getArtifactStore( key );
                if ( store == null )
                {
                    throw new WebdavException( "Cannot find store: " + key );
                }

                //                logger.info( "Getting Transfer for: {} from: {}", path, store );
                final Transfer si = fileManager.getStorageReference( store, path );
                if ( si.exists() )
                {
                    //                    logger.info( "Using Transfer: {} for path: {}", si, path );
                    item = si;
                }
            }
        }
        catch ( final IndyDataException e )
        {
            logger.error( String.format( "Failed to lookup ArtifactStore(s) for key: %s. Reason: %s", key, e.getMessage() ), e );
            throw new WebdavException( "Failed to get content for: " + resourceUri );
        }

        return item;
    }

    @Override
    public long setResourceContent( final ITransaction transaction, final String resourceUri, final InputStream content, final String contentType,
                                    final String characterEncoding )
        throws WebdavException
    {
        final StoreURIMatcher matcher = new StoreURIMatcher( resourceUri );
        if ( !matcher.hasStorePath() )
        {
            throw new WebdavException( "No store-level path specified: '" + resourceUri
                + "'. This URI references either a list of stores, a root store directory, or something else equally read-only." );
        }

        final StorageAdvice advice = getStorageAdviceFor( matcher );

        final String path = matcher.getStorePath();
        final Transfer item = fileManager.getStorageReference( advice.getHostedStore(), path );
        Writer writer = null;
        try
        {
            if ( characterEncoding != null )
            {
                writer = new OutputStreamWriter( item.openOutputStream( TransferOperation.UPLOAD ), characterEncoding );
            }
            else
            {
                writer = new OutputStreamWriter( item.openOutputStream( TransferOperation.UPLOAD ) );
            }

            copy( content, writer );

            return item.length();
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to write file: {} in store: {}. Reason: {}", e, path, advice.getStore()
                                                                                              .getKey(), e.getMessage() );
            throw new WebdavException( "Failed to write file: " + resourceUri );
        }
        finally
        {
            closeQuietly( writer );
        }
    }

    @Override
    public String[] getChildrenNames( final ITransaction transaction, final String folderUri )
        throws WebdavException
    {
        String[] names;
        final StoreURIMatcher matcher = new StoreURIMatcher( folderUri );
        if ( matcher.hasStorePath() || matcher.hasStoreName() )
        {
            String path = matcher.getStorePath();
            if ( isEmpty( path ) )
            {
                path = PathUtils.ROOT;
            }

            final StoreKey key = matcher.getStoreKey();
            try
            {
                if ( key != null && StoreType.group == key.getType() )
                {
                    final List<ArtifactStore> stores =
                            indy.query().packageType( key.getPackageType() ).getOrderedStoresInGroup( key.getName() );

                    final Set<String> noms = new TreeSet<>();
                    for ( final ArtifactStore store : stores )
                    {
                        final Transfer item = fileManager.getStorageReference( store, path );
                        if ( !item.exists() )
                        {
                            continue;
                        }

                        if ( !item.isDirectory() )
                        {
                            logger.error( "Transfer: {} in {} is not a directory.", path, store.getKey() );
                            continue;
                        }

                        noms.addAll( Arrays.asList( item.list() ) );
                    }

                    names = noms.toArray( new String[noms.size()] );
                }
                else
                {
                    final ArtifactStore store = indy.getArtifactStore( key );

                    if ( store == null )
                    {
                        logger.error( "Cannot find ArtifactStore to match key: {}.", key );
                        names = new String[] {};
                    }
                    else
                    {
                        final Transfer item = fileManager.getStorageReference( store, path );
                        if ( !item.exists() || !item.isDirectory() )
                        {
                            logger.error( "Transfer: {} in {} is not a directory.", path, store.getKey() );
                            names = new String[] {};
                        }
                        else
                        {
                            names = item.list();
                        }
                    }
                }
            }
            catch ( final IndyDataException e )
            {
                logger.error( String.format( "Failed to lookup ArtifactStore(s) for key: %s. Reason: %s", key, e.getMessage() ), e );
                throw new WebdavException( "Failed to get listing for: " + folderUri );
            }
            catch ( final IOException e )
            {
                logger.error( String.format( "Failed to list %s in %s. Reason: %s", path, key, e.getMessage() ), e );
                throw new WebdavException( "Failed to get listing for: " + folderUri );
            }
        }
        else if ( matcher.hasStoreType() )
        {
            String packageType = matcher.getPackageType();
            final StoreType type = matcher.getStoreType();
            try
            {
                List<String> noms = indy.query()
                           .packageType( packageType )
                           .storeTypes( type )
                           .stream()
                           .map( ArtifactStore::getName )
                           .collect( Collectors.toList() );

                names = noms.toArray( new String[noms.size()] );
            }
            catch ( final IndyDataException e )
            {
                logger.error( String.format( "Failed to lookup ArtifactStores of type: %s. Reason: %s", type, e.getMessage() ), e );
                throw new WebdavException( "Failed to get listing for: " + folderUri );
            }
        }
        else
        {
            names =
                new String[] { StoreType.hosted.singularEndpointName(), StoreType.group.singularEndpointName(),
                    StoreType.remote.singularEndpointName() };
        }

        return names;
    }

    @Override
    public long getResourceLength( final ITransaction transaction, final String path )
        throws WebdavException
    {
        final StoreURIMatcher matcher = new StoreURIMatcher( path );
        if ( matcher.hasStorePath() )
        {
            final Transfer item = getTransfer( matcher );
            if ( item != null )
            {
                return item.length();
            }

        }

        return 0;
    }

    @Override
    public void removeObject( final ITransaction transaction, final String uri )
        throws WebdavException
    {
        final StoreURIMatcher matcher = new StoreURIMatcher( uri );
        if ( !matcher.hasStorePath() )
        {
            throw new WebdavException( "No store-level path specified: '" + uri
                + "'. This URI references either a list of stores, a root store directory, or something else equally read-only." );
        }

        final StorageAdvice advice = getStorageAdviceFor( matcher );

        final String path = matcher.getStorePath();
        final Transfer item = fileManager.getStorageReference( advice.getHostedStore(), path );
        try
        {
            if ( item.exists() )
            {
                item.delete();
            }
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to delete file: {} in store: {}. Reason: {}", e, path, advice.getStore()
                                                                                               .getKey(), e.getMessage() );
            throw new WebdavException( "Failed to delete file: " + uri );
        }
    }

    @Override
    public StoredObject getStoredObject( final ITransaction transaction, final String uri )
        throws WebdavException
    {
        final StoredObject so = new StoredObject();

        final StoreURIMatcher matcher = new StoreURIMatcher( uri );
        if ( matcher.hasStorePath() )
        {
            final Transfer item = getTransfer( matcher );

            if ( item == null )
            {
                return null;
            }

            so.setCreationDate( new Date( item.lastModified() ) );
            so.setLastModified( new Date( item.lastModified() ) );
            so.setFolder( item.isDirectory() );
            so.setResourceLength( item.length() );
        }
        else
        {
            final Date d = new Date();
            so.setCreationDate( d );
            so.setLastModified( d );
            so.setFolder( true );
        }

        return so;
    }

    private StorageAdvice getStorageAdviceFor( final StoreURIMatcher matcher )
        throws WebdavException
    {
        final String uri = matcher.getURI();
        final StoreKey key = matcher.getStoreKey();
        ArtifactStore store;
        try
        {
            store = indy.getArtifactStore( key );
        }
        catch ( final IndyDataException e )
        {
            logger.error( String.format( "Failed to retrieve artifact store: %s for URI: %s\nReason: %s", key, uri, e.getMessage() ), e );
            throw new WebdavException( "Cannot create: " + uri );
        }

        if ( store == null )
        {
            throw new WebdavException( "Cannot retrieve ArtifactStore: " + key );
        }

        StorageAdvice advice;
        try
        {
            advice = advisor.getStorageAdvice( store );
        }
        catch ( final DotMavenException e )
        {
            logger.error( String.format( "Failed to retrieve storage advice for: %s (URI: %s)\nReason: %s", key, uri, e.getMessage() ), e );
            throw new WebdavException( "Cannot create: " + uri );
        }

        if ( !advice.isDeployable() )
        {
            throw new WebdavException( "Read-only area. Cannot create: " + uri );
        }

        return advice;
    }

}
