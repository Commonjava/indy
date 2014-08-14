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
package org.commonjava.aprox.dotmaven.vertx;

import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.aprox.bind.vertx.AproxRouter;
import org.commonjava.aprox.dotmaven.inject.DotMavenApp;
import org.commonjava.vertx.vabr.ApplicationRouterConfig;
import org.commonjava.vertx.vabr.bind.filter.FilterCollection;
import org.commonjava.vertx.vabr.bind.route.RouteCollection;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.jboss.weld.environment.se.events.ContainerInitialized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DotMavenRouter
    extends AproxRouter
{

    public static final String PREFIX = "/mavdav";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

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
        super( new ApplicationRouterConfig().withPrefix( PREFIX ) );
    }

    public DotMavenRouter( final Set<RequestHandler> handlers, final List<RouteCollection> routeCollections,
                           final List<FilterCollection> filterCollections )
    {
        super( new ApplicationRouterConfig().withPrefix( PREFIX )
                                            .withHandlers( handlers )
                                            .withRouteCollections( routeCollections )
                                            .withFilterCollections( filterCollections ) );
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
        bindHandlers( handlerInstances );
        bindRouteCollections( routeCollectionInstances );
        bindFilterCollections( filterCollectionInstances );
        logger.info( "\n\n...done.\n\n" );
    }

}
