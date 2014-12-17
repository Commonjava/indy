package org.commonjava.aprox.folo.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.folo.model.AffectedStoreRecord;
import org.commonjava.aprox.folo.model.StoreEffect;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AffectedStoreRecordTest
{

    private ObjectMapper mapper;

    @Before
    public void setup()
    {
        mapper = new AproxObjectMapper( false );
    }

    @Test
    public void roundTripEmptyRecordToJson()
        throws Exception
    {
        final StoreType type = StoreType.group;
        final String name = "test-group";

        final AffectedStoreRecord record = new AffectedStoreRecord( new StoreKey( type, name ) );

        final String json = mapper.writeValueAsString( record );
        System.out.println( json );

        final AffectedStoreRecord result = mapper.readValue( json, AffectedStoreRecord.class );

        assertThat( result, notNullValue() );
        assertThat( result.getKey(), equalTo( record.getKey() ) );
        assertThat( result.getDownloadedPaths(), nullValue() );
        assertThat( result.getUploadedPaths(), nullValue() );
    }

    @Test
    public void roundTripRecordWithTwoDownloadsToJson()
        throws Exception
    {
        final StoreType type = StoreType.group;
        final String name = "test-group";

        final AffectedStoreRecord record = new AffectedStoreRecord( new StoreKey( type, name ) );
        record.add( "/path/one", StoreEffect.DONWLOAD );
        record.add( "/path/two", StoreEffect.DONWLOAD );

        final String json = mapper.writeValueAsString( record );
        System.out.println( json );

        final AffectedStoreRecord result = mapper.readValue( json, AffectedStoreRecord.class );

        assertThat( result, notNullValue() );
        assertThat( result.getKey(), equalTo( record.getKey() ) );
        assertThat( result.getDownloadedPaths(), equalTo( record.getDownloadedPaths() ) );
        assertThat( result.getUploadedPaths(), nullValue() );
    }

    @Test
    public void roundTripRecordWithTwoUploadsToJson()
        throws Exception
    {
        final StoreType type = StoreType.group;
        final String name = "test-group";

        final AffectedStoreRecord record = new AffectedStoreRecord( new StoreKey( type, name ) );
        record.add( "/path/one", StoreEffect.UPLOAD );
        record.add( "/path/two", StoreEffect.UPLOAD );

        final String json = mapper.writeValueAsString( record );
        System.out.println( json );

        final AffectedStoreRecord result = mapper.readValue( json, AffectedStoreRecord.class );

        assertThat( result, notNullValue() );
        assertThat( result.getKey(), equalTo( record.getKey() ) );
        assertThat( result.getDownloadedPaths(), nullValue() );
        assertThat( result.getUploadedPaths(), equalTo( record.getUploadedPaths() ) );
    }

    @Test
    public void roundTripRecordWithOneDownloadAndOneUploadToJson()
        throws Exception
    {
        final StoreType type = StoreType.group;
        final String name = "test-group";

        final AffectedStoreRecord record = new AffectedStoreRecord( new StoreKey( type, name ) );
        record.add( "/path/one", StoreEffect.DONWLOAD );
        record.add( "/path/two", StoreEffect.UPLOAD );

        final String json = mapper.writeValueAsString( record );
        System.out.println( json );

        final AffectedStoreRecord result = mapper.readValue( json, AffectedStoreRecord.class );

        assertThat( result, notNullValue() );
        assertThat( result.getKey(), equalTo( record.getKey() ) );
        assertThat( result.getDownloadedPaths(), equalTo( record.getDownloadedPaths() ) );
        assertThat( result.getUploadedPaths(), equalTo( record.getUploadedPaths() ) );
    }

    @Test
    public void uniqueDownloadPaths()
        throws Exception
    {
        final StoreType type = StoreType.group;
        final String name = "test-group";

        final AffectedStoreRecord record = new AffectedStoreRecord( new StoreKey( type, name ) );
        record.add( "/path/one", StoreEffect.DONWLOAD );
        record.add( "/path/one", StoreEffect.DONWLOAD );

        assertThat( record.getDownloadedPaths()
                          .size(), equalTo( 1 ) );
    }

    @Test
    public void uniqueUploadPaths()
        throws Exception
    {
        final StoreType type = StoreType.group;
        final String name = "test-group";

        final AffectedStoreRecord record = new AffectedStoreRecord( new StoreKey( type, name ) );
        record.add( "/path/one", StoreEffect.UPLOAD );
        record.add( "/path/one", StoreEffect.UPLOAD );

        assertThat( record.getUploadedPaths()
                          .size(), equalTo( 1 ) );
    }

    @Test
    public void recordDownloadsSorted()
        throws Exception
    {
        final StoreType type = StoreType.group;
        final String name = "test-group";

        final AffectedStoreRecord record = new AffectedStoreRecord( new StoreKey( type, name ) );
        record.add( "/path/two", StoreEffect.DONWLOAD );
        record.add( "/path/one", StoreEffect.DONWLOAD );

        assertThat( record.getDownloadedPaths()
                          .iterator()
                          .next(), equalTo( "/path/one" ) );
    }

    @Test
    public void recordUploadsSorted()
        throws Exception
    {
        final StoreType type = StoreType.group;
        final String name = "test-group";

        final AffectedStoreRecord record = new AffectedStoreRecord( new StoreKey( type, name ) );
        record.add( "/path/two", StoreEffect.UPLOAD );
        record.add( "/path/one", StoreEffect.UPLOAD );

        assertThat( record.getUploadedPaths()
                          .iterator()
                          .next(), equalTo( "/path/one" ) );
    }

    @Test
    public void dontRecordNullDownload()
        throws Exception
    {
        final StoreType type = StoreType.group;
        final String name = "test-group";

        final AffectedStoreRecord record = new AffectedStoreRecord( new StoreKey( type, name ) );
        record.add( null, StoreEffect.DONWLOAD );

        assertThat( record.getDownloadedPaths(), nullValue() );
    }

    @Test
    public void dontRecordNullUpload()
        throws Exception
    {
        final StoreType type = StoreType.group;
        final String name = "test-group";

        final AffectedStoreRecord record = new AffectedStoreRecord( new StoreKey( type, name ) );
        record.add( null, StoreEffect.UPLOAD );

        assertThat( record.getUploadedPaths(), nullValue() );
    }

}
