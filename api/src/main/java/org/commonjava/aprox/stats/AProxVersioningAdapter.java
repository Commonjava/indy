package org.commonjava.aprox.stats;

import java.lang.reflect.Type;

import org.commonjava.web.json.ser.WebSerializationAdapter;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public final class AProxVersioningAdapter
    implements JsonSerializer<AProxVersioning>, WebSerializationAdapter
{

    @Override
    public JsonElement serialize( final AProxVersioning src, final Type typeOfSrc,
                                  final JsonSerializationContext context )
    {
        final JsonObject obj = new JsonObject();

        obj.add( "version", new JsonPrimitive( src.getVersion() ) );
        obj.add( "built_by", new JsonPrimitive( src.getBuilder() ) );
        obj.add( "commit_id", new JsonPrimitive( src.getCommitId() ) );
        obj.add( "built_on", new JsonPrimitive( src.getTimestamp() ) );

        return obj;
    }

    @Override
    public void register( final GsonBuilder builder )
    {
        builder.registerTypeAdapter( AProxVersioning.class, this );
    }

}
