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
package org.commonjava.aprox.depgraph.util;

import static org.commonjava.aprox.util.RequestUtils.getBooleanParamWithDefault;
import static org.commonjava.aprox.util.RequestUtils.getFirstParameterValue;
import static org.commonjava.aprox.util.RequestUtils.getLongParamWithDefault;
import static org.commonjava.aprox.util.RequestUtils.getStringParamWithDefault;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.depgraph.conf.AproxDepgraphConfig;
import org.commonjava.maven.atlas.graph.filter.DependencyFilter;
import org.commonjava.maven.atlas.graph.filter.ExtensionFilter;
import org.commonjava.maven.atlas.graph.filter.OrFilter;
import org.commonjava.maven.atlas.graph.filter.ParentFilter;
import org.commonjava.maven.atlas.graph.filter.PluginRuntimeFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DefaultDiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.cartographer.preset.PresetSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RequestAdvisor
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    protected AproxDepgraphConfig config;

    @Inject
    protected PresetSelector presetSelector;

    protected RequestAdvisor()
    {
    }

    public RequestAdvisor( final PresetSelector presetSelector )
    {
        this.presetSelector = presetSelector;
    }

    public ProjectRelationshipFilter createRelationshipFilter( final Map<String, String[]> params,
                                                               final Map<String, Object> presetParameters )
    {
        ProjectRelationshipFilter filter =
            presetSelector.getPresetFilter( getFirstParameterValue( params, "preset" ),
                                            config.getDefaultWebFilterPreset(), presetParameters );

        if ( filter != null )
        {
            return filter;
        }

        final List<ProjectRelationshipFilter> filters = new ArrayList<ProjectRelationshipFilter>();
        if ( getBooleanParamWithDefault( params, "d", true ) )
        {
            filters.add( new DependencyFilter( DependencyScope.getScope( getStringParamWithDefault( params, "s",
                                                                                                    "runtime" ) ) ) );
        }
        if ( getBooleanParamWithDefault( params, "pa", true ) )
        {
            filters.add( ParentFilter.EXCLUDE_TERMINAL_PARENTS );
        }
        if ( getBooleanParamWithDefault( params, "pl", false ) )
        {
            filters.add( new PluginRuntimeFilter() );
        }
        if ( getBooleanParamWithDefault( params, "e", false ) )
        {
            filters.add( new ExtensionFilter() );
        }

        filter = new OrFilter( filters );
        logger.info( "FILTER:\n\n{}\n\n", filter );

        return filter;
    }

    public DiscoveryConfig createDiscoveryConfig( final Map<String, String[]> params, final URI source,
                                                  final DiscoverySourceManager sourceFactory )
        throws CartoDataException
    {
        DiscoveryConfig result = DiscoveryConfig.DISABLED;
        if ( getBooleanParamWithDefault( params, "discover", false ) )
        {
            URI s = source;
            if ( s == null )
            {
                s = sourceFactory.createSourceURI( getStringParamWithDefault( params, "from", null ) );
            }

            final DefaultDiscoveryConfig c = new DefaultDiscoveryConfig( s );
            result = c;

            c.setEnabled( true );
            c.setTimeoutMillis( getLongParamWithDefault( params, "timeout", c.getTimeoutMillis() ) );
        }

        return result;
    }

}
