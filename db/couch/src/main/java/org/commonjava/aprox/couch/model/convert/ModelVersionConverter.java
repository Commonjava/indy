package org.commonjava.aprox.couch.model.convert;

import java.lang.reflect.Type;

import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.couch.model.DeployPointDoc;
import org.commonjava.aprox.couch.model.GroupDoc;
import org.commonjava.aprox.couch.model.RepositoryDoc;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.WebSerializationAdapter;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class ModelVersionConverter
    implements WebSerializationAdapter
{

    @Override
    public void register( final GsonBuilder builder )
    {
        builder.registerTypeAdapter( RepositoryDoc.class, new ConvertingRepoDeserializer() );
        builder.registerTypeAdapter( DeployPointDoc.class, new ConvertingDeployPointDeserializer() );
        builder.registerTypeAdapter( GroupDoc.class, new ConvertingGroupDeserializer() );
    }

    public static final class ConvertingRepoDeserializer
        implements JsonDeserializer<RepositoryDoc>
    {

        private final Logger logger = new Logger( getClass() );

        @Override
        public RepositoryDoc deserialize( final JsonElement json, final Type typeOfT,
                                          final JsonDeserializationContext context )
            throws JsonParseException
        {
            final JsonObject obj = json.getAsJsonObject();

            final String id = obj.get( "_id" )
                                 .getAsString();
            final String rev = obj.get( "_rev" )
                                  .getAsString();

            final JsonElement verEl = obj.get( "modelVersion" );
            final String modelVersion = verEl == null ? "1" : verEl.getAsString();

            logger.info( "Deserializing Repository: %s with version: %s", id, modelVersion );

            Repository store;
            if ( verEl == null )
            {
                // do conversion.
                store = context.deserialize( json, Repository.class );
            }
            else
            {
                final JsonElement storeEl = obj.get( "store" );
                store = context.deserialize( storeEl, Repository.class );
            }

            logger.info( "Got store: %s", store );

            return new RepositoryDoc( id, rev, modelVersion, store );
        }

    }

    public static final class ConvertingDeployPointDeserializer
        implements JsonDeserializer<DeployPointDoc>
    {

        private final Logger logger = new Logger( getClass() );

        @Override
        public DeployPointDoc deserialize( final JsonElement json, final Type typeOfT,
                                           final JsonDeserializationContext context )
            throws JsonParseException
        {
            final JsonObject obj = json.getAsJsonObject();

            final String id = obj.get( "_id" )
                                 .getAsString();
            final String rev = obj.get( "_rev" )
                                  .getAsString();

            final JsonElement verEl = obj.get( "modelVersion" );
            final String modelVersion = verEl == null ? "1" : verEl.getAsString();

            logger.info( "Deserializing Deploy Point: %s with version: %s", id, modelVersion );

            DeployPoint store;
            if ( verEl == null )
            {
                // do conversion.
                store = context.deserialize( json, DeployPoint.class );
            }
            else
            {
                final JsonElement storeEl = obj.get( "store" );
                store = context.deserialize( storeEl, DeployPoint.class );
            }

            logger.info( "Got store: %s", store );
            return new DeployPointDoc( id, rev, modelVersion, store );
        }

    }

    public static final class ConvertingGroupDeserializer
        implements JsonDeserializer<GroupDoc>
    {

        private final Logger logger = new Logger( getClass() );

        @Override
        public GroupDoc deserialize( final JsonElement json, final Type typeOfT,
                                     final JsonDeserializationContext context )
            throws JsonParseException
        {
            final JsonObject obj = json.getAsJsonObject();

            final String id = obj.get( "_id" )
                                 .getAsString();
            final String rev = obj.get( "_rev" )
                                  .getAsString();

            final JsonElement verEl = obj.get( "modelVersion" );
            final String modelVersion = verEl == null ? "1" : verEl.getAsString();

            logger.info( "Deserializing Group: %s with version: %s", id, modelVersion );

            Group store;
            if ( verEl == null )
            {
                // do conversion.
                store = context.deserialize( json, Group.class );
            }
            else
            {
                final JsonElement storeEl = obj.get( "store" );
                store = context.deserialize( storeEl, Group.class );
            }

            logger.info( "Got store: %s", store );
            return new GroupDoc( id, rev, modelVersion, store );
        }

    }

}
