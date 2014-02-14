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
package org.commonjava.aprox.bind.vertx.access;

import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.BindingType;
import org.commonjava.vertx.vabr.types.Method;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiOperation;

@Handles( prefix = "/hosted" )
@Api( description = "Handles GET/PUT/DELETE requests for content in a hosted repository store", value = "Handle hosted repository content" )
public class HostedContentHandler
    extends AbstractContentHandler<HostedRepository>
    implements RequestHandler
{
    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.access.DeployPointAccessResource#createContent(java.lang.String,
     * java.lang.String, javax.servlet.http.HttpServletRequest)
     */
    @Routes( { @Route( path = "/:name:path=(/.+)", method = Method.PUT, binding = BindingType.raw ) } )
    @ApiOperation( value = "Store new content at the given path in store with the given name." )
    public void createContent( final HttpServerRequest request )
    {
        doCreate( request );
    }

    @Routes( { @Route( path = "/:name:?path=(/.+)", method = Method.DELETE ) } )
    @ApiOperation( value = "Delete content at the given path in hosted repository with the given name." )
    @ApiError( code = 404, reason = "If either the hosted repository or the path within the hosted repository doesn't exist" )
    public void deleteContent( final Buffer buffer, final HttpServerRequest request )
    {
        doDelete( request );
    }

    @Routes( { @Route( path = "/:name:path=(/.*)", method = Method.GET ) } )
    @ApiOperation( value = "Retrieve content given by path in hosted repository with the given name." )
    @ApiError( code = 404, reason = "If either the hosted repository or the path within the hosted repository doesn't exist" )
    public void getContent( final Buffer buffer, final HttpServerRequest request )
    {
        doGet( request );
    }

    @Override
    protected StoreType getStoreType()
    {
        return StoreType.hosted;
    }

}
