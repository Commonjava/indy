package org.commonjava.indy.filer.def;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.maven.galley.cache.FileCacheProvider;
import org.commonjava.maven.galley.event.NoOpFileEventManager;
import org.commonjava.maven.galley.io.HashedLocationPathGenerator;
import org.commonjava.maven.galley.io.TransferDecoratorManager;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.SimpleLocation;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class FileChangeTrackingDecoratorTest
{

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private DefaultStorageProviderConfiguration config;

    private CacheProvider cp;

    private FileChangeTrackingDecorator decorator;

    private void init( int logSize ) throws IOException
    {
        config = new DefaultStorageProviderConfiguration();
        config.setStorageRootDirectory( temp.newFolder( "storage-root" ) );
        config.setChangeTrackingRollSize( logSize );

        decorator = new FileChangeTrackingDecorator( config );

        cp = new FileCacheProvider( config.getStorageRootDirectory(), new HashedLocationPathGenerator(),
                                         new NoOpFileEventManager(), new TransferDecoratorManager( decorator ),
                                         false );
    }

    @Test
    public void decorateWrite() throws IOException
    {
        init( 10 );
        Transfer foo = cp.getTransfer( new ConcreteResource( new SimpleLocation( "foo", "http://foo.bar" ),
                                                             "path/to/foo.txt" ) );

        byte[] data = "This is a test".getBytes();
        try(OutputStream os = foo.openOutputStream( TransferOperation.UPLOAD ))
        {
            os.write( data );
        }

        verifyLogFileCount( 1 );
    }

    @Test
    public void testLogSizeRolling() throws IOException
    {
        init( 1 );
        for ( String path : Arrays.asList( "foo.txt", "bar.txt" ) )
        {
            Transfer foo = cp.getTransfer( new ConcreteResource( new SimpleLocation( "foo", "http://foo.bar" ),
                                                                 "path/to/" + path ) );

            byte[] data = "This is a test".getBytes();
            try(OutputStream os = foo.openOutputStream( TransferOperation.UPLOAD ))
            {
                os.write( data );
            }
        }

        verifyLogFileCount( 2 );
    }

    private void verifyLogFileCount( int total ) throws IOException
    {
        File[] files = Paths.get( config.getChangeTrackingDirectory() ).toFile().listFiles();

        List<String> allLines = new ArrayList<>();
        for ( File file : files )
        {
            if ( file.isDirectory() )
            {
                for ( File child : file.listFiles() )
                {
                    List<String> lines = FileUtils.readLines( child );
                    System.out.println( child + ": " + lines );
                    allLines.addAll( lines );
                }
            }
            else
            {
                List<String> lines = FileUtils.readLines( file );
                System.out.println( file + ": " + lines );
                allLines.addAll( lines );
            }
        }

        System.out.println( allLines );
        assertThat( StringUtils.join( allLines, "\n" ), allLines.size(), equalTo( total ) );
    }

    @Test
    public void decorateCopyFrom() throws IOException
    {
        init( 1 );
        SimpleLocation loc = new SimpleLocation( "foo", "http://foo.bar" );
        ConcreteResource from = new ConcreteResource( loc, "path/to/foo.txt" );
        Transfer fromTx = cp.getTransfer( from );

        ConcreteResource to = new ConcreteResource( loc, "path/to/bar.txt" );
        Transfer toTx = cp.getTransfer( to );

        byte[] data = "This is a test".getBytes();
        try (OutputStream os = fromTx.openOutputStream( TransferOperation.UPLOAD ))
        {
            os.write( data );
        }

        toTx.copyFrom( fromTx );
        verifyLogFileCount( 2 );
    }

    @Test
    public void decorateDelete() throws IOException
    {
        init( 1 );
        SimpleLocation loc = new SimpleLocation( "foo", "http://foo.bar" );
        ConcreteResource res = new ConcreteResource( loc, "path/to/foo.txt" );
        Transfer tx = cp.getTransfer( res );

        byte[] data = "This is a test".getBytes();
        try(OutputStream os = tx.openOutputStream( TransferOperation.UPLOAD ))
        {
            os.write( data );
        }

        tx.delete();
        verifyLogFileCount( 2 );
    }

    @Test
    public void decorateCreateFile() throws IOException
    {
        init( 1 );
        SimpleLocation loc = new SimpleLocation( "foo", "http://foo.bar" );
        ConcreteResource res = new ConcreteResource( loc, "path/to/foo.txt" );
        Transfer tx = cp.getTransfer( res );

        tx.mkdirs();
        tx.createFile();
        verifyLogFileCount( 1 );
    }
}