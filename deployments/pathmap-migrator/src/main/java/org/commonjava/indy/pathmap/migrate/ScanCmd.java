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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.commonjava.indy.pathmap.migrate.Util.TODO_FILES_DIR;
import static org.commonjava.indy.pathmap.migrate.Util.prepareWorkingDir;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_GENERIC_HTTP;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_NPM;

public class ScanCmd
        implements Command
{

    private void init( MigrateOptions options )
            throws MigrateException
    {
        try
        {
            prepareWorkingDir( options.getWorkDir() );
        }
        catch ( IOException e )
        {
            throw new MigrateException( "Error: can not prepare work dir.", e );
        }
    }

    public void run( MigrateOptions options )
            throws MigrateException
    {
        init( options );

        final long start = System.currentTimeMillis();
        final List<String> pkgFolderPaths;
        pkgFolderPaths = listValidPkgFolders( options.getBaseDir() );

        final AtomicInteger total = new AtomicInteger( 0 );

        final ExecutorService executor = Executors.newFixedThreadPool( pkgFolderPaths.size() );
        final CountDownLatch latch = new CountDownLatch( pkgFolderPaths.size() );
        pkgFolderPaths.forEach( p -> executor.execute( () -> {
            try
            {
                int totalForPkg = listFiles( p, options );
                total.addAndGet( totalForPkg );
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }
            finally
            {
                System.out.println( String.format( "%s: %s scan finished", Thread.currentThread().getName(), p ) );
                latch.countDown();
            }
        } ) );
        try
        {
            latch.await();
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
        executor.shutdownNow();
        final long end = System.currentTimeMillis();
        System.out.println( "\n\n" );
        System.out.println( String.format( "File Scan completed, there are %s files need to migrate.", total.get() ) );
        System.out.println( String.format( "Time consumed: %s seconds", ( end - start ) / 1000 ) );

        try
        {
            storeTotal( total.get(), options );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
    }

    private List<String> listValidPkgFolders( final String baseDir )
    {
        final List<String> pkgPaths = new ArrayList<>( 3 );
        for ( String pkg : Arrays.asList( PKG_TYPE_GENERIC_HTTP, PKG_TYPE_MAVEN, PKG_TYPE_NPM ) )
        {
            if ( Files.isDirectory( Paths.get( baseDir, pkg ) ) )
            {
                pkgPaths.add( Paths.get( baseDir, pkg ).toString() );
            }
        }
        return pkgPaths;
    }

    private int listFiles( final String pkgDir, final MigrateOptions options )
            throws IOException
    {
        final Path repoPath = Paths.get( pkgDir );
        final String pkgName = repoPath.getName( repoPath.getNameCount() - 1 ).toString();
        final String todoPrefix = TODO_FILES_DIR + "-" + pkgName;
        System.out.println( String.format( "Start to scan package %s for files", pkgDir ) );
        final List<String> filePaths = new ArrayList<>( options.getBatchSize() );
        final AtomicInteger batchNum = new AtomicInteger( 0 );
        final AtomicInteger totalFileNum = new AtomicInteger( 0 );
        Files.walk( repoPath, Integer.MAX_VALUE ).filter( Files::isRegularFile ).forEach( p -> {
            filePaths.add( p.toString() );
            if ( filePaths.size() >= options.getBatchSize() )
            {
                storeBatchToFile( filePaths, options.getToDoDir(), todoPrefix, batchNum.get() );
                totalFileNum.addAndGet( filePaths.size() );
                filePaths.clear();
                batchNum.getAndIncrement();
            }
        } );

        if ( !filePaths.isEmpty() )
        {
            storeBatchToFile( filePaths, options.getToDoDir(), todoPrefix, batchNum.get() );
            totalFileNum.addAndGet( filePaths.size() );
            filePaths.clear();
        }
        System.out.println(
                String.format( "There are %s files in package path %s to migrate", totalFileNum.get(), pkgDir ) );
        return totalFileNum.get();
    }

    //    private List<String> listPaths( final String rootPath, final int maxDepth, final int initFactor,
    //                                    final Predicate<Path> filter )
    //            throws IOException
    //    {
    //        final List<String> allReposPath = new ArrayList<>( 2000 );
    //        final Path base = Paths.get( rootPath );
    //        Files.walk( base, maxDepth ).filter( filter ).forEach( p -> allReposPath.add( p.toString() ) );
    //        return allReposPath;
    //    }

    private void storeBatchToFile( final List<String> filePaths, final String todoDir, final String prefix,
                                   final int batch )
    {
        final String batchFileName = prefix + "-" + "batch-" + batch + ".txt";
        final Path batchFilePath = Paths.get( todoDir, batchFileName );
        System.out.println(
                String.format( "Start to store paths for batch %s to file %s for %s", batch, batchFileName, todoDir ) );
        try (OutputStream os = new FileOutputStream( batchFilePath.toFile() ))
        {
            IOUtils.writeLines( filePaths, null, os );
        }
        catch ( IOException e )
        {
            System.out.println( String.format( "Error: Cannot write paths to files for batch %s", batchFileName ) );
        }
        System.out.println( String.format( "Batch %s to file %s for %s finished", batch, batchFileName, todoDir ) );
        //        generatedFilePaths.add( batchFileName );
    }

    private void storeTotal( final int totalNum, final MigrateOptions options )
            throws IOException
    {
        final File f = options.getStatusFile();
        try (FileOutputStream os = new FileOutputStream( f ))
        {
            IOUtils.write( String.format( "Total:%s", totalNum ), os );
        }
    }

}
