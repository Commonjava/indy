package org.commonjava.aprox.bind.vertx;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.route.RouteCollection;
import org.commonjava.vertx.vabr.route.RouteHandler;

@ApplicationScoped
public class AProxRouter
    extends ApplicationRouter
{

    public static final String PREFIX = "/api/1.0";

    @Inject
    private Instance<RouteHandler> routeHandlerInstances;

    @Inject
    private Instance<RouteCollection> routeCollectionInstances;

    protected AProxRouter()
    {
        super( PREFIX );
    }

    public AProxRouter( final Set<RouteHandler> handlers, final Set<RouteCollection> collections )
    {
        super( PREFIX, handlers, collections );
    }

    @PostConstruct
    public void cdiInit()
    {
        bindRoutes( routeHandlerInstances, routeCollectionInstances );
    }

}
