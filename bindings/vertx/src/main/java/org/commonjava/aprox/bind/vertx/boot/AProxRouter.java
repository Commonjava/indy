package org.commonjava.aprox.bind.vertx.boot;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.util.logging.Logger;
import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.filter.FilterCollection;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.route.RouteCollection;
import org.jboss.weld.environment.se.events.ContainerInitialized;

@ApplicationScoped
@Named( "rest" )
public class AProxRouter
    extends ApplicationRouter
{

    public static final String PREFIX = "/api/1.0";

    private final Logger logger = new Logger( getClass() );

    @Inject
    private Instance<RequestHandler> handlerInstances;

    @Inject
    private Instance<RouteCollection> routeCollectionInstances;

    @Inject
    private Instance<FilterCollection> filterCollectionInstances;

    protected AProxRouter()
    {
        super( PREFIX );
    }

    public AProxRouter( final Set<RequestHandler> handlers, final Set<RouteCollection> routeCollections, final Set<FilterCollection> filterCollections )
    {
        super( PREFIX, handlers, routeCollections );
        bindFilters( handlers, filterCollections );
    }

    public void containerInit( @Observes final Event<ContainerInitialized> evt )
    {
        cdiInit();
    }

    @PostConstruct
    public void cdiInit()
    {
        logger.info( "\n\nCONSTRUCTING WEB ROUTES FOR APROX...\n\n" );
        bindRoutes( handlerInstances, routeCollectionInstances );
        bindFilters( handlerInstances, filterCollectionInstances );
        logger.info( "\n\n...done.\n\n" );
    }

}
