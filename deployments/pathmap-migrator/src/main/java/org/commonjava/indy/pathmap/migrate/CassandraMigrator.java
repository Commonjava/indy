/**
 * Copyright (C) 2013~2019 Red Hat, Inc.
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
package org.commonjava.indy.pathmap.migrate;

import org.apache.commons.io.IOUtils;
import org.commonjava.storage.pathmapped.config.DefaultPathMappedStorageConfig;
import org.commonjava.storage.pathmapped.config.PathMappedStorageConfig;
import org.commonjava.storage.pathmapped.core.FileBasedPhysicalStore;
import org.commonjava.storage.pathmapped.pathdb.datastax.CassandraPathDB;
import org.commonjava.storage.pathmapped.spi.FileInfo;
import org.commonjava.storage.pathmapped.spi.PhysicalStore;
import org.commonjava.storage.pathmapped.util.ChecksumCalculator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Map;

public class CassandraMigrator
{
    private static CassandraMigrator migrator;

    private final CassandraPathDB pathDB;

    private final PhysicalStore physicalStore;

    private final IndyStoreBasedPathGenerator storePathGen;

    private final String baseDir;

    private final boolean dedup;

    private final String dedupAlgo;

    private CassandraMigrator( final PathMappedStorageConfig config, final String baseDir,
                               final boolean dedup, final String dedupAlgo )
    {
        this.pathDB = new CassandraPathDB( config );
        this.storePathGen = new IndyStoreBasedPathGenerator( baseDir );
        this.physicalStore = new FileBasedPhysicalStore( new File( baseDir ) );
        this.dedup = dedup;
        this.dedupAlgo = dedupAlgo;
        this.baseDir = baseDir;
    }

    public static CassandraMigrator getMigrator( final Map<String, Object> cassandraConfig, final String baseDir,
                                                 final boolean dedup, final String dedupAlgo )
    {
        synchronized ( CassandraMigrator.class )
        {
            if ( migrator == null )
            {
                final PathMappedStorageConfig config = new DefaultPathMappedStorageConfig( cassandraConfig );
                migrator = new CassandraMigrator( config, baseDir, dedup, dedupAlgo );
            }
        }
        return migrator;

    }

    public void startUp()
    {

    }

    public void migrate( final String physicalFilePath )
            throws MigrateException
    {

        File file = Paths.get( physicalFilePath ).normalize().toFile();
        if ( !file.exists() || !file.isFile() )
        {
            throw new MigrateException( "Error: the physical path {} does not exists or is not a real file.",
                                        physicalFilePath );
        }

        String checksum = null;
        if ( dedup )
        {
            try
            {
                checksum = calculateChecksum( file );
            }
            catch ( IOException e )
            {
                throw new MigrateException(
                                String.format( "Error: Can not get file checksum for file of %s", physicalFilePath ), e );
            }
        }
        final String fileSystem = storePathGen.generateFileSystem( physicalFilePath );
        final String path = storePathGen.generatePath( physicalFilePath );
        final String storePath = storePathGen.generateStorePath( physicalFilePath );
        FileInfo fileInfo = physicalStore.getFileInfo( fileSystem, path );

        try
        {
            pathDB.insert( fileSystem, path, new Date(), fileInfo.getFileId(), file.length(), storePath, checksum );
        }
        catch ( Exception e )
        {
            throw new MigrateException( "Error: something wrong happened during update path db.", e );
        }
    }

    private String calculateChecksum( File file )
            throws IOException
    {
        if ( !file.exists() || !file.isFile() )
        {
            throw new IOException(
                    String.format( "Digest error: file not exists or not a regular file for file %s", file ) );
        }

        ChecksumCalculator checksumCalculator = null;
        {
            try
            {
                checksumCalculator = new ChecksumCalculator( dedupAlgo );
            }
            catch ( NoSuchAlgorithmException e )
            {
                // verified, ex never happen
            }
        }

        try (FileInputStream is = new FileInputStream( file ))
        {
            checksumCalculator.update( IOUtils.toByteArray( is ) );
        }
        return checksumCalculator.getDigestHex();
    }

    public void shutdown()
    {
        migrator = null;
        pathDB.close();
    }
}
