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
package org.commonjava.aprox.model.io;

import java.lang.reflect.Type;

import org.commonjava.aprox.model.StoreKey;
import org.commonjava.web.json.ser.WebSerializationAdapter;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class StoreKeySerializer
    implements WebSerializationAdapter, JsonSerializer<StoreKey>, JsonDeserializer<StoreKey>
{

    @Override
    public StoreKey deserialize( final JsonElement json, final Type typeOfT, final JsonDeserializationContext context )
        throws JsonParseException
    {
        final String id = json.getAsString();
        return StoreKey.fromString( id );
    }

    @Override
    public JsonElement serialize( final StoreKey src, final Type typeOfSrc, final JsonSerializationContext context )
    {
        return new JsonPrimitive( src.toString() );
    }

    @Override
    public void register( final GsonBuilder gsonBuilder )
    {
        gsonBuilder.registerTypeAdapter( StoreKey.class, this );
    }

    @Override
    public boolean equals( final Object other )
    {
        return getClass().getName()
                         .equals( other.getClass()
                                       .getName() );
    }

    @Override
    public int hashCode()
    {
        return 31 + getClass().getName()
                              .hashCode();
    }

}
