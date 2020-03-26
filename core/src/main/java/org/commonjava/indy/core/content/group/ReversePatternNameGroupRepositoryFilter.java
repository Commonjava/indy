package org.commonjava.indy.core.content.group;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreType;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Reverse pattern is specifically useful. It excludes repos matching the filterPattern if pathPattern not matches.
 * e.g, if path not contains '-rh', exclude those with name like 'rh-build'.
 */
public abstract class ReversePatternNameGroupRepositoryFilter
                extends AbstractGroupRepositoryFilter
{
    protected Pattern pathPattern;

    protected Pattern filterPattern;

    public ReversePatternNameGroupRepositoryFilter( String pathPattern, String filterPattern )
    {
        this.pathPattern = Pattern.compile( pathPattern );
        this.filterPattern = Pattern.compile( filterPattern );
    }

    @Override
    public List<ArtifactStore> filter( String path, Group group, List<ArtifactStore> concreteStores )
    {
        Matcher matcher = pathPattern.matcher( path );
        if ( !matcher.matches() )
        {
            return concreteStores.stream()
                                 .filter( store -> store.getType() == StoreType.remote || !filterPattern.matcher(
                                                 store.getName() ).matches() )
                                 .collect( Collectors.toList() );
        }
        return concreteStores;
    }
}
