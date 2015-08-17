/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.aprox.depgraph.util;

import static org.commonjava.aprox.model.util.HttpUtils.getBooleanParamWithDefault;
import static org.commonjava.aprox.model.util.HttpUtils.getFirstParameterValue;
import static org.commonjava.aprox.model.util.HttpUtils.getLongParamWithDefault;
import static org.commonjava.aprox.model.util.HttpUtils.getStringParamWithDefault;

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
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.graph.discover.DiscoveryConfig;
import org.commonjava.cartographer.spi.graph.discover.DiscoverySourceManager;
import org.commonjava.cartographer.graph.preset.PresetSelector;
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
        DiscoveryConfig result = null;
        if ( getBooleanParamWithDefault( params, "discover", false ) )
        {
            URI s = source;
            if ( s == null )
            {
                s = sourceFactory.createSourceURI( getStringParamWithDefault( params, "from", null ) );
            }

            final DiscoveryConfig c = new DiscoveryConfig( s );
            result = c;

            c.setEnabled( true );
            c.setTimeoutMillis( getLongParamWithDefault( params, "timeout", c.getTimeoutMillis() ) );
        }

        return result == null ? DiscoveryConfig.getDisabledConfig() : result;
    }

}
