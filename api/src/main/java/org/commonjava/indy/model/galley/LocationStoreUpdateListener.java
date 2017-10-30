package org.commonjava.indy.model.galley;

import org.commonjava.indy.change.event.ArtifactStorePreUpdateEvent;
import org.commonjava.indy.util.LocationUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

/**
 * This listener is responsible for clearing / re-creating {@link org.commonjava.maven.galley.model.Location}
 * instances associated with {@link org.commonjava.indy.model.core.ArtifactStore}s when the store gets updated.
 * Any number of configuration parameters on the store can change the way the location instance behaves, so the
 * location has to be synchronized to the new store state.
 *
 * We're caching these location instances to avoid the need to re-create them for each new
 * {@link org.commonjava.maven.galley.model.ConcreteResource}, which is often used as a key to a list of in-memory
 * {@link org.commonjava.maven.galley.model.Transfer} instances, to help with file locking in Galley's
 * {@link org.commonjava.maven.galley.spi.cache.CacheProvider} implementations.
 *
 * Created by jdcasey on 10/26/17.
 */
@ApplicationScoped
public class LocationStoreUpdateListener
{
    public void storeUpdated( @Observes ArtifactStorePreUpdateEvent event )
    {
        event.forEach( (store)->{
            // remove and re-initialize the associated KeyedLocation.
            store.removeTransientMetadata( LocationUtils.KEYED_LOCATION_METADATA );
            LocationUtils.toLocation( store );
        } );
    }
}
