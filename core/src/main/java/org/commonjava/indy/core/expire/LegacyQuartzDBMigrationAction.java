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
package org.commonjava.indy.core.expire;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.MigrationAction;
import org.commonjava.indy.subsys.datafile.conf.DataFileConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Used to remove legacy quartz db data file as it is not used in new ISPN timeout way.
 */
@Named( "legacy-quartzdb-migration" )
public class LegacyQuartzDBMigrationAction
        implements MigrationAction
{
    @Inject
    private DataFileConfiguration fileConfig;

    private static final String SCHEDULE_DB_NAME = "scheduler";

    public LegacyQuartzDBMigrationAction()
    {

    }

    // Used for unit test only
    LegacyQuartzDBMigrationAction( DataFileConfiguration fileConfig )
    {
        this.fileConfig = fileConfig;
    }

    @Override
    public boolean migrate()
            throws IndyLifecycleException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "Disabled." );
        return true;
    }

    private boolean doMigrate()
            throws IndyLifecycleException
    {
        final Logger logger = LoggerFactory.getLogger( LegacyQuartzDBMigrationAction.class );
        Path path = Paths.get( fileConfig.getDataBasedir().toURI() );
        final Stream<Path> listFiles;
        try
        {
            listFiles = Files.list( path );
        }
        catch ( IOException e )
        {
            logger.warn( "IOException happened when list path {}", path );
            return false;
        }
        if ( listFiles != null )
        {
            listFiles.filter( filePath -> Files.isRegularFile( filePath ) && filePath.getFileName()
                                                                                     .toString()
                                                                                     .startsWith( SCHEDULE_DB_NAME ) )
                     .forEach( file -> {
                         try
                         {
                             Files.delete( file );
                         }
                         catch ( IOException e )
                         {
                             logger.warn( "IOException happened when delete file {}", file );
                         }
                     } );

        }
        return true;
    }

    @Override
    public int getMigrationPriority()
    {
        return 90;
    }

    @Override
    public String getId()
    {
        return "Legacy quartz db data remove";
    }
}
