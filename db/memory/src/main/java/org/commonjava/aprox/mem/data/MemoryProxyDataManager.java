package org.commonjava.aprox.mem.data;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.ProxyDataManager;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreKey;

public class MemoryProxyDataManager
    implements ProxyDataManager
{

    @Override
    public DeployPoint getDeployPoint( String name )
        throws ProxyDataException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Repository getRepository( String name )
        throws ProxyDataException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Group getGroup( String name )
        throws ProxyDataException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends Group> getAllGroups()
        throws ProxyDataException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends Repository> getAllRepositories()
        throws ProxyDataException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends DeployPoint> getAllDeployPoints()
        throws ProxyDataException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends ArtifactStore> getOrderedConcreteStoresInGroup( String groupName )
        throws ProxyDataException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<? extends Group> getGroupsContaining( StoreKey repo )
        throws ProxyDataException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void storeDeployPoints( Collection<DeployPoint> deploys )
        throws ProxyDataException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean storeDeployPoint( DeployPoint deploy )
        throws ProxyDataException
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean storeDeployPoint( DeployPoint deploy, boolean skipIfExists )
        throws ProxyDataException
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void storeRepositories( Collection<Repository> repos )
        throws ProxyDataException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean storeRepository( Repository proxy )
        throws ProxyDataException
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean storeRepository( Repository repository, boolean skipIfExists )
        throws ProxyDataException
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void storeGroups( Collection<Group> groups )
        throws ProxyDataException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean storeGroup( Group group )
        throws ProxyDataException
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean storeGroup( Group group, boolean skipIfExists )
        throws ProxyDataException
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void deleteDeployPoint( DeployPoint deploy )
        throws ProxyDataException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteDeployPoint( String name )
        throws ProxyDataException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteRepository( Repository repo )
        throws ProxyDataException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteRepository( String name )
        throws ProxyDataException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteGroup( Group group )
        throws ProxyDataException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteGroup( String name )
        throws ProxyDataException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void install()
        throws ProxyDataException
    {
        // TODO Auto-generated method stub

    }

}
