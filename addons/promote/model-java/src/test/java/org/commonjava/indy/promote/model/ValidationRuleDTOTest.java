package org.commonjava.indy.promote.model;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 4/25/16.
 */
public class ValidationRuleDTOTest
{
    @Test
    public void jsonRoundTrip()
            throws IOException
    {
        try(InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream( "no-snapshots.groovy" ))
        {
            String spec = IOUtils.toString( stream );
            ValidationRuleDTO in = new ValidationRuleDTO( "test", spec );

            IndyObjectMapper mapper = new IndyObjectMapper( true );
            String json = mapper.writeValueAsString( in );

            ValidationRuleDTO out = mapper.readValue( json, ValidationRuleDTO.class );
            assertThat( out, notNullValue() );
            assertThat( out.getName(), equalTo( in.getName() ) );
            assertThat( out.getSpec(), equalTo( in.getSpec() ) );
            assertThat( out, equalTo( in ) );
        }
    }
}
