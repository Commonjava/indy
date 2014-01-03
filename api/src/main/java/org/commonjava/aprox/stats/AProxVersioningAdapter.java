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
