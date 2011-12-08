package org.commonjava.aprox.mem.model;

import java.util.List;

import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.ModelFactory;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreKey;

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

}
