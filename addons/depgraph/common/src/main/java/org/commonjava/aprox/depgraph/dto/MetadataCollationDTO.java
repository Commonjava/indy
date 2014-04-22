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

import org.commonjava.aprox.model.StoreKey;
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

    public void calculateLocations( final LocationExpander locationExpander )
        throws TransferException
    {
        final Logger logger = LoggerFactory.getLogger( getClass() );
        if ( source != null )
        {
            setSourceLocation( LocationUtils.toCacheLocation( source ) );
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
