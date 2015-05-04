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
package org.commonjava.aprox.depgraph.json;

import static org.commonjava.aprox.depgraph.json.SerializationConstants.CURRENT_JSON_VERSION;
import static org.commonjava.aprox.depgraph.json.SerializationConstants.DECLARING_REF;
import static org.commonjava.aprox.depgraph.json.SerializationConstants.INDEX;
import static org.commonjava.aprox.depgraph.json.SerializationConstants.JSON_VERSION;
import static org.commonjava.aprox.depgraph.json.SerializationConstants.MANAGED;
import static org.commonjava.aprox.depgraph.json.SerializationConstants.PLUGIN_REF;
import static org.commonjava.aprox.depgraph.json.SerializationConstants.POM_LOCATION_URI;
import static org.commonjava.aprox.depgraph.json.SerializationConstants.RELATIONSHIP_TYPE;
import static org.commonjava.aprox.depgraph.json.SerializationConstants.SCOPE;
import static org.commonjava.aprox.depgraph.json.SerializationConstants.TARGET_REF;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.enterprise.context.ApplicationScoped;

import org.commonjava.aprox.model.core.io.AproxSerializationException;
import org.commonjava.maven.atlas.graph.rel.BomRelationship;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@ApplicationScoped
public class ProjectRelationshipSerializerModule
    extends SimpleModule
{

    private static final long serialVersionUID = 1L;

    public ProjectRelationshipSerializerModule()
    {
        super( "ProjectRelationship<?> Serializer" );
        addSerializer( ProjectRelationship.class, new ProjectRelationshipSerializer() );
        addDeserializer( ProjectRelationship.class, new ProjectRelationshipDeserializer() );
    }

    @Override
    public int hashCode()
    {
        return getClass().getSimpleName()
                         .hashCode() + 17;
    }

    @Override
    public boolean equals( final Object other )
    {
        return getClass().equals( other.getClass() );
    }

    @SuppressWarnings( "rawtypes" )
    private static final class ProjectRelationshipSerializer
        extends StdSerializer<ProjectRelationship>
    {
        ProjectRelationshipSerializer()
        {
            super( ProjectRelationship.class );
        }

        @SuppressWarnings( "incomplete-switch" )
        @Override
        public void serialize( final ProjectRelationship value, final JsonGenerator gen,
                               final SerializerProvider provider )
            throws IOException, JsonGenerationException
        {
            gen.writeStartObject();
            gen.writeNumberField( JSON_VERSION.getValue(), SerializationConstants.CURRENT_JSON_VERSION );
            gen.writeStringField( RELATIONSHIP_TYPE.getValue(), value.getType().name() );
            provider.defaultSerializeField( DECLARING_REF.getValue(), value.getDeclaring(), gen );
            provider.defaultSerializeField( TARGET_REF.getValue(), value.getTarget(), gen );
            gen.writeNumberField(INDEX.getValue(), value.getIndex());

            switch ( value.getType() )
            {
                case DEPENDENCY:
                {
                    gen.writeStringField( SCOPE.getValue(), ( (DependencyRelationship) value ).getScope()
                                                                                                   .realName() );
                    break;
                }
                case PLUGIN_DEP:
                {
                    provider.defaultSerializeField( PLUGIN_REF.getValue(), ( (PluginDependencyRelationship) value ).getPlugin(), gen );
                    break;
                }
            }
            gen.writeEndObject();
        }

    }

    @SuppressWarnings( "rawtypes" )
    private static final class ProjectRelationshipDeserializer
        extends StdDeserializer<ProjectRelationship>
    {
        private static final long serialVersionUID = 1L;

        ProjectRelationshipDeserializer()
        {
            super( ProjectRelationship.class );
        }

        @Override
        public ProjectRelationship deserialize( final JsonParser jp, final DeserializationContext ctx )
            throws IOException, JsonProcessingException
        {
            final int version = expectField( jp, JSON_VERSION ).nextIntValue( CURRENT_JSON_VERSION );
            if ( version > CURRENT_JSON_VERSION )
            {
                throw new AproxSerializationException( "Cannot deserialize ProjectRelationship JSON with version: " + version, jp.getCurrentLocation() );
            }
            
            final JsonDeserializer<Object> prDeser =
                ctx.findRootValueDeserializer( ctx.getTypeFactory()
                                                  .constructType( ProjectRef.class ) );
            final JsonDeserializer<Object> pvrDeser =
                ctx.findRootValueDeserializer( ctx.getTypeFactory()
                                                  .constructType( ProjectVersionRef.class ) );
            final JsonDeserializer<Object> arDeser =
                ctx.findRootValueDeserializer( ctx.getTypeFactory()
                                                  .constructType( ArtifactRef.class ) );

            final RelationshipType type =
                RelationshipType.getType( expectField( jp, RELATIONSHIP_TYPE ).nextTextValue() );
            
            expectField( jp, DECLARING_REF ).nextTextValue();
            final ProjectVersionRef declaring = (ProjectVersionRef) pvrDeser.deserialize( jp, ctx );

            final String uri = expectField( jp, POM_LOCATION_URI ).nextTextValue();
            URI pomLocation;
            try
            {
                pomLocation = new URI( uri );
            }
            catch ( final URISyntaxException e )
            {
                throw new AproxSerializationException( "Invalid " + POM_LOCATION_URI + ": '" + uri + "': "
                    + e.getMessage(), jp.getCurrentLocation(), e );
            }

            switch ( type )
            {
                case DEPENDENCY:
                {
                    expectField( jp, TARGET_REF ).nextTextValue();
                    final ArtifactRef target = (ArtifactRef) arDeser.deserialize( jp, ctx );

                    final DependencyScope scope = DependencyScope.getScope( expectField( jp, SCOPE ).nextTextValue() );

                    return new DependencyRelationship( pomLocation, declaring, target, scope, getIndex( jp ),
                                                       isManaged( jp ) );
                }
                case EXTENSION:
                {
                    expectField( jp, TARGET_REF ).nextTextValue();
                    final ProjectVersionRef target = (ProjectVersionRef) pvrDeser.deserialize( jp, ctx );

                    return new ExtensionRelationship( pomLocation, declaring, target, getIndex( jp ) );
                }
                case PARENT:
                {
                    expectField( jp, TARGET_REF ).nextTextValue();
                    final ProjectVersionRef target = (ProjectVersionRef) pvrDeser.deserialize( jp, ctx );

                    return new ParentRelationship( pomLocation, declaring, target );
                }
                case PLUGIN:
                {
                    expectField( jp, TARGET_REF ).nextTextValue();
                    final ProjectVersionRef target = (ProjectVersionRef) pvrDeser.deserialize( jp, ctx );

                    return new PluginRelationship( pomLocation, declaring, target, getIndex( jp ), isManaged( jp ) );
                }
                case PLUGIN_DEP:
                {
                    expectField( jp, PLUGIN_REF ).nextTextValue();
                    final ProjectRef plugin = (ProjectRef) prDeser.deserialize( jp, ctx );

                    expectField( jp, TARGET_REF ).nextTextValue();
                    final ArtifactRef target = (ArtifactRef) arDeser.deserialize( jp, ctx );

                    return new PluginDependencyRelationship( pomLocation, declaring, plugin, target, getIndex( jp ),
                                                             isManaged( jp ) );
                }
                case BOM:
                {
                    expectField( jp, TARGET_REF ).nextTextValue();
                    final ProjectVersionRef target = (ProjectVersionRef) pvrDeser.deserialize( jp, ctx );

                    return new BomRelationship( pomLocation, declaring, target, getIndex( jp ) );
                }
            }

            return null;
        }

        private JsonParser expectField( final JsonParser jp, final SerializedString named )
            throws JsonParseException, IOException
        {
            if ( !jp.nextFieldName( named ) )
            {
                throw new AproxSerializationException( "Expected field: " + named, jp.getCurrentLocation() );
            }

            return jp;
        }

        private boolean isManaged( final JsonParser jp )
            throws JsonParseException, IOException
        {
            return expectField( jp, MANAGED ).nextBooleanValue();
        }

        private int getIndex( final JsonParser jp )
            throws JsonParseException, IOException
        {
            return expectField( jp, INDEX ).nextIntValue( 0 );
        }
    }

}
