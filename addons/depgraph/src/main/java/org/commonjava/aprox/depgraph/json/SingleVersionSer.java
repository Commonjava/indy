package org.commonjava.aprox.depgraph.json;

import java.lang.reflect.Type;

import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.maven.atlas.ident.version.VersionUtils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class SingleVersionSer
    implements JsonSerializer<SingleVersion>, JsonDeserializer<SingleVersion>
{

    @Override
    public SingleVersion deserialize( final JsonElement src, final Type type, final JsonDeserializationContext ctx )
        throws JsonParseException
    {
        return VersionUtils.createSingleVersion( src.getAsString() );
    }

    @Override
    public JsonElement serialize( final SingleVersion src, final Type type, final JsonSerializationContext ctx )
    {
        return new JsonPrimitive( src.renderStandard() );
    }

}
