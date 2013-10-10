package org.commonjava.aprox.depgraph.dto;

import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.cartographer.dto.MetadataCollationRecipe;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.commonjava.util.logging.Logger;

public class MetadataCollationDTO
    extends MetadataCollationRecipe
{

    private StoreKey source;

    public void calculateLocations( final LocationExpander locationExpander )
        throws TransferException
    {
        final Logger logger = new Logger( getClass() );
        if ( source != null )
        {
            setSourceLocation( LocationUtils.toCacheLocation( source ) );
            logger.info( "Set sourceLocation to: '%s'", getSourceLocation() );
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
