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
import org.commonjava.vertx.vabr.ApplicationRouterConfig;

public abstract class AproxRouter
    extends ApplicationRouter
{

    protected AproxRouter()
    {
        super( new ApplicationRouterConfig() );
    }

    protected AproxRouter( final ApplicationRouterConfig config )
    {
        super( config );
    }

    public abstract void initializeComponents();
}
