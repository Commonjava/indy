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

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.aprox.bind.vertx.AproxRouter;
import org.commonjava.aprox.dotmaven.inject.DotMavenApp;
import org.commonjava.vertx.vabr.filter.FilterCollection;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.route.RouteCollection;
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
