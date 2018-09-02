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
package org.commonjava.indy.hostedbyarc;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.hostedbyarc.config.HostedByArchiveConfig;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.commonjava.indy.util.ApplicationStatus;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.TransferOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@ApplicationScoped
public class HostedByArchiveManager
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    public static final String TMP_DIR = StringUtils.defaultIfBlank( System.getProperty( "java.io.tmpdir" ), "/tmp" );

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private ContentManager contentManager;

    @Inject
    private HostedByArchiveConfig config;

    @Inject
    @WeftManaged
    @ExecutorConfig( named = "Hosted-by-arc-executor", priority = 1, threads = 8 )
    private ExecutorService executors;

    public HostedRepository createStoreByArc( final InputStream fileInput, final String repoName, final String user,
                                              final String pathPrefix, final boolean deleteFilesAfterUnzip )
            throws IndyWorkflowException
    {
        final String unzipPath = HostedByArchiveManager.TMP_DIR + "/" + System.currentTimeMillis();
        final List<String> unzippedFiles;
        try
        {
            unzippedFiles = unzip( fileInput, unzipPath );
        }
        catch ( IOException e )
        {
            throw new IndyWorkflowException( "Archive de-compressing failed!", e );
        }

        final HostedRepository repo = createHostedByName( repoName, user, "Create hosted by zip." );

        storeUnzippedFiles( repo, unzippedFiles, unzipPath, pathPrefix );

        try
        {
            if ( deleteFilesAfterUnzip )
            {
                deleteDirectory( new File( unzipPath ) );
            }
        }
        catch ( IOException e )
        {
            logger.warn( "Delete unzipped files failed! Error: {}", e.getMessage() );
        }

        return repo;
    }

    private List<String> unzip( final InputStream zipStream, final String unzipPath )
            throws IOException
    {
        final List<String> filePaths = new ArrayList<>();
        final File unzipDir = new File( unzipPath );
        if ( unzipDir.exists() )
        {
            deleteDirectory( unzipDir );
        }

        if ( unzipDir.mkdirs() )
        {
            try (ZipInputStream zis = new ZipInputStream( zipStream ))
            {
                ZipEntry zipEntry = zis.getNextEntry();
                while ( zipEntry != null )
                {
                    String fileName = zipEntry.getName();
                    File newFile = new File( unzipPath + "/" + fileName );
                    if ( zipEntry.isDirectory() )
                    {
                        newFile.mkdirs();
                    }
                    else
                    {
                        filePaths.add( fileName );
                        if ( !newFile.getParentFile().exists() )
                        {
                            newFile.getParentFile().mkdirs();
                        }
                        try (FileOutputStream fos = new FileOutputStream( newFile ))
                        {
                            IOUtils.copy( zis, fos );
                        }
                    }
                    zipEntry = zis.getNextEntry();
                }
                zis.closeEntry();
            }
        }

        logger.debug( "Unzipped file list: {}", filePaths );

        return filePaths;
    }

    private HostedRepository createHostedByName( final String repoName, final String user, final String changeLog )
            throws IndyWorkflowException
    {
        final HostedRepository hosted = new HostedRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, repoName );
        String storeChangeLog = changeLog;
        if ( StringUtils.isBlank( changeLog ) )
        {
            storeChangeLog = "Changelog not provided";
        }

        final ChangeSummary summary = new ChangeSummary( user, storeChangeLog );

        logger.trace( "Persisting hosted store: {} using: {}", hosted, storeDataManager );
        try
        {

            if ( storeDataManager.storeArtifactStore( hosted, summary, true, true, new EventMetadata() ) )
            {
                return hosted;
            }
            else
            {
                throw new IndyWorkflowException( ApplicationStatus.SERVER_ERROR.code(), "Failed to store: {}. ",
                                                 hosted.getKey() );
            }
        }
        catch ( final IndyDataException e )
        {
            throw new IndyWorkflowException( ApplicationStatus.SERVER_ERROR.code(), "Failed to store: {}. Reason: {}",
                                             e, hosted.getKey(), e.getMessage() );
        }
    }

    private void storeUnzippedFiles( final HostedRepository repo, List<String> paths, final String unzippedDir,
                                     final String pathPrefix )
            throws IndyWorkflowException
    {
        final CountDownLatch latch = new CountDownLatch( paths.size() );

        paths.forEach( s -> executors.execute( () -> {
            String path = s.startsWith( "/" ) ? s : "/" + s;
            if ( StringUtils.isNotBlank( pathPrefix ) )
            {
                if ( path.startsWith( pathPrefix ) )
                {
                    path = path.replaceFirst( pathPrefix, "" );
                }
            }
            File f = new File( unzippedDir + "/" + s );
            try
            {
                contentManager.store( repo, path, new FileInputStream( f ), TransferOperation.UPLOAD );
            }
            catch ( IndyWorkflowException | FileNotFoundException e )
            {
                logger.error( "store failed for path {}", s, e );
            }
            finally
            {
                latch.countDown();
            }
        } ) );

        try
        {
            latch.await( config.getLockTimeoutMins(), TimeUnit.MINUTES );
        }
        catch ( InterruptedException e )
        {
            throw new IndyWorkflowException( "File store process interrupted!", e );
        }
    }

    private void deleteDirectory( final File dir )
            throws IOException
    {
        Files.list( dir.toPath() ).forEach( p -> {
            try
            {
                if ( Files.isDirectory( p, LinkOption.NOFOLLOW_LINKS ) )
                {
                    deleteDirectory( p.toFile() );
                }
                else
                {
                    Files.delete( p );
                }
            }
            catch ( IOException e )
            {
                logger.warn( "Delete failed for deleting directory {}!", dir );
            }
        } );
        Files.deleteIfExists( dir.toPath() );
    }

    @Deprecated
    private boolean isZipFileType( final File file )
    {
        if ( file == null || !file.exists() )
        {
            return false;
        }
        try
        {
            return Files.probeContentType( file.toPath() ).contains( "application/zip" );
        }
        catch ( IOException e )
        {
            logger.warn( "zip file check failed. Failed message is {}", e.getMessage() );
            return false;
        }

        // Another way
        //        try
        //        {
        //            ZipFile zipFile = new ZipFile( file );
        //            return true;
        //        }
        //        catch ( IOException e )
        //        {
        //            logger.warn("zip file check failed. Failed message is {}", e.getMessage());
        //            return false
        //        }

    }
}
