/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.conf.AproxDepgraphConfig;
import org.commonjava.maven.atlas.graph.filter.DependencyFilter;
import org.commonjava.maven.atlas.graph.filter.ExtensionFilter;
import org.commonjava.maven.atlas.graph.filter.OrFilter;
import org.commonjava.maven.atlas.graph.filter.ParentFilter;
import org.commonjava.maven.atlas.graph.filter.PluginRuntimeFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.DefaultDiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.cartographer.preset.PresetSelector;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class RequestAdvisor
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private CartoDataManager carto;

    @Inject
    private AproxDepgraphConfig config;

    @Inject
    private PresetSelector presetSelector;

    public RequestAdvisor()
    {
    }

    public RequestAdvisor( final CartoDataManager carto, final PresetSelector presetSelector )
    {
        this.carto = carto;
        this.presetSelector = presetSelector;
    }

    public Set<ProjectVersionRef> getIncomplete( final ProjectVersionRef ref, final Map<String, String[]> params,
                                                 final Map<String, Object> presetParameters )
        throws CartoDataException
    {
        final ProjectRelationshipFilter filter = createRelationshipFilter( params, presetParameters );

        Set<ProjectVersionRef> incomplete;
        if ( ref != null )
        {
            incomplete = carto.getIncompleteSubgraphsFor( filter, null, ref );

            if ( incomplete != null )
            {
                if ( filter != null )
                {
                    incomplete = carto.pathFilter( filter, incomplete, ref );
                }
            }
        }
        else
        {
            incomplete = carto.getAllIncompleteSubgraphs();
        }

        return incomplete;
    }

    public ProjectRelationshipFilter createRelationshipFilter( final Map<String, String[]> params, final Map<String, Object> presetParameters )
    {
        ProjectRelationshipFilter filter =
            presetSelector.getPresetFilter( getFirstParameterValue( params, "preset" ), config.getDefaultWebFilterPreset(), presetParameters );

        if ( filter != null )
        {
            return filter;
        }

        final List<ProjectRelationshipFilter> filters = new ArrayList<ProjectRelationshipFilter>();
        if ( getBooleanParamWithDefault( params, "d", true ) )
        {
            filters.add( new DependencyFilter( DependencyScope.getScope( getStringParamWithDefault( params, "s", "runtime" ) ) ) );
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
        logger.info( "FILTER:\n\n%s\n\n", filter );

        return filter;
    }

    public DiscoveryConfig createDiscoveryConfig( final Map<String, String[]> params, final URI source, final DiscoverySourceManager sourceFactory )
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

    public void checkForIncompleteOrVariableGraphs( final EProjectGraph graph )
        throws AproxWorkflowException
    {
        // TODO Report to log...
        // TODO Find a way to advise the client that the response is incomplete.
    }

}
