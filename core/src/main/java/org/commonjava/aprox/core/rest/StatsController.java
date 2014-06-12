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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.dto.AddOnListing;
import org.commonjava.aprox.dto.EndpointViewListing;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.spi.AproxAddOn;
import org.commonjava.aprox.spi.AproxAddOnID;
import org.commonjava.aprox.stats.AProxVersioning;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.aprox.util.UriFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StatsController
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private AProxVersioning versioning;

    @Inject
    private StoreDataManager dataManager;

    @Inject
    private Instance<AproxAddOn> addonsInjected;

    private Set<AproxAddOn> addons;

    protected StatsController()
    {
    }

    public StatsController( final StoreDataManager storeManager, final AProxVersioning versioning,
                            final Set<AproxAddOn> addons )
    {
        dataManager = storeManager;
        this.versioning = versioning;
        this.addons = addons;
    }

    @PostConstruct
    public void init()
    {
        addons = new HashSet<AproxAddOn>();

        if ( addonsInjected != null )
        {
            for ( final AproxAddOn addon : addonsInjected )
            {
                addons.add( addon );
            }
        }
    }

    public AddOnListing getActiveAddOns()
    {
        final List<AproxAddOnID> ids = new ArrayList<AproxAddOnID>();
        if ( addons != null )
        {
            logger.info( "Getting list of installed add-ons..." );
            for ( final AproxAddOn addon : addons )
            {
                final AproxAddOnID id = addon.getId();
                logger.info( "Adding {}", id );
                ids.add( id );
            }
        }

        return new AddOnListing( ids );
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
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to retrieve all endpoints: {}",
                                              e, e.getMessage() );
        }

        return new EndpointViewListing( stores, baseUri, uriFormatter );
    }

}
