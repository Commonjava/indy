package org.commonjava.aprox.depgraph.discover;

import static org.commonjava.aprox.depgraph.util.AproxDepgraphUtils.toDiscoveryURI;

import java.net.URI;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.inject.Production;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;

@ApplicationScoped
@Production
@Default
public class AproxDiscoverySourceManager
    implements DiscoverySourceManager
{

    @Inject
    private StoreDataManager stores;

    protected AproxDiscoverySourceManager()
    {
    }

    public AproxDiscoverySourceManager( final StoreDataManager stores )
    {
        this.stores = stores;
    }

    @Override
    public URI createSourceURI( final String source )
    {
        final StoreKey key = StoreKey.fromString( source );
        return toDiscoveryURI( key );
    }

    @Override
    public String getFormatHint()
    {
        return "<store-type>:<store-name>";
    }

    @Override
    public void activateWorkspaceSources( final GraphWorkspace ws, final String... sources )
        throws CartoDataException
    {
        if ( ws != null )
        {
            for ( final String src : sources )
            {
                final StoreKey key = StoreKey.fromString( src );
                if ( key.getType() == StoreType.group )
                {
                    try
                    {
                        final List<ArtifactStore> orderedStores =
                            stores.getOrderedConcreteStoresInGroup( key.getName() );
                        for ( final ArtifactStore store : orderedStores )
                        {
                            ws.addActiveSource( toDiscoveryURI( store.getKey() ) );
                        }
                    }
                    catch ( final ProxyDataException e )
                    {
                        throw new CartoDataException( "Failed to lookup ordered concrete stores for: %s. Reason: %s",
                                                      e, key, e.getMessage() );
                    }
                }
                else
                {
                    ws.addActiveSource( toDiscoveryURI( key ) );
                }
            }
        }
    }

}
