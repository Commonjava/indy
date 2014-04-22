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
package org.commonjava.aprox.bind.vertx.boot;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.aprox.bind.vertx.AproxRouter;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.MultiApplicationRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MasterRouter
    extends MultiApplicationRouter
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final String PREFIX = "";

    @Inject
    private Instance<AproxRouter> routers;

    @Inject
    @ExecutorConfig( daemon = true, named = "vertx-raw-dispatch" )
    private ExecutorService executorService;

    protected MasterRouter()
    {
    }

    public MasterRouter( final List<ApplicationRouter> routers, final ExecutorService executor )
    {
        super( routers, executor );
    }

    @PostConstruct
    public void initializeComponents()
    {
        logger.info( "\n\nCONSTRUCTING WEB ROUTES FOR ALL APPS IN APROX...\n\n" );

        setHandlerExecutor( executorService );

        final Set<AproxRouter> r = new HashSet<>();
        for ( final AproxRouter router : routers )
        {
            //            router.initializeComponents();
            logger.info( router.getClass()
                               .getName() );
            r.add( router );
        }

        bindRouters( r );

        logger.info( "...done" );
    }

}
