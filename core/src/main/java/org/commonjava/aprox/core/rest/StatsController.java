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
import java.util.Collections;
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
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.spi.AproxAddOn;
import org.commonjava.aprox.spi.AproxAddOnID;
import org.commonjava.aprox.stats.AProxVersioning;
import org.commonjava.aprox.subsys.template.AproxGroovyException;
import org.commonjava.aprox.subsys.template.TemplatingEngine;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.aprox.util.UriFormatter;
import org.commonjava.web.json.ser.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StatsController
{

    private static final String ADDONS_KEY = "addonsJson";

    private static final String ACTIVE_ADDONS_JS = "active-addons-js";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private AProxVersioning versioning;

    @Inject
    private StoreDataManager dataManager;

    @Inject
    private TemplatingEngine templates;

    @Inject
    @AproxData
    private JsonSerializer serializer;

    @Inject
    private Instance<AproxAddOn> addonsInjected;

    private Set<AproxAddOn> addons;

    protected StatsController()
    {
    }

    public StatsController( final StoreDataManager dataManager, final TemplatingEngine templates,
                            final JsonSerializer serializer, final AProxVersioning versioning,
                            final Set<AproxAddOn> addons )
    {
        this.dataManager = dataManager;
        this.templates = templates;
        this.serializer = serializer;
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

    public String getActiveAddOnsJavascript()
        throws AproxWorkflowException
    {
        final String json = serializer.toString( getActiveAddOns() );
        try
        {
            return templates.render( ACTIVE_ADDONS_JS, Collections.<String, Object> singletonMap( ADDONS_KEY, json ) );
        }
        catch ( final AproxGroovyException e )
        {
            throw new AproxWorkflowException( "Failed to render javascript wrapper for active addons. Reason: %s", e,
                                              e.getMessage() );
        }
    }

}
