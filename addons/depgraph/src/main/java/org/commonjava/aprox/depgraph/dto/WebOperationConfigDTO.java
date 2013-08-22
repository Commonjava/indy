package org.commonjava.aprox.depgraph.dto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.cartographer.dto.RepositoryContentRecipe;
import org.commonjava.maven.galley.model.Location;

public class WebOperationConfigDTO
    extends RepositoryContentRecipe
{

    private StoreKey source;

    private Set<StoreKey> excludedSources;

    private String preset;

    private Boolean localUrls;

    public String getPreset()
    {
        return preset;
    }

    public void setPreset( final String preset )
    {
        this.preset = preset;
    }

    public StoreKey getSource()
    {
        return source;
    }

    public void setSource( final StoreKey source )
    {
        this.source = source;
    }

    public Boolean getLocalUrls()
    {
        return localUrls == null ? false : localUrls;
    }

    public void setLocalUrls( final Boolean localUrls )
    {
        this.localUrls = localUrls;
    }

    public Set<StoreKey> getExcludedSources()
    {
        return excludedSources;
    }

    public void setExcludedSources( final Set<StoreKey> excludedSources )
    {
        this.excludedSources = excludedSources;
    }

    public void calculateLocations( final StoreDataManager storeData )
        throws ProxyDataException
    {
        if ( source != null )
        {
            final ArtifactStore store = storeData.getArtifactStore( source );
            setSourceLocation( LocationUtils.toLocation( store ) );
        }

        if ( excludedSources != null )
        {
            final Set<Location> excluded = new HashSet<>();
            for ( final StoreKey key : excludedSources )
            {
                if ( key == null )
                {
                    continue;
                }

                final ArtifactStore store = storeData.getArtifactStore( key );
                excluded.add( LocationUtils.toLocation( store ) );

                if ( key.getType() == StoreType.group )
                {
                    final List<ArtifactStore> members = storeData.getOrderedConcreteStoresInGroup( key.getName() );
                    for ( final ArtifactStore member : members )
                    {
                        excluded.add( LocationUtils.toLocation( member ) );
                    }
                }
            }

            setExcludedSourceLocations( excluded );
        }
    }

}
