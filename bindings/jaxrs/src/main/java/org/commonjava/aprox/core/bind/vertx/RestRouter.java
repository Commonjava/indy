/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.core.bind.vertx;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;

import org.commonjava.aprox.bind.jaxrs.AproxDeployment;
import org.commonjava.aprox.inject.RestApp;
import org.commonjava.vertx.vabr.ApplicationRouterConfig;
import org.commonjava.vertx.vabr.bind.filter.FilterCollection;
import org.commonjava.vertx.vabr.bind.route.RouteCollection;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.jboss.weld.environment.se.events.ContainerInitialized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Named( "rest" )
public class RestRouter
    extends AproxDeployment
{

    private static final Map<String, String> ROUTE_ALIASES = Collections.unmodifiableMap( new HashMap<String, String>()
    {
        {
            put( "/1.0/", "/" );
        }

        private static final long serialVersionUID = 1L;
    } );

    public static final String PREFIX = "/api";

    public static final String APROX_ID = "aprox";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Instance<RequestHandler> handlerInstances;

    @Inject
    private Instance<RouteCollection> routeCollectionInstances;

    @Inject
    private Instance<FilterCollection> filterCollectionInstances;

    public RestRouter()
    {
        super( new ApplicationRouterConfig().withAppAcceptId( APROX_ID )
                                            .withPrefix( PREFIX )
                                            .withRouteAliases( ROUTE_ALIASES ) );
    }

    public RestRouter( final Set<RequestHandler> handlers, final List<RouteCollection> routeCollections,
                       final List<FilterCollection> filterCollections )
    {
        super( new ApplicationRouterConfig().withAppAcceptId( APROX_ID )
                                            .withPrefix( PREFIX )
                                            .withHandlers( handlers )
                                            .withRouteCollections( routeCollections )
                                            .withFilterCollections( filterCollections )
                                            .withRouteAliases( ROUTE_ALIASES ) );
    }

    public void containerInit( @Observes final Event<ContainerInitialized> evt )
    {
        initializeComponents();
    }

    @Override
    @PostConstruct
    public void initializeComponents()
    {
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

        bindHandlers( handlerInstances );
        bindRouteCollections( routes );
        bindFilterCollections( filters );
        logger.info( "\n\n...done.\n\n" );
    }

}
