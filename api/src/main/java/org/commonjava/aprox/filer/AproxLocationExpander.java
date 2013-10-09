package org.commonjava.aprox.filer;

import static org.apache.commons.lang.StringUtils.join;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.galley.CacheOnlyLocation;
import org.commonjava.aprox.model.galley.GroupLocation;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Resource;
import org.commonjava.maven.galley.model.VirtualResource;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class AproxLocationExpander
    implements LocationExpander
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private StoreDataManager data;

    protected AproxLocationExpander()
    {
    }

    public AproxLocationExpander( final StoreDataManager data )
    {
        this.data = data;
    }

    @Override
    public List<Location> expand( final Location... locations )
        throws TransferException
    {
        return expand( Arrays.asList( locations ) );
    }

    @Override
    public <T extends Location> List<Location> expand( final Collection<T> locations )
        throws TransferException
    {
        final List<Location> result = new ArrayList<>();
        for ( final Location location : locations )
        {
            if ( location instanceof GroupLocation )
            {
                final GroupLocation gl = (GroupLocation) location;
                try
                {
                    logger.info( "Expanding group: %s", gl.getKey() );
                    final List<ArtifactStore> members = data.getOrderedConcreteStoresInGroup( gl.getKey()
                                                                                                .getName() );
                    if ( members != null )
                    {
                        for ( final ArtifactStore member : members )
                        {
                            if ( !result.contains( member ) )
                            {
                                logger.info( "expansion += %s", member.getKey() );
                                result.add( LocationUtils.toLocation( member ) );
                            }
                        }
                        logger.info( "Expanded group: %s to:\n  %s", gl.getKey(), join( result, "\n  " ) );
                    }
                }
                catch ( final ProxyDataException e )
                {
                    throw new TransferException( "Failed to lookup ordered concrete artifact stores contained in group: %s. Reason: %s", e, gl,
                                                 e.getMessage() );
                }
            }
            else if ( location instanceof CacheOnlyLocation && !( (CacheOnlyLocation) location ).hasDeployPoint() )
            {
                final StoreKey key = ( (KeyedLocation) location ).getKey();
                try
                {
                    final ArtifactStore store = data.getArtifactStore( key );
                    logger.info( "Adding single store: %s for location: %s", store, location );
                    result.add( LocationUtils.toLocation( store ) );
                }
                catch ( final ProxyDataException e )
                {
                    throw new TransferException( "Failed to lookup store for key: %s. Reason: %s", e, key, e.getMessage() );
                }
            }
            else
            {
                logger.info( "No expansion available for location: %s", location );
                result.add( location );
            }
        }

        return result;
    }

    @Override
    public VirtualResource expand( final Resource resource )
        throws TransferException
    {
        List<Location> locations;
        if ( resource instanceof VirtualResource )
        {
            locations = expand( ( (VirtualResource) resource ).getLocations() );
        }
        else
        {
            locations = expand( ( (ConcreteResource) resource ).getLocation() );
        }

        return new VirtualResource( locations, resource.getPath() );
    }

}
