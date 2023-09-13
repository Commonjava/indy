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
package org.commonjava.indy.filer.def;

import org.commonjava.storage.pathmapped.core.FileBasedPhysicalStore;
import org.commonjava.storage.pathmapped.spi.FileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_GENERIC_HTTP;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_NPM;

public class LegacyReadonlyPhysicalStore
                extends FileBasedPhysicalStore
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    /*
     * legacy storage may use different base dir so we can mount it elsewhere like "/mnt/legacy/storage"
     * this is useful as:
     * 1. not mix with new storage;
     * 2. ease back up (legacy volume is readonly, not need to backup);
     * 3. separate from other dirs on the same volume (indy/data, ui) to make the migration easier.
     */
    private final File legacyBaseDir;

    public LegacyReadonlyPhysicalStore( File baseDir, File legacyBaseDir )
    {
        super( baseDir );
        this.legacyBaseDir = legacyBaseDir;
    }

    @Override
    public InputStream getInputStream( String storageFile ) throws IOException
    {
        if ( legacyBaseDir != null && isLegacyFile( storageFile ) )
        {
            File f = new File( legacyBaseDir, storageFile );
            if ( f.isDirectory() || !f.exists() )
            {
                logger.debug( "Target file not exists, file: {}", f.getAbsolutePath() );
                return null;
            }
            return new FileInputStream( f );
        }
        return super.getInputStream( storageFile );
    }

    @Override
    public boolean delete( FileInfo fileInfo )
    {
        String fileStorage = fileInfo.getFileStorage();
        if ( isLegacyFile( fileStorage ) )
        {
            logger.debug( "Skip read-only legacy file: {}", fileStorage );
            return true;
        }
        return super.delete( fileInfo );
    }

    @Override
    public boolean exists( String storageFile )
    {
        if ( legacyBaseDir != null && isLegacyFile( storageFile ) )
        {
            return new File( legacyBaseDir, storageFile ).exists();
        }
        return super.exists( storageFile );
    }

    //Legacy folders: generic-http, maven, npm
    private boolean isLegacyFile( String fileStorage )
    {
        return fileStorage != null && ( fileStorage.startsWith( PKG_TYPE_MAVEN ) || fileStorage.startsWith(
                        PKG_TYPE_GENERIC_HTTP ) || fileStorage.startsWith( PKG_TYPE_NPM ) );
    }
}
