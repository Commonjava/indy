package org.commonjava.aprox.depgraph.dto;

import java.util.HashSet;
import java.util.Set;

import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.cartographer.dto.RepositoryContentRecipe;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.commonjava.util.logging.Logger;

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

    public void calculateLocations( final LocationExpander locationExpander )
        throws TransferException
    {
        final Logger logger = new Logger( getClass() );
        if ( source != null )
        {
            setSourceLocation( LocationUtils.toCacheLocation( source ) );
            logger.info( "Set sourceLocation to: '%s'", getSourceLocation() );
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

                final Location loc = LocationUtils.toCacheLocation( key );
                excluded.add( loc );
                excluded.addAll( locationExpander.expand( loc ) );
            }

            setExcludedSourceLocations( excluded );
        }
    }

}
