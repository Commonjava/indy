/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.depgraph.dto;

import java.util.HashSet;
import java.util.Set;

import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.cartographer.dto.RepositoryContentRecipe;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebOperationConfigDTO
    extends RepositoryContentRecipe
{

    private StoreKey source;

    private Set<StoreKey> excludedSources;

    private Boolean localUrls;

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
        final Logger logger = LoggerFactory.getLogger( getClass() );
        if ( source != null )
        {
            setSourceLocation( LocationUtils.toCacheLocation( source ) );
            logger.debug( "Set sourceLocation to: '{}'", getSourceLocation() );
        }

        if ( excludedSources != null )
        {
            final Set<Location> excluded = new HashSet<Location>();
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
