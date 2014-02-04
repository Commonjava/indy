package org.commonjava.aprox.dotmaven.vertx;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.aprox.bind.vertx.AproxRouter;
import org.commonjava.aprox.dotmaven.inject.DotMavenApp;
import org.commonjava.util.logging.Logger;
import org.commonjava.vertx.vabr.filter.FilterCollection;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.route.RouteCollection;
import org.jboss.weld.environment.se.events.ContainerInitialized;

@ApplicationScoped
public class DotMavenRouter
    extends AproxRouter
{

    public static final String PREFIX = "/mavdav";

    private final Logger logger = new Logger( getClass() );

    @Inject
    @DotMavenApp
    private Instance<RequestHandler> handlerInstances;

    @Inject
    @DotMavenApp
    private Instance<RouteCollection> routeCollectionInstances;

    @Inject
    @DotMavenApp
    private Instance<FilterCollection> filterCollectionInstances;

    protected DotMavenRouter()
    {
        super( PREFIX );
    }

    public DotMavenRouter( final Set<RequestHandler> handlers, final Set<RouteCollection> routeCollections,
                           final Set<FilterCollection> filterCollections )
    {
        super( PREFIX, handlers, routeCollections );
        bindFilters( handlers, filterCollections );
    }

    public void containerInit( @Observes final Event<ContainerInitialized> evt )
    {
        initializeComponents();
    }

    @Override
    @PostConstruct
    public void initializeComponents()
    {
        logger.info( "\n\nCONSTRUCTING WEB ROUTES FOR Dot-Maven add-on...\n\n" );
        bindRoutes( handlerInstances, routeCollectionInstances );
        bindFilters( handlerInstances, filterCollectionInstances );
        logger.info( "\n\n...done.\n\n" );
    }

}
