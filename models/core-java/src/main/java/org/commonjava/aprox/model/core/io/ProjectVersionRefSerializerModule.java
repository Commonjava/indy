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
package org.commonjava.aprox.model.core.io;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.enterprise.context.ApplicationScoped;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.VersionlessArtifactRef;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@ApplicationScoped
public class ProjectVersionRefSerializerModule
    extends SimpleModule
{

    private static final long serialVersionUID = 1L;

    public ProjectVersionRefSerializerModule()
    {
        super( "ProjectRef (with variants) Serializer" );

        // ProjectRef
        addSerializer( ProjectRef.class, new ProjectRefSerializer<ProjectRef>( ProjectRef.class ) );

        addDeserializer( ProjectRef.class, new ProjectRefDeserializer<ProjectRef>( ProjectRef.class ) );

        // ProjectVersionRef
        addSerializer( ProjectVersionRef.class, new ProjectRefSerializer<ProjectVersionRef>( ProjectVersionRef.class ) );

        addDeserializer( ProjectVersionRef.class,
                         new ProjectRefDeserializer<ProjectVersionRef>( ProjectVersionRef.class ) );

        // ArtifactRef
        addSerializer( ArtifactRef.class, new ProjectRefSerializer<ArtifactRef>( ArtifactRef.class ) );

        addDeserializer( ArtifactRef.class, new ProjectRefDeserializer<ArtifactRef>( ArtifactRef.class ) );

        // VersionlessArtifactRef
        addSerializer( VersionlessArtifactRef.class,
                       new ProjectRefSerializer<VersionlessArtifactRef>( VersionlessArtifactRef.class ) );

        addDeserializer( VersionlessArtifactRef.class,
                         new ProjectRefDeserializer<VersionlessArtifactRef>( VersionlessArtifactRef.class ) );
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

    private static final class ProjectRefSerializer<T extends ProjectRef>
        extends StdSerializer<T>
    {
        ProjectRefSerializer( final Class<T> refCls )
        {
            super( refCls );
        }

        @Override
        public void serialize( final T src, final JsonGenerator generator,
                               final SerializerProvider provider )
            throws IOException, JsonGenerationException
        {
            generator.writeString( src.toString() );
        }
    }

    private static final class ProjectRefDeserializer<T extends ProjectRef>
        extends StdDeserializer<T>
    {
        private static final long serialVersionUID = 1L;

        private final Class<T> refCls;

        ProjectRefDeserializer( final Class<T> refCls )
        {
            super( refCls );
            this.refCls = refCls;
        }

        @Override
        public T deserialize( final JsonParser jp, final DeserializationContext ctxt )
            throws IOException, JsonProcessingException
        {
            try
            {
                final Method parseMethod = refCls.getMethod( "parse", String.class );
                return refCls.cast( parseMethod.invoke( null, jp.getText() ) );
            }
            catch ( NoSuchMethodException | IllegalAccessException | InvocationTargetException e )
            {
                throw new IOException( "Failed to lookup/invoke parse() method on " + refCls.getSimpleName(), e );
            }
        }
    }

}
