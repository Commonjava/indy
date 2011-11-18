package org.commonjava.aprox.core.data;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreKey;

public interface ProxyDataManager
{

    DeployPoint getDeployPoint( final String name )
        throws ProxyDataException;

    Repository getRepository( final String name )
        throws ProxyDataException;

    Group getGroup( final String name )
        throws ProxyDataException;

    List<Group> getAllGroups()
        throws ProxyDataException;

    List<Repository> getAllRepositories()
        throws ProxyDataException;

    List<DeployPoint> getAllDeployPoints()
        throws ProxyDataException;

    List<ArtifactStore> getOrderedConcreteStoresInGroup( final String groupName )
        throws ProxyDataException;

    Set<Group> getGroupsContaining( final StoreKey repo )
        throws ProxyDataException;

    void storeDeployPoints( final Collection<DeployPoint> deploys )
        throws ProxyDataException;

    boolean storeDeployPoint( final DeployPoint deploy )
        throws ProxyDataException;

    boolean storeDeployPoint( final DeployPoint deploy, final boolean skipIfExists )
        throws ProxyDataException;

    void storeRepositories( final Collection<Repository> repos )
        throws ProxyDataException;

    boolean storeRepository( final Repository proxy )
        throws ProxyDataException;

    boolean storeRepository( final Repository repository, final boolean skipIfExists )
        throws ProxyDataException;

    void storeGroups( final Collection<Group> groups )
        throws ProxyDataException;

    boolean storeGroup( final Group group )
        throws ProxyDataException;

    boolean storeGroup( final Group group, final boolean skipIfExists )
        throws ProxyDataException;

    void deleteDeployPoint( final DeployPoint deploy )
        throws ProxyDataException;

    void deleteDeployPoint( final String name )
        throws ProxyDataException;

    void deleteRepository( final Repository repo )
        throws ProxyDataException;

    void deleteRepository( final String name )
        throws ProxyDataException;

    void deleteGroup( final Group group )
        throws ProxyDataException;

    void deleteGroup( final String name )
        throws ProxyDataException;

    void install()
        throws ProxyDataException;

}