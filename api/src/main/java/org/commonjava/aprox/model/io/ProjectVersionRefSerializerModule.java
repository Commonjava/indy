/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.model.io;

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
