/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.datafile.DataFile;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.indy.flat.data.DataFileStoreConstants.INDY_STORE;

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

    /**
     * TODO: dump the repo definitions as they exist in the StoreDataManager instead.
     * Currently, those are the same thing, but when we move to a cluster-enabled Indy implementation we're
     * going to need to escape the filesystem for things like repo definition storage, and use an ISPN cache
     * or similar instead.
     */
    private void zipRepositoryFiles( ZipOutputStream zip ) throws IOException
    {
        DataFile[] packageDirs = dataFileManager.getDataFile( INDY_STORE ).listFiles( ( f ) -> true );
        for ( DataFile pkgDir : packageDirs )
        {
            String pkgDirName = REPOS_DIR + "/" + pkgDir.getName();
            for ( StoreType type : StoreType.values() )
            {
                String typeDirName = pkgDirName + "/" + type.singularEndpointName();
                DataFile[] files = pkgDir.getChild( type.singularEndpointName() ).listFiles( f -> true );
                if ( files != null )
                {
                    for ( DataFile f : files )
                    {
                        final String json = f.readString();
                        String name = typeDirName + "/" + f.getName();
                        logger.debug( "Adding {} to repo zip", name );
                        zip.putNextEntry( new ZipEntry( name ) );
                        IOUtils.copy( toInputStream( json ), zip );
                    }
                }
            }
        }
    }

    private File createTempFile( String name ) throws IOException
    {
        return File.createTempFile(
                        "indy-" + name + "." + new SimpleDateFormat( "yyyy-MM-dd-hh-mm-ss.SSSZ" ).format( new Date() ),
                        ".zip" );

    }
}
