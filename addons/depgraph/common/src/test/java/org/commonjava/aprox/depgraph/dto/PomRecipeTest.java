/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.aprox.depgraph.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.aprox.depgraph.json.ProjectRelationshipSerializerModule;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.request.GraphComposition;
import org.commonjava.maven.cartographer.request.GraphDescription;
import org.commonjava.maven.cartographer.request.PomRequest;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class PomRecipeTest
{

    @Test
    public void roundTripToJson()
        throws Exception
    {
        final ObjectMapper serializer =
            new AproxObjectMapper( true,
                                   new ProjectRelationshipSerializerModule() );

        final GraphDescription desc =
            new GraphDescription( "runtime", Collections.<String, Object> emptyMap(),
                                  Collections.singleton( new ProjectVersionRef( "org.foo", "bar", "1.0" ) ) );

        final GraphComposition comp = new GraphComposition( null, Collections.singletonList( desc ) );

        final PomRequest dto = new PomRequest();
        dto.setGraphComposition( comp );
        dto.setSource( new StoreKey( StoreType.remote, "central" ).toString() );
        dto.setOutput( new ProjectVersionRef( "org.foo", "bar-bom", "1.0" ) );
        dto.setWorkspaceId( "bar" );
        dto.setGraphToManagedDeps( true );
        dto.setResolve( true );
        dto.setMetas( Collections.<String> emptySet() );

        final String json = serializer.writeValueAsString( dto );

        System.out.println( json );

        final PomRequest out = serializer.readValue( json, PomRequest.class );

        System.out.println( out.getOutput() );

        assertThat( out, notNullValue() );

        final GraphComposition outComp = out.getGraphComposition();

        assertThat( outComp, notNullValue() );
        assertThat( outComp, equalTo( comp ) );

        assertThat( out.isResolve(), equalTo( true ) );
        assertThat( out.isGraphToManagedDeps(), equalTo( true ) );
        assertThat( out.getSource(), equalTo( dto.getSource() ) );
        assertThat( out.getOutput(), equalTo( dto.getOutput() ) );
        assertThat( out.getWorkspaceId(), equalTo( dto.getWorkspaceId() ) );
        assertThat( out.getMetas(), equalTo( dto.getMetas() ) );
    }

}
