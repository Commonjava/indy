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
package org.commonjava.aprox.bind.vertx.admin;

import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatBadRequestResponse;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatCreatedResponse;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.commonjava.aprox.bind.vertx.util.PathParam;
import org.commonjava.aprox.bind.vertx.util.RequestSerialHelper;
import org.commonjava.aprox.core.model.io.AProxModelSerializer;
import org.commonjava.aprox.core.rest.AdminController;
import org.commonjava.aprox.core.util.UriFormatter;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationContent;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.commonjava.util.logging.Logger;
import org.commonjava.vertx.vabr.Method;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.web.json.model.Listing;
import org.vertx.java.core.http.HttpServerRequest;

//@Path( "/admin/{type}" )
@RequestScoped
public class AdminResource
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private AdminController adminController;

    @Inject
    private AProxModelSerializer modelSerializer;

    @Inject
    private RequestSerialHelper modelServletUtils;

    @Inject
    private UriFormatter uriFormatter;

    //    @Context
    //    private UriInfo uriInfo;
    //
    //    @Context
    //    private HttpServletRequest request;

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.DeployPointAdminResource#create()
     */
    @Routes( { @Route( path = "/admin/:type", method = Method.POST, contentType = ApplicationContent.application_json ) } )
    public void create( final HttpServerRequest request )
    {
        final String type = request.params()
                                   .get( PathParam.type.key() );
        final StoreType st = StoreType.get( type );
        final ArtifactStore store = modelServletUtils.storeFromRequestBody( st, request );

        logger.info( "\n\nGot artifact store: %s\n\n", store );

        try
        {
            if ( adminController.store( store, true ) )
            {
                formatCreatedResponse( request, uriFormatter, "admin", type, store.getName() );
                request.response()
                       .write( modelSerializer.toString( store ) );
            }
            else
            {
                request.response()
                       .setStatusCode( ApplicationStatus.CONFLICT.code() )
                       .setStatusMessage( ApplicationStatus.CONFLICT.message() );
                request.response()
                       .write( "{\"error\": \"Store already exists.\"}" );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to create artifact store: %s. Reason: %s", e, e.getMessage() );
            formatResponse( e, request.response() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.DeployPointAdminResource#store(java.lang.String)
     */
    @Routes( { @Route( path = "/admin/:type/:name", method = Method.PUT, contentType = ApplicationContent.application_json ) } )
    public void store( final HttpServerRequest request )
    {
        final String type = request.params()
                                   .get( PathParam.type.key() );
        final String name = request.params()
                                   .get( PathParam.name.key() );

        final StoreType st = StoreType.get( type );
        final ArtifactStore store = modelServletUtils.storeFromRequestBody( st, request );

        if ( !name.equals( store.getName() ) )
        {
            formatBadRequestResponse( request, String.format( "Store in URL path is: '%s' but in JSON it is: '%s'", name, store.getName() ) );
        }

        try
        {
            if ( adminController.store( store, false ) )
            {
                formatCreatedResponse( request, uriFormatter, "admin", type, store.getName() );
            }
            else
            {
                request.response()
                       .setStatusCode( ApplicationStatus.NOT_MODIFIED.code() )
                       .setStatusMessage( ApplicationStatus.NOT_MODIFIED.message() );
                request.response()
                       .write( "{\"error\": \"Store already exists.\"}" );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to save proxy: %s. Reason: %s", e, e.getMessage() );
            formatResponse( e, request.response() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.DeployPointAdminResource#getAll()
     */
    @Routes( { @Route( path = "/admin/:type", method = Method.GET, contentType = ApplicationContent.application_json ) } )
    public void getAll( final HttpServerRequest request )
    {
        final String type = request.params()
                                   .get( PathParam.type.key() );
        final StoreType st = StoreType.get( type );

        try
        {
            @SuppressWarnings( "unchecked" )
            final List<ArtifactStore> stores = (List<ArtifactStore>) adminController.getAllOfType( st );
            logger.info( "Returning listing containing stores:\n\t%s", join( stores, "\n\t" ) );

            final Listing<ArtifactStore> listing = new Listing<ArtifactStore>( stores );

            final String json = modelSerializer.storeListingToString( listing );
            logger.info( "JSON:\n\n%s", json );

            formatOkResponseWithJsonEntity( request, json );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request.response() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.DeployPointAdminResource#get(java.lang.String)
     */
    @Routes( { @Route( path = "/admin/:type/:name", method = Method.GET, contentType = ApplicationContent.application_json ) } )
    public void get( final HttpServerRequest request )
    {
        final String type = request.params()
                                   .get( PathParam.type.key() );
        final String name = request.params()
                                   .get( PathParam.name.key() );

        final StoreType st = StoreType.get( type );
        final StoreKey key = new StoreKey( st, name );
        try
        {
            final ArtifactStore store = adminController.get( key );
            logger.info( "Returning repository: %s", store );

            if ( store == null )
            {
                request.response()
                       .setStatusCode( ApplicationStatus.NOT_FOUND.code() )
                       .setStatusMessage( ApplicationStatus.NOT_FOUND.message() );
            }
            else
            {
                formatOkResponseWithJsonEntity( request, modelSerializer.toString( store ) );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request.response() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.DeployPointAdminResource#delete(java.lang.String)
     */
    @Routes( { @Route( path = "/admin/:type/:name", method = Method.DELETE ) } )
    public void delete( final HttpServerRequest request )
    {
        final String type = request.params()
                                   .get( PathParam.type.key() );
        final String name = request.params()
                                   .get( PathParam.name.key() );

        final StoreType st = StoreType.get( type );
        final StoreKey key = new StoreKey( st, name );

        try
        {
            adminController.delete( key );
            request.response()
                   .setStatusCode( ApplicationStatus.OK.code() )
                   .setStatusMessage( ApplicationStatus.OK.message() );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request.response() );
        }
    }

}
