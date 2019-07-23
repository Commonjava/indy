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
package org.commonjava.indy.hostedbyarc;

import org.apache.commons.lang.StringUtils;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@ApplicationScoped
public class HostedByArchiveManager
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

//    public static final String TMP_DIR = StringUtils.defaultIfBlank( System.getProperty( "java.io.tmpdir" ), "/tmp" );

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private ContentManager contentManager;

    @Inject
    private HostedByArchiveConfig config;

//    @Inject
//    @WeftManaged
//    @ExecutorConfig( named = "Hosted-by-arc-executor", priority = 1, threads = 8 )
//    @Deprecated
//    private ExecutorService executors;

    public HostedRepository createStoreByArc( final InputStream fileInput, final String repoName, final String user,
                                              final String ignoredPrefix )
            throws IndyWorkflowException
    {
        final HostedRepository repo = createHostedByName( repoName, user, "Create hosted by zip." );

        storeZipContentInHosted( fileInput, ignoredPrefix, repo );

        return repo;
    }

    private HostedRepository createHostedByName( final String repoName, final String user, final String changeLog )
            throws IndyWorkflowException
    {
        final HostedRepository hosted = new HostedRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, repoName );
        // I think this new hosted should allow snapshots and releases
        hosted.setAllowSnapshots( true );
        hosted.setAllowReleases( true );
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

    private void storeZipContentInHosted( final InputStream zipStream, final String ignoredPrefix,
                                          final HostedRepository repo )
            throws IndyWorkflowException
    {

        try (ZipInputStream zis = new ZipInputStream( zipStream ))
        {
            ZipEntry zipEntry = zis.getNextEntry();
            while ( zipEntry != null )
            {
                String fileName = zipEntry.getName();
                if ( !zipEntry.isDirectory() )
                {
                    storeStreamWithPath( fileName, ignoredPrefix, zis, repo );
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
        catch ( IOException e )
        {
            throw new IndyWorkflowException( "", e );
        }

    }

    private void storeStreamWithPath( final String rawPath, final String ignoredPrefix, final InputStream input,
                                      final HostedRepository repo )
            throws IndyWorkflowException
    {
        logger.trace( "Raw path is {}, ignored prefix is {}", rawPath, ignoredPrefix );
        String path = rawPath.startsWith( "/" ) ? rawPath : "/" + rawPath;
        logger.trace( "Processed path is {}", path );
        if ( StringUtils.isNotBlank( ignoredPrefix ) && path.startsWith( ignoredPrefix ) )
        {
            path = path.replaceFirst( ignoredPrefix, "" );
        }
        contentManager.store( repo, path, input, TransferOperation.UPLOAD );
    }

}
