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
package org.commonjava.aprox.core.bind.vertx.admin;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.setStatus;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.vertx.util.PathParam;
import org.commonjava.aprox.bind.vertx.util.SecurityParam;
import org.commonjava.aprox.core.ctl.AdminController;
import org.commonjava.aprox.dto.StoreListingDTO;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.vertx.vabr.util.Respond;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Handles( prefix = "/admin/:type" )
public class StoreAdminHandler
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private AdminController adminController;

    @Inject
    private ObjectMapper objectMapper;

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
        final String json = buffer.getString( 0, buffer.length() );
        final String type = request.params()
                                   .get( PathParam.type.key() );

        final StoreType st = StoreType.get( type );

        ArtifactStore store;
        try
        {
            store = objectMapper.readValue( json, st.getStoreClass() );
        }
        catch ( final IOException e )
        {
            final String message = "Failed to read " + st.getStoreClass()
                                                         .getSimpleName() + " from JSON:\n" + json;
            logger.error( message, e );
            Respond.to( request )
                   .serverError( e, message, true )
                   .send();
            return;
        }

        logger.info( "\n\nGot artifact store: {}\n\n", store );

        try
        {
            if ( adminController.store( store, request.params()
                                                      .get( SecurityParam.user.key() ), true ) )
            {
                Respond.to( request )
                       .created( "admin", type, store.getName() )
                       .jsonEntity( store, objectMapper )
                       .send();
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
            Respond.to( request )
                   .serverError( e, true )
                   .send();
        }
        catch ( final JsonProcessingException e )
        {
            logger.error( e.getMessage(), e );
            Respond.to( request )
                   .serverError( e, "Failed to serialize JSON response.", true )
                   .send();
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

        logger.info( "Got JSON:\n\n{}\n\n", json );
        ArtifactStore store;
        try
        {
            store = objectMapper.readValue( json, st.getStoreClass() );
        }
        catch ( final IOException e )
        {
            final String message = "Failed to read " + st.getStoreClass()
                                                         .getSimpleName() + " from JSON:\n" + json;
            logger.error( message, e );
            Respond.to( request )
                   .serverError( e, message, true )
                   .send();
            return;
        }

        if ( !name.equals( store.getName() ) )
        {
            Respond.to( request )
                   .badRequest( String.format( "Store in URL path is: '%s' but in JSON it is: '%s'", name,
                                               store.getName() ) )
                   .send();
            return;
        }

        try
        {
            if ( adminController.store( store, request.params()
                                                      .get( SecurityParam.user.key() ), false ) )
            {
                setStatus( ApplicationStatus.OK, request );
                request.response()
                       .end();
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

            logger.info( "Returning listing containing stores:\n\t{}", new JoinString( "\n\t", stores ) );

            final StoreListingDTO dto = new StoreListingDTO( stores );
            Respond.to( request )
                   .jsonEntity( dto, objectMapper )
                   .send();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            Respond.to( request )
                   .serverError( e, true )
                   .send();
        }
        catch ( final JsonProcessingException e )
        {
            logger.error( e.getMessage(), e );
            Respond.to( request )
                   .serverError( e, true )
                   .send();
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
                Respond.to( request )
                       .jsonEntity( store, objectMapper )
                       .send();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            Respond.to( request )
                   .serverError( e, true )
                   .send();
        }
        catch ( final JsonProcessingException e )
        {
            logger.error( e.getMessage(), e );
            Respond.to( request )
                   .serverError( e, true )
                   .send();
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
            String summary = buffer.getString( 0, buffer.length() );
            if ( isEmpty( summary ) )
            {
                summary = request.headers()
                                 .get( "CHANGELOG" );
            }

            if ( isEmpty( summary ) )
            {
                summary = "Changelog not provided";
            }

            adminController.delete( key, request.params()
                                                .get( SecurityParam.user.key() ), summary );

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
