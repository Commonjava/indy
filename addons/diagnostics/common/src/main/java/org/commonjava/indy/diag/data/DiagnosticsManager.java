package org.commonjava.indy.diag.data;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.FileFilter;
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
import java.util.Iterator;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.apache.commons.lang.StringUtils.join;

/**
 * Created by jdcasey on 1/11/17.
 *
 * Manages collection and packaging of diagnostic information.
 */
@ApplicationScoped
public class DiagnosticsManager
{
    private static final String FILE_LOGGER = "FILE";

    public static final String THREAD_DUMP_FILE = "thread-dump.txt";

    public static final String LOGS_DIR = "logs";

    public String getThreadDumpString()
    {
        StringBuilder sb = new StringBuilder();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo( threadMXBean.getAllThreadIds(), 100 );
        Stream.of( threadInfos ).forEachOrdered( ( ti ) -> {
            if ( sb.length() > 0 )
            {
                sb.append( "\n\n" );
            }

            sb.append( ti.getThreadName() )
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
        File out = File.createTempFile(
                "indy-diags." + new SimpleDateFormat( "yyyy-MM-dd-hh-mm-ss.SSSZ" ).format( new Date() ),  ".zip" );

        org.slf4j.Logger logger = LoggerFactory.getLogger( getClass() );

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
}
