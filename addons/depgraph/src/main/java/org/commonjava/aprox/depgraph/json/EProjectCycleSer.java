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
