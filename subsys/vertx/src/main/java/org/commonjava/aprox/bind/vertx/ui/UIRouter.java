package org.commonjava.aprox.bind.vertx.ui;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.bind.vertx.AproxRouter;
import org.commonjava.vertx.vabr.filter.FilterCollection;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.route.RouteCollection;

@ApplicationScoped
@Named( "ui" )
public class UIRouter
    extends AproxRouter
{

    public static final String PREFIX = "/";

    @Inject
    @UIApp
    private Instance<RequestHandler> handlers;

    @Inject
    @UIApp
    private Instance<RouteCollection> routeCollections;

    @Inject
    @UIApp
    private Instance<FilterCollection> filterCollections;

    protected UIRouter()
    {
        super( PREFIX );
    }

    public UIRouter( final Set<RequestHandler> handlers, final Set<RouteCollection> routeCollections, final Set<FilterCollection> filterCollections )
    {
        super( PREFIX, handlers, routeCollections );
        bindFilters( handlers, filterCollections );
    }

    @PostConstruct
    @Override
    public void initializeComponents()
    {
        logger.info( "\n\nCONSTRUCTING WEB ROUTES FOR UI app...\n\n" );
        bindRoutes( handlers, routeCollections );
        bindFilters( handlers, filterCollections );
        logger.info( "\n\n...done.\n\n" );
    }

}
