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
package org.commonjava.indy.folo.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AffectedStoreRecordTest
{

    private ObjectMapper mapper;

    @Before
    public void setup()
    {
        mapper = new IndyObjectMapper( false );
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
        record.add( "/path/one", StoreEffect.DOWNLOAD );
        record.add( "/path/two", StoreEffect.DOWNLOAD );

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
        record.add( "/path/one", StoreEffect.DOWNLOAD );
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
        record.add( "/path/one", StoreEffect.DOWNLOAD );
        record.add( "/path/one", StoreEffect.DOWNLOAD );

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
        record.add( "/path/two", StoreEffect.DOWNLOAD );
        record.add( "/path/one", StoreEffect.DOWNLOAD );

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
        record.add( null, StoreEffect.DOWNLOAD );

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
