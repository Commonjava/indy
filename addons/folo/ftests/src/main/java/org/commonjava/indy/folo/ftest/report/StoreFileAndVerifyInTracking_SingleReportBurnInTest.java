/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.folo.ftest.report;

import ch.qos.logback.classic.Level;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.util.UrlUtils;
import org.commonjava.indy.folo.client.IndyFoloAdminClientModule;
import org.commonjava.indy.folo.client.IndyFoloContentClientModule;
import org.commonjava.indy.folo.dto.TrackedContentDTO;
import org.commonjava.indy.folo.dto.TrackedContentEntryDTO;
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.apache.commons.codec.digest.DigestUtils.shaHex;
import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@Category( EventDependent.class )
public class StoreFileAndVerifyInTracking_SingleReportBurnInTest
    extends AbstractTrackingReportTest
{

    private static final int MEGABYTE = (int) Math.pow( 1024, 2 );

    private static final int MIN_SIZE = 1024;

    private static final int COUNT = 100;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private List<String> words;

    private Random rand = new Random();

    private ExecutorCompletionService<FileEntry> fileWriteService =
            new ExecutorCompletionService<FileEntry>( Executors.newFixedThreadPool(24) );

    private static final class FileEntry
    {
        private File file;
        private String path;
    }

    @Override
    protected int getTestTimeoutMultiplier()
    {
        return 4 * super.getTestTimeoutMultiplier();
    }

    @Before
    public void setupTest()
            throws IOException
    {
        ch.qos.logback.classic.Logger wireLogger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger( "org.apache.http.wire" );

        wireLogger.setLevel( Level.INFO );

        try(InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream( "words" ))
        {
            words = IOUtils.readLines( stream );
        }
    }

    @Test
    public void run()
        throws Exception
    {
        final String trackingId = newName();

        writeFiles();
        Map<String, File> catalog = uploadFiles( trackingId );

        IndyFoloAdminClientModule adminModule = client.module( IndyFoloAdminClientModule.class );

        logger.info( "\n\nSealing tracking record.\n\n" );
        boolean success = adminModule.sealTrackingRecord( trackingId );
        assertThat( success, equalTo( true ) );

        logger.info( "\n\nRetrieving tracking record.\n\n" );
        final TrackedContentDTO report = adminModule
                .getTrackingReport( trackingId );
        assertThat( report, notNullValue() );

        logger.info( "\n\nVerifying uploaded content.\n\n" );
        final Set<TrackedContentEntryDTO> uploads = report.getUploads();

        assertThat( uploads, notNullValue() );
        assertThat( uploads.size(), equalTo( COUNT ) );

        assertUploads( uploads, catalog );
    }

    private Map<String, File> uploadFiles( String trackingId )
            throws InterruptedException, ExecutionException
    {
        Map<String, File> catalog = new HashMap<>();
        for ( int i = 0; i < COUNT; i++ )
        {
            FileEntry fileEntry = fileWriteService.take().get();
            File file = fileEntry.file;
            String path = fileEntry.path;
            try
            {
                try (InputStream stream = new FileInputStream( file ))
                {
                    logger.info( "\n\nStoring {}/{} ({} bytes) to:\n    {}\n\n\n", i+1,
                                 COUNT, file.length(), path );
                    client.module( IndyFoloContentClientModule.class ).store( trackingId, hosted, STORE, path, stream );
                }

                catalog.put( path, file );
            }
            catch ( IOException e )
            {
                fail( "Failed to read temp data file: " + file );
            }
            catch ( IndyClientException e )
            {
                fail( "Failed to upload temp data file: " + file );
            }
        }

        return catalog;
    }

    private void assertUploads( Set<TrackedContentEntryDTO> uploads, Map<String, File> catalog )
    {
        AtomicInteger count=new AtomicInteger( 0 );
        uploads.parallelStream().forEach( (entry)->{
            logger.info( "\n\n\nVerifying entry {}/{}:\n    {}\n\n\n", count.incrementAndGet(), uploads.size(),
                         entry == null ? "INVALID" : entry.getPath() );

            assertThat( entry, notNullValue() );

            String path = entry.getPath();
            if ( path.startsWith( "/" ) && path.length() > 0 )
            {
                path = path.substring( 1 );
            }

            File dataFile = catalog.get( path );
            byte[] data = new byte[0];
            try
            {
                data = FileUtils.readFileToByteArray( dataFile );
            }
            catch ( Exception e )
            {
                fail( "Cannot read temp data file: " + dataFile + " for path: " + path );
            }

            assertThat( path + " has wrong host key", entry.getStoreKey(), equalTo( new StoreKey( hosted, STORE ) ) );
            assertThat( path + " has wrong local URL", entry.getLocalUrl(),
                        equalTo( UrlUtils.buildUrl( client.getBaseUrl(), hosted.singularEndpointName(), STORE, path ) ) );

            assertThat( path + " has non-empty origin URL (uploads should NOT have these)", entry.getOriginUrl(), nullValue() );
            try
            {
                assertThat( path + " has wrong MD5", entry.getMd5(), equalTo( md5Hex( data ) ) );
                assertThat( path + " has wrong SHA1", entry.getSha1(), equalTo( shaHex( data ) ) );
                assertThat( path + " has wrong SHA256", entry.getSha256(), equalTo( sha256Hex( data ) ) );
            }
            catch ( Exception e )
            {
                fail( "Failed to compute hashes for: " + path );
            }

            assertThat( path + " has wrong size", entry.getSize(), equalTo( (long) data.length ) );
        } );
    }

    private void writeFiles()
            throws IOException, IndyClientException
    {
        File dir = temp.newFolder();
        for( int f=0; f<COUNT; f++)
        {
            final int idx = f;
            fileWriteService.submit( ()->{
                int size = rand.nextInt( 90 * MEGABYTE ) + MIN_SIZE;

                byte[] data = new byte[size];
                rand.nextBytes( data );

                Supplier<String> randomWord = () -> words.get( rand.nextInt( words.size() - 1 ) );

                String gid = generateString( 5, '/', randomWord );
                String aid = generateString( 4, '-', randomWord );
                String ver = generateString( 3, '.', () -> Integer.toString( rand.nextInt( 99 ) ) ) + "." + idx;

                final String path = String.format( "%1$s/%2$s/%3$s/%2$s-%3$s.jar", gid, aid, ver );

                File contentDir = new File( dir, new File( path).getParent());
                contentDir.mkdirs();

                logger.info( "\n\n\nWriting test temp file {}/{}:\n    {}\n\n\n", idx+1, COUNT, path );

                File file = new File( dir, path );
                FileUtils.writeByteArrayToFile(file, data);

                FileEntry entry = new FileEntry();
                entry.path = path;
                entry.file = file;

                return entry;
            } );
        }
    }

    private String generateString( int segments, char separator, Supplier<String> segSupplier )
    {
        StringBuilder sb = new StringBuilder();
        int sz = rand.nextInt( segments-1 ) + 1;
        for ( int i = 0; i < sz; i++ )
        {
            if ( sb.length() > 0 )
            {
                sb.append( separator );
            }
            sb.append( segSupplier.get() );
        }

        return sb.toString();
    }

}
