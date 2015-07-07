package org.commonjava.aprox.model.galley;

import javax.inject.Inject;

import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.spi.transport.LocationResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AproxLocationResolver
    implements LocationResolver
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager dataManager;

    protected AproxLocationResolver()
    {
    }

    public AproxLocationResolver( final StoreDataManager dataManager )
    {
        this.dataManager = dataManager;
    }

    @Override
    public Location resolve( final String spec )
        throws TransferException
    {
        ArtifactStore store;
        try
        {
            final StoreKey source = StoreKey.fromString( spec );
            if ( source == null )
            {
                throw new TransferException(
                                             "Failed to parse StoreKey (format: '[remote|hosted|group]:name') from: '%s'." );
            }

            store = dataManager.getArtifactStore( source );
        }
        catch ( final AproxDataException e )
        {
            throw new TransferException( "Cannot find ArtifactStore to match source key: %s. Reason: %s", e, spec,
                                         e.getMessage() );
        }

        if ( store == null )
        {
            throw new TransferException( "Cannot find ArtifactStore to match source key: %s.", spec );
        }

        final KeyedLocation location = LocationUtils.toLocation( store );
        logger.debug( "resolved source location: '{}' to: '{}'", spec, location );

        return location;
    }

}
