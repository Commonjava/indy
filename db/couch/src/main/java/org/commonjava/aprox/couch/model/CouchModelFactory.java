package org.commonjava.aprox.couch.model;

import java.lang.reflect.Type;
import java.util.List;

import javax.inject.Singleton;

import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.ModelFactory;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreKey;

import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;

@Singleton
public class CouchModelFactory
    implements ModelFactory
{

    @Override
    public DeployPoint createDeployPoint( final String name )
    {
        return new DeployPointDoc( name );
    }

    @Override
    public Repository createRepository( final String name, final String remoteUrl )
    {
        return new RepositoryDoc( name, remoteUrl );
    }

    @Override
    public Group createGroup( final String name, final List<StoreKey> constituents )
    {
        return new GroupDoc( name, constituents );
    }

    @Override
    public Group createGroup( final String name, final StoreKey... constituents )
    {
        return new GroupDoc( name, constituents );
    }

    @Override
    public ArtifactStore convertModel( final ArtifactStore store )
    {
        if ( store instanceof ArtifactStoreDoc )
        {
            return store;
        }
        else if ( store instanceof Group )
        {
            final Group g = (Group) store;
            return createGroup( g.getName(), g.getConstituents() );
        }
        else if ( store instanceof Repository )
        {
            final Repository r = (Repository) store;
            return createRepository( r.getName(), r.getUrl() );
        }
        else if ( store instanceof DeployPoint )
        {
            final DeployPoint dp = (DeployPoint) store;
            return createDeployPoint( dp.getName() );
        }

        throw new RuntimeException( "Unknown model class: " + store.getClass()
                                                                   .getName() );
    }

    @Override
    public Class<? extends Group> getGroupType()
    {
        return GroupDoc.class;
    }

    @Override
    public Class<? extends Repository> getRepositoryType()
    {
        return RepositoryDoc.class;
    }

    @Override
    public Class<? extends DeployPoint> getDeployPointType()
    {
        return DeployPointDoc.class;
    }

    public static final class DeployPointInstanceCreator
        implements InstanceCreator<DeployPoint>
    {
        @Override
        public DeployPoint createInstance( final Type type )
        {
            return new DeployPointDoc();
        }
    }

    public static final class RepositoryInstanceCreator
        implements InstanceCreator<Repository>
    {
        @Override
        public Repository createInstance( final Type type )
        {
            return new RepositoryDoc();
        }
    }

    public static final class GroupInstanceCreator
        implements InstanceCreator<Group>
    {
        @Override
        public Group createInstance( final Type type )
        {
            return new GroupDoc();
        }
    }

    @Override
    public void register( final GsonBuilder gsonBuilder )
    {
        gsonBuilder.registerTypeAdapter( DeployPoint.class, new DeployPointInstanceCreator() );
        gsonBuilder.registerTypeAdapter( Repository.class, new RepositoryInstanceCreator() );
        gsonBuilder.registerTypeAdapter( Group.class, new GroupInstanceCreator() );
    }

}
