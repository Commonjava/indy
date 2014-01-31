package org.commonjava.aprox.bind.vertx.boot;

import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.route.RouteCollection;

public abstract class AproxRouter
    extends ApplicationRouter
{

    public abstract void initializeComponents();

    protected AproxRouter()
    {
    }

    protected AproxRouter( final Iterable<?> routes, final Iterable<RouteCollection> routeCollections )
    {
        super( routes, routeCollections );
    }

    protected AproxRouter( final String prefix, final Iterable<?> routes, final Iterable<RouteCollection> routeCollections )
    {
        super( prefix, routes, routeCollections );
    }

    protected AproxRouter( final String prefix )
    {
        super( prefix );
    }

}
