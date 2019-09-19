/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.core.ctl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.dto.EndpointView;
import org.commonjava.indy.model.core.dto.EndpointViewListing;
import org.commonjava.indy.model.spi.AddOnListing;
import org.commonjava.indy.model.spi.IndyAddOnID;
import org.commonjava.indy.spi.IndyAddOn;
import org.commonjava.indy.stats.IndyVersioning;
import org.commonjava.indy.subsys.template.IndyGroovyException;
import org.commonjava.indy.subsys.template.TemplatingEngine;
import org.commonjava.indy.util.ApplicationStatus;
import org.commonjava.indy.util.UriFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class StatsController
{

    private static final String ADDONS_KEY = "addonsJson";

    private static final String ACTIVE_ADDONS_JS = "active-addons-js";

    private static final String ADDONS_LOGIC = "addonsLogic";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private IndyVersioning versioning;

    @Inject
    private StoreDataManager dataManager;

    @Inject
    private TemplatingEngine templates;

    @Inject
    private ObjectMapper serializer;

    @Inject
    private Instance<IndyAddOn> addonsInjected;

    private Set<IndyAddOn> addons;

    protected StatsController()
    {
    }

    public StatsController( final StoreDataManager dataManager, final TemplatingEngine templates,
                            final ObjectMapper serializer, final IndyVersioning versioning,
                            final Set<IndyAddOn> addons )
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
        addons = new HashSet<IndyAddOn>();

        if ( addonsInjected != null )
        {
            for ( final IndyAddOn addon : addonsInjected )
            {
                addons.add( addon );
            }
        }
    }

    public AddOnListing getActiveAddOns()
    {
        final List<IndyAddOnID> ids = new ArrayList<IndyAddOnID>();
        if ( addons != null )
        {
            logger.info( "Getting list of installed add-ons..." );
            for ( final IndyAddOn addon : addons )
            {
                final IndyAddOnID id = addon.getId();
                logger.info( "Adding {}", id );
                ids.add( id );
            }
        }

        return new AddOnListing( ids );
    }

    public IndyVersioning getVersionInfo()
    {
        return versioning;
    }

    public EndpointViewListing getEndpointsListing( final String baseUri, final UriFormatter uriFormatter )
        throws IndyWorkflowException
    {
        final List<ArtifactStore> stores = new ArrayList<ArtifactStore>();
        try
        {
            stores.addAll( dataManager.getAllArtifactStores() );
        }
        catch ( final IndyDataException e )
        {
            throw new IndyWorkflowException( ApplicationStatus.SERVER_ERROR.code(),
                                              "Failed to retrieve all endpoints: {}",
                                              e, e.getMessage() );
        }

        final List<EndpointView> points = new ArrayList<EndpointView>();
        for ( final ArtifactStore store : stores )
        {
            final StoreKey key = store.getKey();
            final String resourceUri =
                uriFormatter.formatAbsolutePathTo( baseUri, "content", key.getPackageType(), key.getType()
                                                               .singularEndpointName(), key.getName() );

            final EndpointView point = new EndpointView( store, resourceUri );
            if ( !points.contains( point ) )
            {
                points.add( point );
            }
        }

        return new EndpointViewListing( points );
    }

    public String getActiveAddOnsJavascript()
        throws IndyWorkflowException
    {
        try
        {
            final String json = serializer.writeValueAsString( getActiveAddOns() );
            final Map<String, Object> params = new HashMap<>();

            final Map<String, String> jsMap = new HashMap<>();
            if ( addons != null )
            {
                final ClassLoader cl = Thread.currentThread()
                                             .getContextClassLoader();
                for ( final IndyAddOn addon : addons )
                {
                    final String jsRef = addon.getId()
                                              .getInitJavascriptHref();
                    if ( jsRef == null )
                    {
                        logger.debug( "Add-On has no init javascript: {}", addon );
                        continue;
                    }

                    try (InputStream in = cl.getResourceAsStream( jsRef ))
                    {
                        if ( in == null )
                        {
                            logger.error( "Add-On failed to load: {}. Initialization javascript NOT FOUND in classpath: {}",
                                          addon, jsRef );
                            continue;
                        }

                        jsMap.put( jsRef, IOUtils.toString( in ) );
                    }
                    catch ( final IOException e )
                    {
                        logger.error( "Add-On failed to load: {}. Cannot load initialization javascript from classpath: {}",
                                      addon, jsRef );
                    }
                }
            }

            params.put( ADDONS_KEY, json );
            params.put( ADDONS_LOGIC, jsMap );

            return templates.render( ACTIVE_ADDONS_JS, params );
        }
        catch ( final IndyGroovyException e )
        {
            throw new IndyWorkflowException( "Failed to render javascript wrapper for active addons. Reason: %s", e,
                                              e.getMessage() );
        }
        catch ( final JsonProcessingException e )
        {
            throw new IndyWorkflowException( "Failed to render javascript wrapper for active addons. Reason: %s", e,
                                              e.getMessage() );
        }
    }

}
