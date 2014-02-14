/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
