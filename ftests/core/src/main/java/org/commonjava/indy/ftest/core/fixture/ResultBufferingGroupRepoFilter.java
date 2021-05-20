package org.commonjava.indy.ftest.core.fixture;

import org.commonjava.indy.core.content.group.AbstractGroupRepositoryFilter;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

@Named
@ApplicationScoped
public class ResultBufferingGroupRepoFilter
    extends AbstractGroupRepositoryFilter
{
    private Map<StorePath, List<ArtifactStore>> resultBuffer = new HashMap<>();

    private final Logger logger = getLogger( getClass().getName() );

    @Override
    public int getPriority()
    {
        return 0;
    }

    @Override
    public boolean canProcess( String path, Group group )
    {
        return true;
    }

    @Override
    public List<ArtifactStore> filter( String path, Group group, List<ArtifactStore> concreteStores )
    {
        List<ArtifactStore> ret = super.filter( path, group, concreteStores );
        logger.info( "\n\n\nStoring filtered repos for: {}:{}", group.getKey(), path );
        resultBuffer.put( new StorePath( path, group.getKey() ), ret );
        logger.info( "Stored filtered repos for: {}:{} of: {}\n\n\n", group.getKey(), path,
                     resultBuffer.get( new StorePath( path, group.getKey() ) ) );
        return ret;
    }

    public List<ArtifactStore> getFilteredRepositories( String path, Group group )
    {
        List<ArtifactStore> ret = resultBuffer.get( new StorePath( path, group.getKey() ) );
        logger.info( "\n\n\nReturning filtered repos for: {}:{} of: {}\n\n\n", group.getKey(), path, ret );
        return ret;
    }

    private static final class StorePath
    {
        private final String path;

        private final StoreKey key;

        public StorePath( String path, StoreKey key )
        {
            this.path = path;
            this.key = key;
        }

        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
                return true;
            if ( o == null || getClass() != o.getClass() )
                return false;
            StorePath storePath = (StorePath) o;
            return Objects.equals( path, storePath.path ) && Objects.equals( key, storePath.key );
        }

        @Override
        public int hashCode()
        {
            return Objects.hash( path, key );
        }
    }

}
