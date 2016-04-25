package org.commonjava.indy.autoprox.rest.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 4/25/16.
 */
public class AutoProxCalculationTest
{
    @Test
    public void jsonRoundTrip_HostedRepoCreation()
            throws IOException
    {
        HostedRepository repo = new HostedRepository( "test" );
        AutoProxCalculation in = new AutoProxCalculation( repo, "test-rule.groovy" );

        IndyObjectMapper mapper = new IndyObjectMapper( true );

        String json = mapper.writeValueAsString( in );

        AutoProxCalculation out = mapper.readValue( json, AutoProxCalculation.class );

        assertThat( out, notNullValue() );
        assertThat( out.getRuleName(), equalTo( in.getRuleName() ) );
        assertThat( out.getStore(), equalTo( in.getStore() ) );
        assertThat( out, equalTo( in ) );
    }

    @Test
    public void jsonRoundTrip_RemoteRepoCreation()
            throws IOException
    {
        RemoteRepository repo = new RemoteRepository( "test", "http://foo.com/test" );
        AutoProxCalculation in = new AutoProxCalculation( repo, "test-rule.groovy" );

        IndyObjectMapper mapper = new IndyObjectMapper( true );

        String json = mapper.writeValueAsString( in );

        AutoProxCalculation out = mapper.readValue( json, AutoProxCalculation.class );

        assertThat( out, notNullValue() );
        assertThat( out.getRuleName(), equalTo( in.getRuleName() ) );
        assertThat( out.getStore(), equalTo( in.getStore() ) );
        assertThat( out, equalTo( in ) );
    }

    @Test
    public void jsonRoundTrip_GroupCreation()
            throws IOException
    {
        HostedRepository first = new HostedRepository( "first" );
        HostedRepository second = new HostedRepository( "second" );
        Group repo = new Group( "test", first.getKey(), second.getKey() );

        AutoProxCalculation in = new AutoProxCalculation( repo, Arrays.asList( first, second ), "test-rule.groovy" );

        IndyObjectMapper mapper = new IndyObjectMapper( true );

        String json = mapper.writeValueAsString( in );

        AutoProxCalculation out = mapper.readValue( json, AutoProxCalculation.class );

        assertThat( out, notNullValue() );
        assertThat( out.getRuleName(), equalTo( in.getRuleName() ) );
        assertThat( out.getStore(), equalTo( in.getStore() ) );
        assertThat( out, equalTo( in ) );

        List<ArtifactStore> supplementalStores = out.getSupplementalStores();

        assertThat( supplementalStores, notNullValue() );
        assertThat( supplementalStores.size(), equalTo( 2 ) );
        assertThat( supplementalStores.get( 0 ), equalTo( first ) );
        assertThat( supplementalStores.get( 1 ), equalTo( second ) );
    }

}
