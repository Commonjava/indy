package org.commonjava.indy.pkg.maven.change;

import org.commonjava.indy.content.StoreContentAction;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.pkg.maven.content.MetadataCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Set;

@ApplicationScoped
public class MetadataStoreContentAction
                implements StoreContentAction
{
    private Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private MetadataCacheManager cacheManager;

    public MetadataStoreContentAction()
    {
    }

    @Override
    public void clearStoreContent( String path, ArtifactStore store, Set<Group> affectedGroups,
                                   boolean clearOriginPath )
    {
        logger.debug( "Clearing metadata cache, path: {}, store: {}, affected: {}", path, store.getKey(), affectedGroups );
        cacheManager.remove( store.getKey(), path );
        affectedGroups.forEach( group -> cacheManager.remove( group.getKey(), path ) );
    }
}
