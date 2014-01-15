/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
