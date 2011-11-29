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
import java.security.acl.Group;

import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreKey;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.couch.io.json.SerializationAdapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class StoreDeserializer
    implements SerializationAdapter, JsonDeserializer<ArtifactStore>
{

    // private final Logger logger = new Logger( getClass() );

    @Override
    public Type typeLiteral()
    {
        return ArtifactStore.class;
    }

    @Override
    public ArtifactStore deserialize( final JsonElement json, final Type typeOfT,
                                      final JsonDeserializationContext context )
        throws JsonParseException
    {
        String id = json.getAsJsonObject().get( "_id" ).getAsString();
        StoreKey key = StoreKey.fromString( id );

        StoreType type = key.getType();
        // logger.info( "Parsing store of type: %s", type );

        ArtifactStore result = null;
        if ( type == StoreType.deploy_point )
        {
            // logger.info( "Parsing deploy-point store..." );
            result = context.deserialize( json, DeployPoint.class );
        }
        else if ( type == StoreType.group )
        {
            // logger.info( "Parsing group store..." );
            result = context.deserialize( json, Group.class );
        }
        else
        {
            // logger.info( "Parsing repository store..." );
            result = context.deserialize( json, Repository.class );
        }

        return result;
    }

}
