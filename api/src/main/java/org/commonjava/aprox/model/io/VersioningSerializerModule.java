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

import org.commonjava.aprox.stats.AProxVersioning;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public final class VersioningSerializerModule
    extends SimpleModule
{

    private static final long serialVersionUID = 1L;

    public VersioningSerializerModule()
    {
        super( "API Versioning Serializer" );
        addSerializer( new VersioningSerializer() );
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

    private static final class VersioningSerializer
        extends StdSerializer<AProxVersioning>
    {
        VersioningSerializer()
        {
            super( AProxVersioning.class );
        }

        @Override
        public void serialize( final AProxVersioning versioning, final JsonGenerator generator,
                               final SerializerProvider provider )
            throws IOException, JsonProcessingException
        {
            generator.writeStartObject();
            generator.writeStringField( "version", versioning.getVersion() );
            generator.writeStringField( "built_by", versioning.getBuilder() );
            generator.writeStringField( "commit_id", versioning.getCommitId() );
            generator.writeStringField( "built_on", versioning.getTimestamp() );
            generator.writeEndObject();
        }
    }
}
