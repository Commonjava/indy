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
package org.commonjava.aprox.couch.model.io;

import java.lang.reflect.Type;

import org.commonjava.aprox.core.model.StoreKey;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.aprox.couch.model.ArtifactStoreDoc;
import org.commonjava.aprox.couch.model.DeployPointDoc;
import org.commonjava.aprox.couch.model.GroupDoc;
import org.commonjava.aprox.couch.model.RepositoryDoc;
import org.commonjava.web.common.ser.WebSerializationAdapter;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class StoreDocDeserializer
    implements WebSerializationAdapter, JsonDeserializer<ArtifactStoreDoc>
{

    @Override
    public ArtifactStoreDoc deserialize( final JsonElement json, final Type typeOfT,
                                         final JsonDeserializationContext context )
        throws JsonParseException
    {
        final String id = json.getAsJsonObject()
                              .get( "_id" )
                              .getAsString();
        final StoreKey key = StoreKey.fromString( id );

        final StoreType type = key.getType();
        // logger.info( "Parsing store of type: %s", type );

        ArtifactStoreDoc result = null;
        if ( type == StoreType.deploy_point )
        {
            // logger.info( "Parsing deploy-point store..." );
            result = context.deserialize( json, DeployPointDoc.class );
        }
        else if ( type == StoreType.group )
        {
            // logger.info( "Parsing group store..." );
            result = context.deserialize( json, GroupDoc.class );
        }
        else
        {
            // logger.info( "Parsing repository store..." );
            result = context.deserialize( json, RepositoryDoc.class );
        }

        return result;
    }

    @Override
    public void register( final GsonBuilder gsonBuilder )
    {
        gsonBuilder.registerTypeAdapter( ArtifactStoreDoc.class, this );
    }

}
