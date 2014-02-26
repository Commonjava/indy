package org.commonjava.aprox.bind.vertx;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;

import org.commonjava.aprox.core.rest.AdminController;
import org.commonjava.aprox.inject.RestApp;
import org.commonjava.vertx.vabr.filter.FilterCollection;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.route.RouteCollection;
import org.jboss.weld.environment.se.events.ContainerInitialized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Named( "rest" )
public class RestRouter
    extends AproxRouter
{

    public static final String PREFIX = "/api/1.0";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Instance<RequestHandler> handlerInstances;

    @Inject
    private Instance<RouteCollection> routeCollectionInstances;

    @Inject
    private Instance<FilterCollection> filterCollectionInstances;

    @Inject
    private AdminController adminController;

    protected RestRouter()
    {
        super( PREFIX );
    }

    public RestRouter( final Set<RequestHandler> handlers, final Set<RouteCollection> routeCollections, final Set<FilterCollection> filterCollections )
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
        adminController.started();

        logger.info( "\n\nCONSTRUCTING WEB ROUTES FOR APROX...\n\n" );

        final Set<RouteCollection> routes = new HashSet<>();
        nextRoute: for ( final RouteCollection collection : routeCollectionInstances )
        {
            final Annotation[] annotations = collection.getClass()
                                                       .getAnnotations();
            boolean foundQualifier = false;
            for ( final Annotation annotation : annotations )
            {
                if ( annotation.getClass()
                               .getAnnotation( Qualifier.class ) != null )
                {
                    foundQualifier = true;
                    if ( RestApp.class.equals( annotation.getClass() ) )
                    {
                        routes.add( collection );
                        continue nextRoute;
                    }
                }
            }

            if ( !foundQualifier )
            {
                routes.add( collection );
            }
        }

        final Set<FilterCollection> filters = new HashSet<>();
        nextFilter: for ( final FilterCollection collection : filterCollectionInstances )
        {
            final Annotation[] annotations = collection.getClass()
                                                       .getAnnotations();
            boolean foundQualifier = false;
            for ( final Annotation annotation : annotations )
            {
                if ( annotation.getClass()
                               .getAnnotation( Qualifier.class ) != null )
                {
                    foundQualifier = true;
                    if ( RestApp.class.equals( annotation.getClass() ) )
                    {
                        filters.add( collection );
                        continue nextFilter;
                    }
                }
            }

            if ( !foundQualifier )
            {
                filters.add( collection );
            }
        }

        bindRoutes( handlerInstances, routes );
        bindFilters( handlerInstances, filters );
        logger.info( "\n\n...done.\n\n" );
    }

    @PreDestroy
    public void stopped()
    {
        adminController.stopped();
    }

}
