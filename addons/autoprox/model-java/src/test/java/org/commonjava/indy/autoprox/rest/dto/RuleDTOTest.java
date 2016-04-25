package org.commonjava.indy.autoprox.rest.dto;

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
public class RuleDTOTest
{
    @Test
    public void jsonRoundTrip()
            throws IOException
    {
        try(InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream( "test-autoprox-rule.groovy" ))
        {
            String spec = IOUtils.toString( stream );
            RuleDTO in = new RuleDTO( "test", spec );

            IndyObjectMapper mapper = new IndyObjectMapper( true );
            String json = mapper.writeValueAsString( in );

            RuleDTO out = mapper.readValue( json, RuleDTO.class );
            assertThat( out, notNullValue() );
            assertThat( out.getName(), equalTo( in.getName() ) );
            assertThat( out.getSpec(), equalTo( in.getSpec() ) );
            assertThat( out, equalTo( in ) );
        }
    }
}
