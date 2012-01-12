package org.commonjava.aprox.mem.model;

import java.lang.reflect.Type;
import java.util.List;

import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.ModelFactory;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreKey;

import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class MemoryModelFactory
    implements ModelFactory
{

    @Override
    public DeployPoint createDeployPoint( final String name )
    {
        return new MemoryDeployPoint( name );
    }

    @Override
    public Repository createRepository( final String name, final String remoteUrl )
    {
        return new MemoryRepository( name, remoteUrl );
    }

    @Override
    public Group createGroup( final String name, final List<StoreKey> constituents )
    {
        return new MemoryGroup( name, constituents );
    }

    @Override
    public Group createGroup( final String name, final StoreKey... constituents )
    {
        return new MemoryGroup( name, constituents );
    }

    @Override
    public ArtifactStore convertModel( final ArtifactStore store )
    {
        return store;
    }

    @Override
    public Class<? extends Group> getGroupType()
    {
        return MemoryGroup.class;
    }

    @Override
    public Class<? extends Repository> getRepositoryType()
    {
        return MemoryRepository.class;
    }

    @Override
    public Class<? extends DeployPoint> getDeployPointType()
    {
        return MemoryDeployPoint.class;
    }

    public static final class DeployPointSer
        implements JsonSerializer<DeployPoint>, JsonDeserializer<DeployPoint>, InstanceCreator<DeployPoint>
    {
        @Override
        public DeployPoint deserialize( final JsonElement json, final Type typeOfT,
                                        final JsonDeserializationContext context )
            throws JsonParseException
        {
            final JsonObject obj = json.getAsJsonObject();
            final String name = obj.get( "name" )
                                   .getAsString();

            JsonElement e = obj.get( "allow_snapshots" );
            final boolean allowSnaps = e == null ? false : e.getAsBoolean();

            e = obj.get( "allow_releases" );
            final boolean allowRel = e == null ? true : e.getAsBoolean();

            final MemoryDeployPoint d = new MemoryDeployPoint( name );
            d.setAllowReleases( allowRel );
            d.setAllowSnapshots( allowSnaps );

            return d;
        }

        @Override
        public JsonElement serialize( final DeployPoint src, final Type typeOfSrc,
                                      final JsonSerializationContext context )
        {
            final JsonObject obj = new JsonObject();
            obj.addProperty( "name", src.getName() );
            obj.addProperty( "allow_snapshots", Boolean.valueOf( src.isAllowSnapshots() ) );
            obj.addProperty( "allow_releases", Boolean.valueOf( src.isAllowReleases() ) );

            return obj;
        }

        @Override
        public DeployPoint createInstance( final Type type )
        {
            return new MemoryDeployPoint();
        }
    }

    public static final class RepositorySer
        implements JsonSerializer<Repository>, JsonDeserializer<Repository>, InstanceCreator<Repository>
    {
        @Override
        public Repository deserialize( final JsonElement json, final Type typeOfT,
                                       final JsonDeserializationContext context )
            throws JsonParseException
        {
            final JsonObject obj = json.getAsJsonObject();

            JsonElement e = obj.get( "name" );
            final String name = e.getAsString();

            e = obj.get( "url" );
            final String url = e.getAsString();

            e = obj.get( "user" );
            final String user = e == null ? null : e.getAsString();

            e = obj.get( "password" );
            final String password = e == null ? null : e.getAsString();

            final String host = obj.get( "host" )
                                   .getAsString();

            e = obj.get( "port" );
            final int port = e == null ? 80 : e.getAsInt();

            e = obj.get( "timeout_seconds" );
            final int timeoutSeconds = e == null ? 0 : e.getAsInt();

            final MemoryRepository r = new MemoryRepository( name );
            r.setHost( host );
            r.setPassword( password );
            r.setPort( port );
            r.setTimeoutSeconds( timeoutSeconds );
            r.setUrl( url );
            r.setUser( user );

            return r;
        }

        @Override
        public JsonElement serialize( final Repository src, final Type typeOfSrc, final JsonSerializationContext context )
        {
            final JsonObject obj = new JsonObject();
            obj.addProperty( "name", src.getName() );
            obj.addProperty( "url", src.getUrl() );
            obj.addProperty( "user", src.getUser() );
            obj.addProperty( "password", src.getPassword() );
            obj.addProperty( "host", src.getHost() );
            obj.addProperty( "port", src.getPort() );
            obj.addProperty( "timeout_seconds", src.getTimeoutSeconds() );

            return obj;
        }

        @Override
        public Repository createInstance( final Type type )
        {
            return new MemoryRepository();
        }
    }

    public static final class GroupSer
        implements JsonSerializer<Group>, JsonDeserializer<Group>, InstanceCreator<Group>
    {
        @Override
        public Group deserialize( final JsonElement json, final Type typeOfT, final JsonDeserializationContext context )
            throws JsonParseException
        {
            final JsonObject obj = json.getAsJsonObject();
            final String name = obj.get( "name" )
                                   .getAsString();

            final MemoryGroup group = new MemoryGroup( name );

            final JsonElement e = obj.get( "constituents" );
            if ( e != null )
            {
                for ( final JsonElement elem : e.getAsJsonArray() )
                {
                    final StoreKey key = context.deserialize( elem, StoreKey.class );
                    group.addConstituent( key );
                }
            }

            return group;
        }

        @Override
        public JsonElement serialize( final Group src, final Type typeOfSrc, final JsonSerializationContext context )
        {
            final JsonObject obj = new JsonObject();
            obj.addProperty( "name", src.getName() );

            final List<StoreKey> constituents = src.getConstituents();
            if ( constituents != null && !constituents.isEmpty() )
            {
                final JsonArray arry = new JsonArray();
                obj.add( "constituents", arry );

                for ( final StoreKey key : src.getConstituents() )
                {
                    arry.add( context.serialize( key ) );
                }
            }

            return obj;
        }

        @Override
        public Group createInstance( final Type type )
        {
            return new MemoryGroup();
        }
    }

    @Override
    public void register( final GsonBuilder gsonBuilder )
    {
        gsonBuilder.registerTypeAdapter( DeployPoint.class, new DeployPointSer() );
        gsonBuilder.registerTypeAdapter( Repository.class, new RepositorySer() );
        gsonBuilder.registerTypeAdapter( Group.class, new GroupSer() );
    }

}
