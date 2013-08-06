package org.commonjava.aprox.depgraph.json;

import java.lang.reflect.Type;
import java.util.Collection;

import org.commonjava.maven.atlas.graph.model.EProjectDirectRelationships;
import org.commonjava.maven.atlas.graph.model.EProjectKey;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

public class EProjectRelsSer
    extends AbstractRelCollectionSer<EProjectDirectRelationships>
{

    @Override
    public EProjectDirectRelationships deserialize( final JsonElement src, final Type typeInfo,
                                              final JsonDeserializationContext ctx )
        throws JsonParseException
    {
        final JsonObject obj = src.getAsJsonObject();

        final EProjectKey key = deserializeKey( obj, ctx );
        final Collection<ProjectRelationship<?>> rels = deserializeRelationships( obj, ctx );

        return new EProjectDirectRelationships.Builder( key ).withRelationships( rels )
                                                       .build();
    }

    @Override
    public JsonElement serialize( final EProjectDirectRelationships src, final Type typeInfo,
                                  final JsonSerializationContext ctx )
    {
        final JsonObject obj = new JsonObject();
        obj.add( SerializationConstants.EPROJECT_KEY, ctx.serialize( src.getKey() ) );

        serializeRelationships( src, obj, ctx );

        return obj;
    }

}
