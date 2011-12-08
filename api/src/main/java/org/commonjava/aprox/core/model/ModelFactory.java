package org.commonjava.aprox.core.model;

import java.util.List;

public interface ModelFactory
{

    DeployPoint createDeployPoint( final String name );

    Repository createRepository( final String name, final String remoteUrl );

    Group createGroup( final String name, final List<StoreKey> constituents );

    Group createGroup( final String name, final StoreKey... constituents );

    ArtifactStore convertModel( ArtifactStore store );
}
