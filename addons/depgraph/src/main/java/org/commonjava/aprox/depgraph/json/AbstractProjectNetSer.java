package org.commonjava.aprox.depgraph.json;

import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.EProjectNet;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

public abstract class AbstractProjectNetSer<T extends EProjectNet>
    extends AbstractRelCollectionSer<T>
{

    protected void serializeCycles( final T src, final JsonObject obj, final JsonSerializationContext ctx )
    {
        final JsonArray arry = new JsonArray();
        final Set<EProjectCycle> cycles = src.getCycles();
        if ( cycles == null )
        {
            return;
        }

        for ( final EProjectCycle cycle : cycles )
        {
            arry.add( ctx.serialize( cycle ) );
        }

        obj.add( SerializationConstants.CYCLES, arry );
    }

    protected Set<EProjectCycle> deserializeCycles( final JsonObject obj, final JsonDeserializationContext ctx )
    {
        final JsonElement cyclesElem = obj.get( SerializationConstants.CYCLES );
        if ( cyclesElem == null )
        {
            return null;
        }

        final JsonArray arry = cyclesElem.getAsJsonArray();
        final Set<EProjectCycle> cycles = new HashSet<EProjectCycle>( arry.size() );

        for ( final JsonElement elem : arry )
        {
            final EProjectCycle cycle = ctx.deserialize( elem, EProjectCycle.class );
            if ( cycle != null )
            {
                cycles.add( cycle );
            }
        }

        return cycles;
    }

}
