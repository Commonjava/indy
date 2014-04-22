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
