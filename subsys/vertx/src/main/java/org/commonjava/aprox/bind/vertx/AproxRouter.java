package org.commonjava.aprox.bind.vertx;

import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.route.RouteCollection;

public abstract class AproxRouter
    extends ApplicationRouter
{

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

    public abstract void initializeComponents();
}
