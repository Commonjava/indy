/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.core.bind.jaxrs.util;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.maven.galley.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MaintenanceController
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private CacheProvider cacheProvider;

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private IndyObjectMapper objectMapper;

    public Set<StoreKey> getTombstoneStores( String packageType ) throws IndyDataException
    {
        List<HostedRepository> stores = storeDataManager.query().getAllHostedRepositories( packageType );
        Set<StoreKey> tombstoneStores = new HashSet<>();
        for ( HostedRepository hosted : stores )
        {
            StoreKey key = hosted.getKey();
            ConcreteResource root = new ConcreteResource( LocationUtils.toLocation( hosted ), PathUtils.ROOT );
            String[] files = cacheProvider.list( root );
            if ( files == null || files.length == 0 )
            {
                logger.debug( "Empty store: {}", key );
                Set<Group> affected = storeDataManager.affectedBy( Arrays.asList( key ) );
                if ( affected == null || affected.isEmpty() )
                {
                    logger.info( "Find tombstone store (no content and not in any group): {}", key );
                    tombstoneStores.add( key );
                }
            }
        }
        return tombstoneStores;
    }

    public void importStoreZip( ServletInputStream inputStream )
    {

        try ( ZipInputStream stream = new ZipInputStream( inputStream ) )
        {
            int hosted = 0;
            int remote = 0;
            int groupCount = 0;
            ZipEntry entry;
            while((entry = stream.getNextEntry())!=null)
            {
                logger.trace( "Read entry: %s", entry.getName() );

                ByteArrayOutputStream bos = null;
                InputStream is = null;
                try
                {
                    bos = new ByteArrayOutputStream();
                    int len;
                    byte[] buffer = new byte[1024];
                    while ((len = stream.read( buffer)) > 0)
                    {
                        bos.write(buffer, 0, len);
                    }

                    is =  new ByteArrayInputStream( bos.toByteArray() );

                    if ( entry.getName().contains( "/remote/" ) )
                    {
                        storeArtifactStore( objectMapper.readValue( is, RemoteRepository.class ) );
                        remote++;
                    }
                    else if ( entry.getName().contains( "/hosted/" ) )
                    {
                        storeArtifactStore( objectMapper.readValue( is, HostedRepository.class ) );
                        hosted++;
                    }
                    else if ( entry.getName().contains( "/group/" ) )
                    {
                        storeArtifactStore( objectMapper.readValue( is, Group.class ) );
                        groupCount++;
                    }
                }
                catch ( Exception e )
                {
                    logger.warn( "Store the artifact store {} error: {}", entry.getName() , e.getMessage(), e );
                }
                finally
                {
                    IOUtils.closeQuietly( bos );
                    IOUtils.closeQuietly( is );
                }
            }

            logger.info( "Import stores done, hosted: {}, remote: {}, group: {}", hosted, remote, groupCount );
        }
        catch ( IOException e )
        {
            logger.error( "Read the store zip error.", e );
        }

    }

    private void storeArtifactStore( ArtifactStore store ) throws IndyDataException
    {

        storeDataManager.storeArtifactStore( store, new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                                  "Initialize or migrate." ),
                                             true, false, null );
    }
}
