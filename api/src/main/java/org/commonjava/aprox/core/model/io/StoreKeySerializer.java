/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.core.model.io;

import java.lang.reflect.Type;

import org.commonjava.aprox.core.model.StoreKey;
import org.commonjava.web.common.ser.WebSerializationAdapter;

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
