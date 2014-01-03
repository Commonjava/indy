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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.commonjava.maven.atlas.graph.model.EProjectKey;
import org.commonjava.maven.atlas.graph.model.EProjectRelationshipCollection;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public abstract class AbstractRelCollectionSer<T extends EProjectRelationshipCollection>
    implements JsonSerializer<T>, JsonDeserializer<T>
{

    protected final List<ProjectRelationship<?>> deserializeRelationships( final JsonObject obj,
                                                                           final JsonDeserializationContext ctx )
    {
        final List<ProjectRelationship<?>> rels = new ArrayList<ProjectRelationship<?>>();
        final JsonElement arrElem = obj.get( SerializationConstants.RELATIONSHIPS );

        if ( arrElem != null )
        {
            for ( final JsonElement relElem : arrElem.getAsJsonArray() )
            {
                rels.add( (ProjectRelationship<?>) ctx.deserialize( relElem, ProjectRelationship.class ) );
            }
        }

        return rels;
    }

    protected final EProjectKey deserializeKey( final JsonObject obj, final JsonDeserializationContext ctx )
    {
        return ctx.deserialize( obj.get( SerializationConstants.EPROJECT_KEY ), EProjectKey.class );
    }

    protected final void serializeKey( final EProjectKey key, final JsonObject obj, final JsonSerializationContext ctx )
    {
        obj.add( SerializationConstants.EPROJECT_KEY, ctx.serialize( key, EProjectKey.class ) );
    }

    protected final JsonArray serializeRelationships( final T src, final JsonObject obj,
                                                      final JsonSerializationContext ctx )
    {
        JsonArray arr = null;
        final Collection<ProjectRelationship<?>> all = src.getExactAllRelationships();
        if ( all != null && !all.isEmpty() )
        {
            arr = new JsonArray();

            for ( final ProjectRelationship<?> rel : all )
            {
                arr.add( ctx.serialize( rel ) );
            }

            obj.add( SerializationConstants.RELATIONSHIPS, arr );
        }

        return arr;
    }

}
