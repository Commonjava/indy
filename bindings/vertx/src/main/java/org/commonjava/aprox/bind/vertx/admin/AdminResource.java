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
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.setStatus;

import java.util.List;

import javax.inject.Inject;

import org.commonjava.aprox.bind.vertx.util.PathParam;
import org.commonjava.aprox.core.model.io.AProxModelSerializer;
import org.commonjava.aprox.core.rest.AdminController;
import org.commonjava.aprox.core.util.UriFormatter;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationContent;
import org.commonjava.aprox.rest.util.ApplicationHeader;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.web.json.model.Listing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( prefix = "/admin/:type" )
public class AdminResource
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private AdminController adminController;

    @Inject
    private AProxModelSerializer modelSerializer;

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
    @Routes( { @Route( method = Method.POST, contentType = ApplicationContent.application_json ) } )
    public void create( final Buffer buffer, final HttpServerRequest request )
    {
        String json = buffer.getString( 0, buffer.length() );
        final String type = request.params()
                                   .get( PathParam.type.key() );

        final StoreType st = StoreType.get( type );

        final ArtifactStore store = modelSerializer.getJsonSerializer()
                                                   .fromString( json, st.getStoreClass() );

        logger.info( "\n\nGot artifact store: {}\n\n", store );

        try
        {
            if ( adminController.store( store, true ) )
            {
                formatCreatedResponse( request, uriFormatter, "admin", type, store.getName() );
                json = modelSerializer.toString( store );
                request.response()
                       .putHeader( ApplicationHeader.content_length.key(), Integer.toString( json.length() ) )
                       .write( json )
                       .end();
            }
            else
            {
                setStatus( ApplicationStatus.CONFLICT, request );
                request.response()
                       .setChunked( true )
                       .write( "{\"error\": \"Store already exists.\"}" )
                       .end();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.DeployPointAdminResource#store(java.lang.String)
     */
    @Routes( { @Route( path = "/:name", method = Method.PUT, contentType = ApplicationContent.application_json ) } )
    public void store( final Buffer buffer, final HttpServerRequest request )
    {
        final String json = buffer.getString( 0, buffer.length() );
        final String type = request.params()
                                   .get( PathParam.type.key() );
        final String name = request.params()
                                   .get( PathParam.name.key() );

        final StoreType st = StoreType.get( type );

        final ArtifactStore store = modelSerializer.getJsonSerializer()
                                                   .fromString( json, st.getStoreClass() );

        if ( !name.equals( store.getName() ) )
        {
            formatBadRequestResponse( request, String.format( "Store in URL path is: '%s' but in JSON it is: '%s'", name, store.getName() ) );
            return;
        }

        try
        {
            if ( adminController.store( store, false ) )
            {
                setStatus( ApplicationStatus.OK, request );
            }
            else
            {
                setStatus( ApplicationStatus.NOT_MODIFIED, request );
                request.response()
                       .setChunked( true )
                       .write( "{\"error\": \"Store already exists.\"}" )
                       .end();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.DeployPointAdminResource#getAll()
     */
    @Routes( { @Route( method = Method.GET, contentType = ApplicationContent.application_json ) } )
    public void getAll( final Buffer buffer, final HttpServerRequest request )
    {
        final String type = request.params()
                                   .get( PathParam.type.key() );
        final StoreType st = StoreType.get( type );

        try
        {
            @SuppressWarnings( "unchecked" )
            final List<ArtifactStore> stores = (List<ArtifactStore>) adminController.getAllOfType( st );

            logger.info( "Returning listing containing stores:\n\t{}", join( stores, "\n\t" ) );

            final Listing<ArtifactStore> listing = new Listing<ArtifactStore>( stores );

            final String json = modelSerializer.storeListingToString( listing );
            logger.info( "JSON:\n\n{}", json );

            formatOkResponseWithJsonEntity( request, json );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.DeployPointAdminResource#get(java.lang.String)
     */
    @Routes( { @Route( path = "/:name", method = Method.GET, contentType = ApplicationContent.application_json ) } )
    public void get( final Buffer buffer, final HttpServerRequest request )
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
            logger.info( "Returning repository: {}", store );

            if ( store == null )
            {
                setStatus( ApplicationStatus.NOT_FOUND, request );
                request.response()
                       .end();
            }
            else
            {
                formatOkResponseWithJsonEntity( request, modelSerializer.toString( store ) );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.DeployPointAdminResource#delete(java.lang.String)
     */
    @Routes( { @Route( path = "/:name", method = Method.DELETE ) } )
    public void delete( final Buffer buffer, final HttpServerRequest request )
    {
        final String type = request.params()
                                   .get( PathParam.type.key() );
        final String name = request.params()
                                   .get( PathParam.name.key() );

        final StoreType st = StoreType.get( type );
        final StoreKey key = new StoreKey( st, name );

        logger.info( "Deleting: {}", key );
        try
        {
            adminController.delete( key );

            setStatus( ApplicationStatus.OK, request );
            request.response()
                   .end();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

}
