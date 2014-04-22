/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
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
