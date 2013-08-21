package org.commonjava.aprox.depgraph.dto;

import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.cartographer.dto.RepositoryContentRecipe;
import org.commonjava.maven.galley.model.Location;

public class WebOperationConfigDTO
    extends RepositoryContentRecipe
{

    private StoreKey source;

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

    @Override
    public Location getSourceLocation()
    {
        return LocationUtils.toLocation( source );
    }

    public Boolean getLocalUrls()
    {
        return localUrls == null ? false : localUrls;
    }

    public void setLocalUrls( final Boolean localUrls )
    {
        this.localUrls = localUrls;
    }

}
