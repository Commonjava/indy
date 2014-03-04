/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUGraphTransferDTO ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.depgraph.json;

import static org.commonjava.aprox.depgraph.json.JsonUtils.deserializeCycles;
import static org.commonjava.aprox.depgraph.json.JsonUtils.deserializeRelationships;
import static org.commonjava.aprox.depgraph.json.JsonUtils.serializeCycles;
import static org.commonjava.aprox.depgraph.json.JsonUtils.serializeRelationships;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.aprox.depgraph.dto.GraphTransferDTO;
import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class GraphTransferDTOSer
    implements JsonSerializer<GraphTransferDTO>, JsonDeserializer<GraphTransferDTO>
{

    @Override
    public GraphTransferDTO deserialize( final JsonElement src, final Type typeInfo, final JsonDeserializationContext ctx )
        throws JsonParseException
    {
        final JsonObject obj = src.getAsJsonObject();

        final Set<ProjectVersionRef> roots = new HashSet<ProjectVersionRef>();

        final JsonElement rootsElem = obj.get( SerializationConstants.WEB_ROOTS );
        if ( rootsElem != null )
        {
            final JsonArray rootsArry = rootsElem.getAsJsonArray();
            for ( final JsonElement e : rootsArry )
            {
                final ProjectVersionRef ref = ctx.deserialize( e, ProjectVersionRef.class );
                if ( ref != null )
                {
                    roots.add( ref );
                }
            }
        }

        final Set<ProjectRelationship<?>> rels = deserializeRelationships( obj, ctx );
        final Set<EProjectCycle> cycles = deserializeCycles( obj, ctx );

        return new GraphTransferDTO( roots, rels, cycles );
    }

    @Override
    public JsonElement serialize( final GraphTransferDTO src, final Type typeInfo, final JsonSerializationContext ctx )
    {
        final JsonObject obj = new JsonObject();

        final JsonArray rootArry = new JsonArray();
        final Set<ProjectVersionRef> roots = src.getRoots();
        for ( final ProjectVersionRef root : roots )
        {
            rootArry.add( ctx.serialize( root ) );
        }

        if ( rootArry.size() > 0 )
        {
            obj.add( SerializationConstants.WEB_ROOTS, rootArry );
        }

        serializeRelationships( src.getRelationships(), obj, ctx );
        serializeCycles( src.getCycles(), obj, ctx );

        return obj;
    }

}
