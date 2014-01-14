package org.commonjava.aprox.bind.vertx.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.commonjava.util.logging.Log4jUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.file.AsyncFile;
import org.vertx.java.core.impl.DefaultVertx;

public class RequestBodyInputStreamTest
{

    private static final String BASE = "RequestBodyInputStream";

    @BeforeClass
    public static void setupClass()
    {
        Log4jUtil.configure( Level.DEBUG );
    }

    @Test
    public void readSimpleFileViaAsyncFile()
        throws IOException
    {
        final FileHandler fh = new FileHandler();

        final DefaultVertx v = new DefaultVertx();
        v.setContext( v.createEventLoopContext() );
        v.fileSystem()
         .open( getResource( BASE, "test-read.txt" ), fh );

        synchronized ( fh )
        {
            try
            {
                fh.wait();
            }
            catch ( final InterruptedException e )
            {
                return;
            }
        }

        InputStream stream = null;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try
        {
            stream = new VertXInputStream( fh.af );
            IOUtils.copy( stream, baos );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
        }

        assertThat( new String( baos.toByteArray() ), equalTo( "This is a test!" ) );
    }

    @Test
    public void closeFileBeforeReadingAll()
        throws IOException
    {
        final FileHandler fh = new FileHandler();

        final DefaultVertx v = new DefaultVertx();
        v.setContext( v.createEventLoopContext() );
        v.fileSystem()
         .open( getResource( BASE, "test-early-close.txt" ), fh );

        synchronized ( fh )
        {
            try
            {
                fh.wait();
            }
            catch ( final InterruptedException e )
            {
                return;
            }
        }

        InputStream stream = null;
        try
        {
            stream = new VertXInputStream( fh.af );

            final byte[] buf = new byte[15];
            stream.read( buf );

            assertThat( new String( buf ), equalTo( "This is a test!" ) );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
        }
    }

    @Test
    public void closeFileBeforeReadingAny()
        throws IOException
    {
        final FileHandler fh = new FileHandler();

        final DefaultVertx v = new DefaultVertx();
        v.setContext( v.createEventLoopContext() );
        v.fileSystem()
         .open( getResource( BASE, "test-early-close.txt" ), fh );

        synchronized ( fh )
        {
            try
            {
                fh.wait();
            }
            catch ( final InterruptedException e )
            {
                return;
            }
        }

        InputStream stream = null;
        try
        {
            stream = new VertXInputStream( fh.af );
            //
            //            final byte[] buf = new byte[15];
            //            stream.read( buf );
            //
            //            assertThat( new String( buf ), equalTo( "This is a test!" ) );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
        }
    }

    private String getResource( final String base, final String... parts )
    {
        final String path = Paths.get( base, parts )
                                 .toString();

        final URL resource = Thread.currentThread()
                                   .getContextClassLoader()
                                   .getResource( path );

        return resource.getPath();
    }

    private static final class FileHandler
        implements Handler<AsyncResult<AsyncFile>>
    {
        private AsyncFile af;

        @Override
        public synchronized void handle( final AsyncResult<AsyncFile> event )
        {
            af = event.result();
            notifyAll();
        }
    }

}
