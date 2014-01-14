package org.commonjava.aprox.core.rest;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.core.model.EndpointViewListing;
import org.commonjava.aprox.core.util.UriFormatter;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.commonjava.aprox.stats.AProxVersioning;

@ApplicationScoped
public class StatsController
{
    @Inject
    private AProxVersioning versioning;

    @Inject
    private StoreDataManager dataManager;

    protected StatsController()
    {
    }

    public StatsController( final StoreDataManager storeManager, final AProxVersioning versioning )
    {
        dataManager = storeManager;
        this.versioning = versioning;
    }

    public AProxVersioning getVersionInfo()
    {
        return versioning;
    }

    public EndpointViewListing getEndpointsListing( final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        final List<ArtifactStore> stores = new ArrayList<ArtifactStore>();
        try
        {
            stores.addAll( dataManager.getAllArtifactStores() );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to retrieve all endpoints: %s", e, e.getMessage() );
        }

        return new EndpointViewListing( stores, uriFormatter );
    }

}
