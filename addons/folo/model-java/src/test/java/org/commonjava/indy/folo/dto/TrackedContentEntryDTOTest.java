package org.commonjava.indy.folo.dto;

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 4/25/16.
 */
public class TrackedContentEntryDTOTest
{
    @Test
    public void jsonRoundTrip_Basic()
            throws IOException
    {
        TrackedContentEntryDTO in =
                new TrackedContentEntryDTO( new StoreKey( StoreType.remote, "foo" ), "/path/to/my.pom" );

        assertRoundTrip( in, (out)->{} );
    }

    @Test
    public void jsonRoundTrip_Checksums()
            throws IOException
    {
        TrackedContentEntryDTO in =
                new TrackedContentEntryDTO( new StoreKey( StoreType.remote, "foo" ), "/path/to/my.pom" );

        String content = "This is a test string";
        in.setMd5( DigestUtils.md5Hex( content ) );
        in.setSha256( DigestUtils.sha256Hex( content ) );

        assertRoundTrip( in, (out)->{
            assertThat( out.getMd5(), equalTo( in.getMd5() ) );
            assertThat( out.getSha256(), equalTo( in.getSha256() ) );
        } );
    }

    @Test
    public void jsonRoundTrip_Urls()
            throws IOException
    {
        TrackedContentEntryDTO in =
                new TrackedContentEntryDTO( new StoreKey( StoreType.remote, "foo" ), "/path/to/my.pom" );

        in.setLocalUrl( "http://localhost:8080/api/remote/foo/path/to/my.pom" );
        in.setOriginUrl( "http://foo.com/repo/path/to/my.pom" );

        assertRoundTrip( in, (out)->{
            assertThat( out.getLocalUrl(), equalTo( out.getLocalUrl() ) );
            assertThat( out.getOriginUrl(), equalTo( out.getOriginUrl() ) );
        } );
    }

    private void assertRoundTrip( TrackedContentEntryDTO in, Consumer<TrackedContentEntryDTO> extraAssertions )
            throws IOException
    {
        IndyObjectMapper mapper = new IndyObjectMapper( true );

        String json = mapper.writeValueAsString( in );

        TrackedContentEntryDTO out = mapper.readValue( json, TrackedContentEntryDTO.class );

        assertThat( out, notNullValue() );
        assertThat( out.getStoreKey(), equalTo( in.getStoreKey() ) );
        assertThat( out.getPath(), equalTo( in.getPath() ) );

        if ( extraAssertions != null )
        {
            extraAssertions.accept( out );
        }

        assertThat( out, equalTo( in ) );
    }
}
