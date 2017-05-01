package org.commonjava.indy.pkg.npm.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.commonjava.indy.pkg.npm.model.PackageMetadata;

import java.io.IOException;

public final class PackageMetadataSerializer
                extends StdSerializer<PackageMetadata>
{
    public PackageMetadataSerializer()
    {
        super( PackageMetadata.class );
    }

    @Override
    public void serialize( final PackageMetadata metadata, final JsonGenerator generator,
                           final SerializerProvider provider ) throws IOException, JsonProcessingException
    {
        generator.writeString( metadata.toString() );
    }
}