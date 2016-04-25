package org.commonjava.indy.autoprox.rest.dto;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 4/25/16.
 */
public class CatalogDTOTest
{
    @Test
    public void jsonRoundTrip()
            throws IOException
    {
        RuleDTO firstRule = readRule( "test-autoprox-rule.groovy" );
        RuleDTO secondRule = readRule( "test-autoprox-rule-2.groovy" );

        CatalogDTO in = new CatalogDTO( true, Arrays.asList( firstRule, secondRule ) );

        IndyObjectMapper mapper = new IndyObjectMapper( true );

        String json = mapper.writeValueAsString( in );

        CatalogDTO out = mapper.readValue( json, CatalogDTO.class );

        assertThat( out, notNullValue() );
        assertThat( out.isEnabled(), equalTo( in.isEnabled() ) );

        List<RuleDTO> rules = out.getRules();

        assertThat( rules, notNullValue() );
        assertThat( rules.size(), equalTo( 2 ) );

        assertThat( rules.get( 0 ), equalTo( firstRule ) );
        assertThat( rules.get( 1 ), equalTo( secondRule ) );
    }

    private RuleDTO readRule( String resource )
            throws IOException
    {
        try(InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream( resource ))
        {
            String spec = IOUtils.toString( stream );
            return new RuleDTO( "test", spec );
        }
    }
}
