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

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatRedirect;

import javax.inject.Inject;

import org.commonjava.aprox.util.UriFormatter;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( key = "apiRootRedirector", prefix = "/" )
public class RootResource
    implements RequestHandler
{

    @Inject
    private UriFormatter uriFormatter;

    @Routes( { @Route( method = Method.GET ) } )
    public void rootStats( final Buffer buffer, final HttpServerRequest request )
    {
        formatRedirect( request, uriFormatter.formatAbsolutePathTo( "stats/version-info" ) );
    }

}
