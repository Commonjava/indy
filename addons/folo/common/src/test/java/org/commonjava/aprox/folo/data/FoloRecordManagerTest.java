package org.commonjava.aprox.folo.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.commonjava.aprox.folo.conf.FoloConfig;
import org.commonjava.aprox.folo.model.StoreEffect;
import org.commonjava.aprox.folo.model.TrackedContentRecord;
import org.commonjava.aprox.folo.model.TrackingKey;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.aprox.subsys.datafile.DataFileManager;
import org.commonjava.aprox.subsys.datafile.change.DataFileEventManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FoloRecordManagerTest
{
    private FoloRecordManager recordManager;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Before
    public void setup()
        throws Exception
    {
        final DataFileManager dataFileManager = new DataFileManager( temp.newFolder(), new DataFileEventManager() );
        final AproxObjectMapper objectMapper = new AproxObjectMapper( false );

        final FoloConfig config = new FoloConfig( (int) TimeUnit.SECONDS.convert( 2, TimeUnit.MINUTES ) );
        final FoloRecordCache cache = new FoloRecordCache( dataFileManager, objectMapper, config )
        {
            @Override
            public File getFile( final TrackingKey key )
            {
                return super.getFile( key );
            }

            @Override
            public void write( final TrackedContentRecord record )
            {
                super.write( record );
            }
        };
        cache.buildCache();

        recordManager = new FoloRecordManager( cache );
    }

    @Test
    public void recordArtifactCreatesNewRecordIfNoneExist()
        throws Exception
    {
        final TrackingKey key = newKey();
        assertThat( recordManager.hasRecord( key ), equalTo( false ) );

        final TrackedContentRecord record =
            recordManager.recordArtifact( key, new StoreKey( StoreType.remote, "foo" ), "/path", StoreEffect.DOWNLOAD );

        assertThat( record, notNullValue() );
        assertThat( recordManager.hasRecord( key ), equalTo( true ) );
    }

    @Test
    public void clearRecordDeletesRecord()
        throws Exception
    {
        final TrackingKey key = newKey();
        assertThat( recordManager.hasRecord( key ), equalTo( false ) );

        final TrackedContentRecord record =
            recordManager.recordArtifact( key, new StoreKey( StoreType.remote, "foo" ), "/path", StoreEffect.DOWNLOAD );

        assertThat( record, notNullValue() );
        assertThat( recordManager.hasRecord( key ), equalTo( true ) );

        recordManager.clearRecord( key );
        assertThat( recordManager.hasRecord( key ), equalTo( false ) );
        assertThat( recordManager.getRecord( key ), nullValue() );
    }

    @Test
    public void getRecordReturnsNullIfNoneExists()
        throws Exception
    {
        final TrackingKey key = newKey();
        assertThat( recordManager.getRecord( key ), nullValue() );
    }

    @Test
    public void hasRecordReturnsFalseIfNoneExists()
        throws Exception
    {
        final TrackingKey key = newKey();
        assertThat( recordManager.hasRecord( key ), equalTo( false ) );
    }

    private TrackingKey newKey()
    {
        final String id = "track";

        return new TrackingKey( id );
    }

}
