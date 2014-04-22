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

import org.commonjava.aprox.model.Group;
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

@Handles( prefix = "/group" )
@Api( description = "Handles GET/PUT/DELETE requests for content in the constituency of group store", value = "Handle group content" )
public class GroupContentHandler
    extends AbstractContentHandler<Group>
    implements RequestHandler
{

    @Routes( { @Route( path = "/:name:?path=(/.+)", method = Method.DELETE ) } )
    @ApiOperation( value = "Delete content at the given path from all constituent stores within the group with the given name." )
    @ApiError( code = 404, reason = "If the deletion fails" )
    public void deleteContent( final Buffer buffer, final HttpServerRequest request )
    {
        doDelete( request );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.access.GroupAccessResource#getProxyContent(java.lang.String,
     * java.lang.String)
     */
    @Routes( { @Route( path = "/:name:path=(/.*)", method = Method.GET ) } )
    @ApiOperation( value = "Retrieve content from the FIRST constituent store that contains the given path, within the group with the given name." )
    @ApiError( code = 404, reason = "If none of the constituent stores contains the path" )
    public void getProxyContent( final Buffer buffer, final HttpServerRequest request )
    {
        doGet( request );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.access.GroupAccessResource#getProxyContent(java.lang.String,
     * java.lang.String)
     */
    @Routes( { @Route( path = "/:name:path=(/.*)", method = Method.HEAD ) } )
    @ApiOperation( value = "Retrieve content headers from the FIRST constituent store that contains the given path, within the group with the given name." )
    @ApiError( code = 404, reason = "If none of the constituent stores contains the path" )
    public void headProxyContent( final Buffer buffer, final HttpServerRequest request )
    {
        doHead( request );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.access.GroupAccessResource#createContent(java.lang.String, java.lang.String,
     * javax.servlet.http.HttpServletRequest)
     */
    @Routes( { @Route( path = "/:name/:path=(.+)", method = Method.PUT, binding = BindingType.raw ) } )
    @ApiOperation( value = "Store new content at the given path in the first deploy-point store constituent listed in the group with the given name." )
    @ApiError( code = 404, reason = "If the group doesn't contain any deploy-point stores" )
    public void createContent( final HttpServerRequest request )
    {
        doCreate( request );
    }

    @Override
    protected StoreType getStoreType()
    {
        return StoreType.group;
    }
}
