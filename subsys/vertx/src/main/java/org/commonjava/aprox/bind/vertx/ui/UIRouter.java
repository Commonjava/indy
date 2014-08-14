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
package org.commonjava.aprox.bind.vertx.ui;

import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.bind.vertx.AproxRouter;
import org.commonjava.vertx.vabr.ApplicationRouterConfig;
import org.commonjava.vertx.vabr.bind.filter.FilterCollection;
import org.commonjava.vertx.vabr.bind.route.RouteCollection;
import org.commonjava.vertx.vabr.helper.RequestHandler;

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
        super( new ApplicationRouterConfig().withPrefix( PREFIX ) );
    }

    public UIRouter( final Set<RequestHandler> handlers, final List<RouteCollection> routeCollections,
                     final List<FilterCollection> filterCollections )
    {
        super( new ApplicationRouterConfig().withPrefix( PREFIX )
                                            .withHandlers( handlers )
                                            .withRouteCollections( routeCollections )
                                            .withFilterCollections( filterCollections ) );
    }

    @PostConstruct
    @Override
    public void initializeComponents()
    {
        logger.info( "\n\nCONSTRUCTING WEB ROUTES FOR UI app...\n\n" );
        bindHandlers( handlers );
        bindRouteCollections( routeCollections );
        bindFilterCollections( filterCollections );
        logger.info( "\n\n...done.\n\n" );
    }

}
