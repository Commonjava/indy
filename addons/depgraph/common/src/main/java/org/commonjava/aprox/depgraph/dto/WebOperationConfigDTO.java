/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.depgraph.dto;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

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
        this.excludedSources = new TreeSet<StoreKey>( excludedSources );
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
