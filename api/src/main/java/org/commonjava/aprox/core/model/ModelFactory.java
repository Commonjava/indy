package org.commonjava.aprox.core.model;

import java.util.List;

import org.commonjava.web.common.ser.WebSerializationAdapter;

public interface ModelFactory
    extends WebSerializationAdapter
{

    Class<? extends Group> getGroupType();

    Class<? extends Repository> getRepositoryType();

    Class<? extends DeployPoint> getDeployPointType();

    DeployPoint createDeployPoint( final String name );

    Repository createRepository( final String name, final String remoteUrl );

    Group createGroup( final String name, final List<StoreKey> constituents );

    Group createGroup( final String name, final StoreKey... constituents );

    ArtifactStore convertModel( ArtifactStore store );
}
