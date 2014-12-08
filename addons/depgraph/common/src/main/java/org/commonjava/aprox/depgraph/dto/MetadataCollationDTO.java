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

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.cartographer.dto.MetadataCollationRecipe;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetadataCollationDTO
    extends MetadataCollationRecipe
{

    private StoreKey source;

    public void calculateLocations( final LocationExpander locationExpander, final StoreDataManager dataManager )
        throws TransferException
    {
        final Logger logger = LoggerFactory.getLogger( getClass() );
        if ( source != null )
        {
            ArtifactStore store;
            try
            {
                store = dataManager.getArtifactStore( source );
            }
            catch ( final ProxyDataException e )
            {
                throw new TransferException( "Cannot find ArtifactStore to match source key: %s. Reason: %s", e,
                                             source, e.getMessage() );
            }

            if ( store == null )
            {
                throw new TransferException( "Cannot find ArtifactStore to match source key: %s.", source );
            }

            setSourceLocation( LocationUtils.toLocation( store ) );
            logger.debug( "Set sourceLocation to: '{}'", getSourceLocation() );
        }

    }

    public StoreKey getSource()
    {
        return source;
    }

    public void setSource( final StoreKey source )
    {
        this.source = source;
    }

}
