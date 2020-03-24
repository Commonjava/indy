package org.commonjava.indy.content;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;

import java.util.List;

public interface GroupRepositoryFilter
                extends Comparable<GroupRepositoryFilter>
{
    /**
     * Higher priority filter get executed first.
     * @return priority
     */
    int getPriority();

    boolean canProcess( String path, Group group );

    /**
     * Get all concrete stores that may contain the target path.
     *
     * @param path
     * @param group
     * @param concreteStores in target group ( include stores in sub groups )
     * @return
     */
    List<ArtifactStore> filter( String path, Group group, List<ArtifactStore> concreteStores );

    /**
     * Get all concrete stores that may contain the first occurrence of the target path.
     *
     * @param path
     * @param group
     * @param concreteStores in target group ( include stores in sub groups )
     * @return
     */
    List<ArtifactStore> filterForFirstMatch( String path, Group group, List<ArtifactStore> concreteStores );

}
