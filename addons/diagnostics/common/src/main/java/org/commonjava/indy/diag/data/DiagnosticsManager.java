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
package org.commonjava.indy.diag.data;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import org.apache.commons.io.IOUtils;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.apache.commons.lang.StringUtils.join;

/**
 * Created by jdcasey on 1/11/17.
 *
 * Manages collection and packaging of diagnostic information.
 */
@ApplicationScoped
public class DiagnosticsManager
{
    org.slf4j.Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String FILE_LOGGER = "FILE";

    public static final String THREAD_DUMP_FILE = "thread-dump.txt";

    public static final String LOGS_DIR = "logs";

    public static final String REPOS_DIR = "repos";

    @Inject
    private DataFileManager dataFileManager;

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private IndyObjectMapper serializer;

    public String getThreadDumpString()
    {
        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate( threads );

        Map<Long, Thread> threadMap = new HashMap<>();
        Stream.of( threads ).forEach( t -> threadMap.put( t.getId(), t ) );
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo( threadMXBean.getAllThreadIds(), 100 );

        StringBuilder sb = new StringBuilder();
        Stream.of( threadInfos ).forEachOrdered( ( ti ) -> {
            if ( sb.length() > 0 )
            {
                sb.append( "\n\n" );
            }

            String threadGroup = "Unknown";
            Thread t = threadMap.get(ti.getThreadId());
            if ( t != null )
            {
                ThreadGroup tg = t.getThreadGroup();
                if ( tg != null )
                {
                    threadGroup = tg.getName();
                }
            }

            sb.append( ti.getThreadName() )
              .append("\n  Group: ")
              .append( threadGroup )
              .append( "\n  State: " )
              .append( ti.getThreadState() )
              .append( "\n  Lock Info: " )
              .append( ti.getLockInfo() )
              .append( "\n  Monitors:" );

            MonitorInfo[] monitors = ti.getLockedMonitors();
            if ( monitors == null || monitors.length < 1 )
            {
                sb.append( "  -NONE-" );
            }
            else
            {
                sb.append( "\n  - " ).append( join( monitors, "\n  - " ) );
            }

            sb.append( "\n  Trace:\n    " ).append( join( ti.getStackTrace(), "\n    " ) );

        } );

        return sb.toString();
    }

    public File getDiagnosticBundle()
            throws IOException
    {
        File out = createTempFile( "diags" );
        Logger rootLogger = (Logger) LoggerFactory.getLogger( "ROOT" );

        logger.info( "Writing diagnostic bundle to: '{}'", out );

        Appender<ILoggingEvent> appender = rootLogger.getAppender( FILE_LOGGER );
        if ( appender != null && (appender instanceof FileAppender) )
        {
            try(ZipOutputStream zip = new ZipOutputStream( new FileOutputStream( out ) ) )
            {
                File dir = new File( ( (FileAppender) appender ).getFile() ).getParentFile();

                for ( File f : dir.listFiles( file -> file.getName().endsWith( ".log" ) ) )
                {
                    String name = LOGS_DIR + "/" + f.getName();
                    logger.info( "Adding {} to bundle zip: {}", name, out );

                    zip.putNextEntry( new ZipEntry( name ) );
                    try(InputStream in = new FileInputStream( f ) )
                    {
                        IOUtils.copy( in, zip );
                    }
                }

                logger.info( "Adding thread dump to bundle zip: {}", out );
                zip.putNextEntry( new ZipEntry( THREAD_DUMP_FILE ) );
                zip.write( getThreadDumpString().getBytes() );
                zip.flush();
                zip.close();
            }
        }
        else
        {
            return null;
        }

        return out;
    }

    public File getRepoBundle() throws IOException
    {
        File out = createTempFile( "repos" );
        logger.info( "Writing repo bundle to: '{}'", out );

        try (ZipOutputStream zip = new ZipOutputStream( new FileOutputStream( out ) ))
        {
            zipRepositoryFiles( zip );
        }
        return out;
    }

    private void zipRepositoryFiles( ZipOutputStream zip ) throws IOException
    {
        Set<ArtifactStore> stores = null;
        try
        {
            stores = storeDataManager.getAllArtifactStores();
        }
        catch ( IndyDataException e )
        {
            logger.error( "Failed to get stores definition", e );
            throw new IOException( e );
        }

        for ( ArtifactStore store : stores )
        {
            String path = Paths.get( REPOS_DIR, store.getPackageType(), store.getType().singularEndpointName(),
                                     store.getName() ).toString();
            logger.debug( "Adding {} to repo zip", path );
            zip.putNextEntry( new ZipEntry( path ) );
            String json = serializer.writeValueAsString( store );
            IOUtils.copy( toInputStream( json ), zip );
        }
    }

    private File createTempFile( String name ) throws IOException
    {
        return File.createTempFile(
                        "indy-" + name + "." + new SimpleDateFormat( "yyyy-MM-dd-hh-mm-ss.SSSZ" ).format( new Date() ),
                        ".zip" );

    }
}
