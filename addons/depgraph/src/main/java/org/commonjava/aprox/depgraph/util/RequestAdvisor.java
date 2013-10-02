package org.commonjava.aprox.depgraph.util;

import static org.commonjava.aprox.depgraph.util.RequestUtils.getBooleanParamWithDefault;
import static org.commonjava.aprox.depgraph.util.RequestUtils.getLongParamWithDefault;
import static org.commonjava.aprox.depgraph.util.RequestUtils.getStringParamWithDefault;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response.ResponseBuilder;

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

    public Set<ProjectVersionRef> getIncomplete( final ProjectVersionRef ref, final HttpServletRequest request )
        throws CartoDataException
    {
        final ProjectRelationshipFilter filter = createRelationshipFilter( request );

        Set<ProjectVersionRef> incomplete;
        if ( ref != null )
        {
            incomplete = carto.getIncompleteSubgraphsFor( filter, ref );

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

    public ProjectRelationshipFilter createRelationshipFilter( final HttpServletRequest request )
    {
        ProjectRelationshipFilter filter = presetSelector.getPresetFilter( request.getParameter( "preset" ), config.getDefaultWebFilterPreset() );
        if ( filter != null )
        {
            return filter;
        }

        final List<ProjectRelationshipFilter> filters = new ArrayList<ProjectRelationshipFilter>();
        if ( getBooleanParamWithDefault( request, "d", true ) )
        {
            filters.add( new DependencyFilter( DependencyScope.getScope( getStringParamWithDefault( request, "s", "runtime" ) ) ) );
        }
        if ( getBooleanParamWithDefault( request, "pa", true ) )
        {
            filters.add( new ParentFilter( false ) );
        }
        if ( getBooleanParamWithDefault( request, "pl", false ) )
        {
            filters.add( new PluginRuntimeFilter() );
        }
        if ( getBooleanParamWithDefault( request, "e", false ) )
        {
            filters.add( new ExtensionFilter() );
        }

        filter = new OrFilter( filters );
        logger.info( "FILTER:\n\n%s\n\n", filter );

        return filter;
    }

    public DiscoveryConfig createDiscoveryConfig( final HttpServletRequest request, final URI source, final DiscoverySourceManager sourceFactory )
    {
        DiscoveryConfig result = DiscoveryConfig.DISABLED;
        if ( getBooleanParamWithDefault( request, "discover", false ) )
        {
            URI s = source;
            if ( s == null )
            {
                s = sourceFactory.createSourceURI( getStringParamWithDefault( request, "from", null ) );
            }

            final DefaultDiscoveryConfig c = new DefaultDiscoveryConfig( s );
            result = c;

            c.setEnabled( true );
            c.setTimeoutMillis( getLongParamWithDefault( request, "timeout", c.getTimeoutMillis() ) );
        }

        return result;
    }

    public void checkForIncompleteOrVariableGraphs( final EProjectGraph graph, final ResponseBuilder rb )
    {
        // TODO Report to log...
        // TODO Find a way to advise the client that the response is incomplete.
    }

}
