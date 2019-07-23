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
package org.commonjava.indy.folo.action;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.MigrationAction;
import org.commonjava.indy.folo.ctl.FoloAdminController;
import org.commonjava.indy.folo.data.FoloFiler;
import org.commonjava.indy.folo.data.FoloSealedCache;
import org.commonjava.indy.folo.model.TrackedContent;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.codehaus.plexus.util.FileUtils.rename;

public class RecordsMigrationAction
                implements MigrationAction
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private FoloFiler foloFiler;

    @Inject
    private FoloAdminController controller;

    @FoloSealedCache
    @Inject
    private CacheHandle<TrackingKey, TrackedContent> sealedRecordCache;

    @Override
    public boolean migrate() throws IndyLifecycleException
    {
        File file = foloFiler.getSealedZipFile().getDetachedFile();
        if ( file.exists() )
        {
            logger.info( "Migrate Folo sealed records, file: {}", file );

            // back up folo-sealed.dat
            File dataFile = foloFiler.getSealedDataFile().getDetachedFile();
            if ( dataFile.exists() )
            {
                File dataFileBak = new File( dataFile.getAbsolutePath() + ".bak." + new SimpleDateFormat(
                                "yyyy-MM-dd-hh-mm-ss.SSS" ).format( new Date() ) );
                try
                {
                    logger.info( "Back up dataFile to {}", dataFileBak );
                    rename( dataFile, dataFileBak );
                }
                catch ( IOException e )
                {
                    throw new IndyLifecycleException( "Backup {} to {} failed", dataFile, dataFileBak, e );
                }
            }

            // import
            try (InputStream stream = new FileInputStream( file ))
            {
                controller.importRecordZip( stream );
            }
            catch ( Exception e )
            {
                throw new IndyLifecycleException( "Import sealed records failed", e );
            }

            // rename the zip file to .loaded
            File toFile = new File( file.getAbsolutePath() + ".loaded" );
            try
            {
                rename( file, toFile );
                logger.info( "Rename {} to {}", file, toFile );
            }
            catch ( IOException e )
            {
                throw new IndyLifecycleException( "Rename {} to {} failed", file, toFile, e );
            }
        }
        else
        {
            logger.info( "Folo migration zip file not exist, file: {}", file.getAbsolutePath() );
        }
        return true;
    }

    @Override
    public String getId()
    {
        return "folo-sealed-records";
    }

    @Override
    public int getMigrationPriority()
    {
        return 90;
    }
}
