package org.commonjava.indy.pathmapped.inject;

import org.commonjava.indy.core.content.group.AbstractGroupRepositoryFilter;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.maven.galley.cache.pathmapped.PathMappedCacheProvider;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.storage.pathmapped.core.PathMappedFileManager;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class PathMappedGroupRepositoryFilter
                extends AbstractGroupRepositoryFilter
{
    @Inject
    private CacheProvider cacheProvider;

    private PathMappedFileManager pathMappedFileManager;

    @PostConstruct
    void setup()
    {
        if ( cacheProvider instanceof PathMappedCacheProvider )
        {
            pathMappedFileManager = ( (PathMappedCacheProvider) cacheProvider ).getPathMappedFileManager();
        }
    }

    @Override
    public int getPriority()
    {
        return 0;
    }

    @Override
    public boolean canProcess( String path, Group group )
    {
        if ( pathMappedFileManager != null )
        {
            return true;
        }
        return false;
    }

    /**
     * Filter for remote repos plus hosted repos which contains the target path. Because caller may try to
     * download the target path from remote repositories.
     */
    @Override
    public List<ArtifactStore> filter( String path, Group group, List<ArtifactStore> concreteStores )
    {
        List<String> candidates = getCandidates( concreteStores );

        // we query by parent path because 1. maven metadata generator need to list it, 2. it is supper set of file path
        String parentPath = Paths.get( path ).getParent().toString();
        Set<String> ret = pathMappedFileManager.getFileSystemContainingDirectory( candidates, parentPath );

        return concreteStores.stream()
                             .filter( store -> store.getType() == StoreType.remote || ret.contains(
                                             store.getKey().toString() ) )
                             .collect( Collectors.toList() );
    }

    /**
     * Get hosted repos
     */
    private List<String> getCandidates( List<ArtifactStore> concreteStores )
    {
        return concreteStores.stream()
                             .filter( store -> store.getType() == StoreType.hosted )
                             .map( store -> store.getKey().toString() )
                             .collect( Collectors.toList() );
    }
}
