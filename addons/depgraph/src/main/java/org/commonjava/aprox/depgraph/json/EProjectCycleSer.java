package org.commonjava.aprox.depgraph.json;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

public class EProjectCycleSer
    extends AbstractRelCollectionSer<EProjectCycle>
{

    @Override
    public JsonElement serialize( final EProjectCycle src, final Type typeOfSrc,
                                  final JsonSerializationContext context )
    {
        final JsonObject obj = new JsonObject();
        serializeRelationships( src, obj, context );
        return obj;
    }

    @Override
    public EProjectCycle deserialize( final JsonElement json, final Type typeOfT,
                                           final JsonDeserializationContext context )
        throws JsonParseException
    {
        final JsonObject obj = json.getAsJsonObject();

        final Collection<ProjectRelationship<?>> rels = deserializeRelationships( obj, context );
        return new EProjectCycle( new ArrayList<ProjectRelationship<?>>( rels ) );
    }

}
