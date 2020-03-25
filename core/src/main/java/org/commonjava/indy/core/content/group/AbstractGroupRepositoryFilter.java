package org.commonjava.indy.core.content.group;

import org.commonjava.indy.content.GroupRepositoryFilter;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;

import java.util.List;

public abstract class AbstractGroupRepositoryFilter
                implements GroupRepositoryFilter
{
    @Override
    public boolean canProcess( String path, Group group )
    {
        return true;
    }

    @Override
    public List<ArtifactStore> filter( String path, Group group, List<ArtifactStore> concreteStores )
    {
        return concreteStores;
    }

    @Override
    public int compareTo( GroupRepositoryFilter groupRepositoryFilter )
    {
        int other = groupRepositoryFilter.getPriority();
        if ( getPriority() > other )
        {
            return 1;
        }
        else if ( getPriority() < other )
        {
            return -1;
        }
        else
        {
            return 0;
        }
    }
}
