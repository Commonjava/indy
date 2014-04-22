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

    @Routes( { @Route( path = "/:name:path=(/.*)", method = Method.HEAD ) } )
    @ApiOperation( value = "Retrieve content headers given by path in hosted repository with the given name." )
    @ApiError( code = 404, reason = "If either the hosted repository or the path within the hosted repository doesn't exist" )
    public void headContent( final Buffer buffer, final HttpServerRequest request )
    {
        doHead( request );
    }

    @Override
    protected StoreType getStoreType()
    {
        return StoreType.hosted;
    }

}
