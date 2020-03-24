package org.commonjava.indy.core.content.group;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Reverse pattern is specifically useful. It excludes repos matching the filterPattern if pathPattern not matches.
 * e.g, if path not contains '-rh', exclude those with name like 'rh-build'.
 */
public abstract class ReversePatternNameGroupRepositoryFilter
                extends AbstractGroupRepositoryFilter
{
    protected abstract String getPathPattern();

    protected abstract String getFilterPattern();

    @Override
    public List<ArtifactStore> filter( String path, Group group, List<ArtifactStore> concreteStores )
    {
        if ( !path.matches( getPathPattern() ) )
        {
            return concreteStores.stream()
                                 .filter( store -> !store.getName().matches( getFilterPattern() ) )
                                 .collect( Collectors.toList() );
        }
        return concreteStores;
    }

    @Override
    public List<ArtifactStore> filterForFirstMatch( String path, Group group, List<ArtifactStore> concreteStores )
    {
        return filter( path, group, concreteStores );
    }

}
