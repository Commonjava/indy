package org.commonjava.web.maven.proxy.model.io;

import java.lang.reflect.Type;
import java.security.acl.Group;

import org.commonjava.couch.io.json.SerializationAdapter;
import org.commonjava.web.maven.proxy.model.ArtifactStore;
import org.commonjava.web.maven.proxy.model.DeployPoint;
import org.commonjava.web.maven.proxy.model.Repository;
import org.commonjava.web.maven.proxy.model.StoreKey;
import org.commonjava.web.maven.proxy.model.StoreType;

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
