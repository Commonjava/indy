package org.commonjava.aprox.tensor.discover;

import static org.commonjava.aprox.tensor.util.AproxTensorUtils.toDiscoveryURI;

import java.net.URI;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.maven.graph.effective.workspace.GraphWorkspace;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.tensor.data.TensorDataException;
import org.commonjava.tensor.data.TensorDataManager;
import org.commonjava.tensor.discover.DiscoverySourceManager;

@ApplicationScoped
public class AproxDiscoverySourceManager
    implements DiscoverySourceManager
{

    @Inject
    private TensorDataManager tensor;

    @Inject
    private StoreDataManager stores;

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
    public void activateWorkspaceSources( final String... sources )
        throws TensorDataException
    {
        final GraphWorkspace ws = tensor.getCurrentWorkspace();
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
                        throw new TensorDataException( "Failed to lookup ordered concrete stores for: %s. Reason: %s",
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
