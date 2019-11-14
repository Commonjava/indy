/**
 * Copyright (C) 2013 Red Hat, Inc.
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.commonjava.indy.pathmap.migrate.Util.FAILED_PATHS_FILE;
import static org.commonjava.indy.pathmap.migrate.Util.STATUS_FILE;
import static org.commonjava.indy.pathmap.migrate.Util.TODO_FILES_DIR;

public class MigrateCmd
        implements Command
{
    private CassandraMigrator migrator;

    private AtomicInteger processedCount = new AtomicInteger( 0 );

    static final Predicate<Path> WORKING_FILES_FILTER =
            p -> Files.isRegularFile( p ) && p.getFileName().toString().startsWith( TODO_FILES_DIR );

    @Override
    public void run( final MigrateOptions options )
            throws MigrateException
    {
        init( options );
        migrator = options.getMigrator();

        final List<String> failedPaths = new ArrayList<>( options.getBatchSize() );

        try
        {
            Files.walk( Paths.get( options.getToDoDir() ), 1 ).filter( WORKING_FILES_FILTER ).forEach( p -> {
                List<String> paths = null;
                try (InputStream is = new FileInputStream( p.toFile() ))
                {
                    paths = IOUtils.readLines( is );
                    Path processedPath = Paths.get( options.getProcessedDir(), p.getFileName().toString() );
                    Files.move( p, processedPath );
                }
                catch ( IOException e )
                {
                    //FIXME: how to handle this exception?
                    e.printStackTrace();
                }
                if ( paths != null && !paths.isEmpty() )
                {
                    paths.forEach( path -> {
                        try
                        {
                            migrator.migrate( path );
                            processedCount.getAndIncrement();
                        }
                        catch ( MigrateException e )
                        {
                            e.printStackTrace();
                            failedPaths.add( path );
                            if ( failedPaths.size() > Util.DEFAULT_BATCH_SIZE )
                            {
                                storeFailedPaths( options, failedPaths );
                                failedPaths.clear();
                            }
                        }
                    } );
                }
            } );
        }
        catch ( Throwable e )
        {
            throw new MigrateException( "Error: Some error happened!", e );
        }
        finally
        {
            if ( !failedPaths.isEmpty() )
            {
                storeFailedPaths( options, failedPaths );
                failedPaths.clear();
            }
        }

    }

    private void init( MigrateOptions options )
    {
        // Reload last processed paths count
        Path statusFilePath = Paths.get( options.getWorkDir(), STATUS_FILE );
        File statusFile = statusFilePath.toFile();
        if ( statusFile.exists() )
        {
            try (BufferedReader reader = new BufferedReader( new FileReader( statusFile ) ))
            {
                String line = reader.readLine();
                while ( line != null )
                {
                    if ( line.trim().startsWith( "Processed" ) )
                    {
                        this.processedCount.set( Integer.parseInt( line.split( ":" )[1].trim() ) );
                        break;
                    }
                    line = reader.readLine();
                }
                Files.delete( statusFilePath );
            }
            catch ( IOException | NumberFormatException e )
            {
                e.printStackTrace();
            }
        }

        final long period = 15000L;
        // Trigger progress update task.
        new Timer().schedule( new UpdateProgressTask( options ), period, period );
    }

    private void storeFailedPaths( MigrateOptions options, List<String> failedPaths )
    {
        File failedFile = Paths.get( options.getWorkDir(), FAILED_PATHS_FILE ).toFile();
        try
        {
            if ( !failedFile.exists() )
            {
                failedFile.createNewFile();
            }
            try (OutputStream os = new FileOutputStream( failedFile ))
            {
                IOUtils.writeLines( failedPaths, null, os );
            }
        }
        catch ( IOException e )
        {
            //FIXME: how to handle this?
            e.printStackTrace();
        }
    }

    private class UpdateProgressTask
            extends TimerTask
    {
        private final MigrateOptions options;

        UpdateProgressTask( final MigrateOptions options )
        {
            this.options = options;
        }

        @Override
        public void run()
        {
            int currentProcessedCnt = MigrateCmd.this.processedCount.get();
            Path statusFilePath = Paths.get( options.getWorkDir(), STATUS_FILE );
            File statusFile = statusFilePath.toFile();
            int totalCnt = 0;
            if ( statusFile.exists() )
            {
                try (BufferedReader reader = new BufferedReader( new FileReader( statusFile ) ))
                {
                    String line = reader.readLine();
                    while ( line != null )
                    {
                        if ( line.trim().startsWith( "Total" ) )
                        {
                            totalCnt = Integer.parseInt( line.split( ":" )[1].trim() );
                            break;
                        }
                        line = reader.readLine();
                    }
                    Files.delete( statusFilePath );
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
            }

            double progress = (double) currentProcessedCnt / (double) totalCnt;
            String progressString = new DecimalFormat( "##.##" ).format( progress );

            try
            {
                boolean created = statusFile.createNewFile();
                if ( created )
                {
                    try (BufferedWriter writer = new BufferedWriter( new FileWriter( statusFile ) ))
                    {
                        writer.write( String.format( "Total:%s", totalCnt ) );
                        writer.newLine();
                        writer.write( String.format( "Processed:%s", currentProcessedCnt ) );
                        writer.newLine();
                        writer.write( String.format( "Progress:%s", progressString ) + "%" );
                    }
                }
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }

        }
    }
}
