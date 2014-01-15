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

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class ProjectVersionRefSer
    implements JsonSerializer<ProjectVersionRef>, JsonDeserializer<ProjectVersionRef>
{

    @Override
    public ProjectVersionRef deserialize( final JsonElement src, final Type type, final JsonDeserializationContext ctx )
        throws JsonParseException
    {
        return JsonUtils.parseProjectVersionRef( src.getAsString() );
    }

    @Override
    public JsonElement serialize( final ProjectVersionRef src, final Type type, final JsonSerializationContext ctx )
    {
        return new JsonPrimitive( JsonUtils.formatRef( src ) );
    }

}
