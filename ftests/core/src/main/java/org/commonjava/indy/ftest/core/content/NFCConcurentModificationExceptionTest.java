package org.commonjava.indy.ftest.core.content;

import org.commonjava.indy.client.core.module.IndyNfcClientModule;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.dto.NotFoundCacheDTO;
import org.hamcrest.core.IsNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Created by xiabai on 7/14/17.
 */
public class NFCConcurentModificationExceptionTest
                extends AbstractContentManagementTest
{
    private HostedRepository hosted;

    private static final String HOSTED = "hosted";

    private static final String POM_PATH = "org/foo/bar/1/bar-1.pom";

    Random random;

    ExecutorService executor;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Before
    public void setupTest() throws Exception
    {
        String change = "test setup";
        hosted = client.stores().create( new HostedRepository( HOSTED ), change, HostedRepository.class );
        random = new Random();
        executor = Executors.newFixedThreadPool( 20 );
    }

    @After
    public void tearDown()
    {
        //        executor.shutdown();
    }

    @Test
    public void run() throws Exception
    {
        try (InputStream inputStream = client.content().get( hosted.getKey(), POM_PATH ))
        {
            assertThat( inputStream, IsNull.nullValue() );
        }

        //        NotFoundCacheDTO dto = client.module( IndyNfcClientModule.class )
        //                                     .getAllNfcContentInStore( StoreType.hosted, hosted.getName() );

        List<Future<String>> list = new ArrayList<Future<String>>();

        try
        {
            for ( int i = 0; i < 10; i++ )
            {
                Callable callable = new GetAllNfcContent();
                Future<String> future = executor.submit( callable );

                list.add( future );
            }

        }
        catch ( Throwable t )
        {
            logger.info( "NFCConcurentModificationExceptionTest :: " + new Date() + "::" +" getAllNfcContentInStore have a exception :{}", t.getMessage() );
            assertEquals( t.getLocalizedMessage().contains( "500" ), false );
        }

        {
            Callable callable = new ClearAllNfcContent();
            Future<String> future = executor.submit( callable );
        }

        try
        {
            for ( int i = 0; i < 10; i++ )
            {
                Callable callable = new GetAllNfcContent();
                Future<String> future = executor.submit( callable );

                list.add( future );
            }

        }
        catch ( Throwable t )
        {
            logger.info( "NFCConcurentModificationExceptionTest :: " + new Date() + "::" +" getAllNfcContentInStore have a exception :{}", t.getMessage() );
            assertEquals( t.getLocalizedMessage().contains( "500" ), false );
        }


        while ( isDown( list ) )
        {
            Thread.sleep( 1000 );
        }
        //        assertEquals( false, true );

    }

    private class GetAllNfcContent
                    implements Callable
    {
        @Override
        public String call() throws Exception
        {
            Thread.sleep( random.nextInt( 3000 ) );
            logger.info( "NFCConcurentModificationExceptionTest :: " + new Date() + "::" +" GetAllNfcContent is running " );
            NotFoundCacheDTO dto = client.module( IndyNfcClientModule.class )
                                         .getAllNfcContentInStore( StoreType.hosted, hosted.getName() );
            return dto.toString();
        }
    }

    private class ClearAllNfcContent
                    implements Callable
    {
        @Override
        public String call() throws Exception
        {
            Thread.sleep( random.nextInt( 3000 ) );
            logger.info( "NFCConcurentModificationExceptionTest :: ClearAllNfcContent is running " );
            client.module( IndyNfcClientModule.class ).clearAll();
            return "success";
        }
    }

    private boolean isDown( List<Future<String>> list ) throws ExecutionException, InterruptedException
    {
        for ( Future<String> future : list )
        {
            logger.info( "NFCConcurentModificationExceptionTest :: " + new Date() + "::" + future.get() );
            if ( !future.isDone() )
                return true;
        }
        return false;
    }
}
