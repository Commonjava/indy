package org.commonjava.aprox.depgraph.dto;

import java.util.Collections;

import org.commonjava.aprox.depgraph.inject.PrettyPrintAdapter;
import org.commonjava.aprox.depgraph.json.DepgraphSerializationAdapter;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.model.io.StoreKeySerializer;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.dto.GraphDescription;
import org.commonjava.web.json.ser.JsonSerializer;
import org.junit.Test;

public class WebBomDTOTest
{

    @Test
    public void roundTripToJson()
    {
        final JsonSerializer serializer =
            new JsonSerializer( new StoreKeySerializer(), new DepgraphSerializationAdapter(), new PrettyPrintAdapter() );

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

        final String json = serializer.toString( dto );

        System.out.println( json );

        final WebBomDTO out = serializer.fromString( json, WebBomDTO.class );

        System.out.println( out.getOutput() );
    }

}
