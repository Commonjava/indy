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
package org.commonjava.aprox.core.rest;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.dto.EndpointViewListing;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.stats.AProxVersioning;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.aprox.util.UriFormatter;

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

    public EndpointViewListing getEndpointsListing( final String baseUri, final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        final List<ArtifactStore> stores = new ArrayList<ArtifactStore>();
        try
        {
            stores.addAll( dataManager.getAllArtifactStores() );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to retrieve all endpoints: {}", e, e.getMessage() );
        }

        return new EndpointViewListing( stores, baseUri, uriFormatter );
    }

}
