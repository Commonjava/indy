package org.commonjava.aprox.depgraph.dto;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.commonjava.aprox.depgraph.json.MetadataBatchUpdateSerializerModule;
import org.commonjava.aprox.depgraph.json.ProjectRelationshipSerializerModule;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class WebBomDTOTest
{

    @Test
    public void roundTripToJson()
        throws Exception
    {
        final ObjectMapper serializer =
            new AproxObjectMapper( true, new MetadataBatchUpdateSerializerModule(),
                                   new ProjectRelationshipSerializerModule() );

        final GraphDescription desc =
            new GraphDescription( "runtime", Collections.<String, Object> emptyMap(),
                                  Collections.singleton( new ProjectVersionRef( "org.foo", "bar", "1.0" ) ) );

        final GraphComposition comp = new GraphComposition( null, Collections.singletonList( desc ) );

        final WebBomDTO dto = new WebBomDTO();
        dto.setGraphComposition( comp );
        dto.setSource( new StoreKey( StoreType.remote, "central" ) );
        dto.setOutput( new ProjectVersionRef( "org.foo", "bar-bom", "1.0" ) );
        dto.setWorkspaceId( "bar" );
        dto.setResolve( true );
        dto.setMetas( Collections.<String> emptySet() );

        final String json = serializer.writeValueAsString( dto );

        System.out.println( json );

        final WebBomDTO out = serializer.readValue( json, WebBomDTO.class );

        System.out.println( out.getOutput() );

        assertThat( out, notNullValue() );

        final GraphComposition outComp = out.getGraphComposition();

        assertThat( outComp, notNullValue() );
        assertThat( outComp, equalTo( comp ) );

        assertThat( out.isResolve(), equalTo( true ) );
        assertThat( out.getSource(), equalTo( dto.getSource() ) );
        assertThat( out.getOutput(), equalTo( dto.getOutput() ) );
        assertThat( out.getWorkspaceId(), equalTo( dto.getWorkspaceId() ) );
        assertThat( out.getMetas(), equalTo( dto.getMetas() ) );
    }

}
