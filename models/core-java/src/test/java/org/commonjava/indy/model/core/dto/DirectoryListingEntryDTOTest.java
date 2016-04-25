package org.commonjava.indy.model.core.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 4/25/16.
 */
public class DirectoryListingEntryDTOTest
{

    @Test
    public void jsonRoundTrip()
            throws IOException
    {
        DirectoryListingEntryDTO in =
                new DirectoryListingEntryDTO( new StoreKey( StoreType.remote, "test" ), "/this/is/a/path.pom" );

        IndyObjectMapper mapper = new IndyObjectMapper( true );
        String json = mapper.writeValueAsString( in );

        DirectoryListingEntryDTO out = mapper.readValue( json, DirectoryListingEntryDTO.class );

        assertThat( out, notNullValue() );
        assertThat( out.getKey(), equalTo( in.getKey() ) );
        assertThat( out.getPath(), equalTo( in.getPath() ) );
    }
}
