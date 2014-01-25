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
package org.commonjava.aprox.depgraph.vertx;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatCreatedResponse;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.setStatus;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_artifactId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_groupId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_newVersion;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_profile;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_source;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_wsid;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.q_for;
import static org.commonjava.aprox.rest.util.ApplicationContent.application_json;
import static org.commonjava.vertx.vabr.BuiltInParam._classBase;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.bind.vertx.util.VertXInputStream;
import org.commonjava.aprox.bind.vertx.util.VertXUriFormatter;
import org.commonjava.aprox.core.dto.CreationDTO;
import org.commonjava.aprox.depgraph.rest.WorkspaceController;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.commonjava.util.logging.Logger;
import org.commonjava.vertx.vabr.Method;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( prefix = "/depgraph/ws" )
@ApplicationScoped
public class WorkspaceResource
    implements RequestHandler
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private WorkspaceController controller;

    @Route( path = "/:wsid", method = Method.DELETE )
    public void delete( final HttpServerRequest request )
    {
        final String id = request.params()
                                 .get( p_wsid.name() );
        try
        {
            controller.delete( id );
            setStatus( ApplicationStatus.OK, request );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request.response() );
        }
    }

    @Route( path = "/:wsid", method = Method.PUT, contentType = application_json )
    public void createNamed( final HttpServerRequest request )
    {
        final String id = request.params()
                                 .get( p_wsid.key() );
        try
        {
            final CreationDTO dto = controller.createNamed( id, request.params()
                                                                       .get( _classBase.key() ), new VertXUriFormatter() );
            if ( dto != null )
            {
                formatCreatedResponse( request, dto );
            }
            else
            {
                setStatus( ApplicationStatus.NOT_MODIFIED, request );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request.response() );
        }
    }

    @Route( path = "/new", method = Method.POST, contentType = application_json )
    public void create( final HttpServerRequest request )
    {
        try
        {
            final CreationDTO dto = controller.create( request.params()
                                                              .get( _classBase.key() ), new VertXUriFormatter() );
            if ( dto != null )
            {
                formatCreatedResponse( request, dto );
            }
            else
            {
                setStatus( ApplicationStatus.NOT_MODIFIED, request );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request.response() );
        }
    }

    @Route( path = "/new/from", method = Method.POST, contentType = application_json )
    public void createFrom( final HttpServerRequest request )
    {
        try
        {
            // FIXME Figure out the character encoding!
            final CreationDTO dto =
                controller.createFrom( request.params()
                                              .get( _classBase.key() ), new VertXUriFormatter(), new VertXInputStream( request ), null );
            if ( dto != null )
            {
                formatCreatedResponse( request, dto );
            }
            else
            {
                setStatus( ApplicationStatus.NOT_MODIFIED, request );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request.response() );
        }
    }

    @Route( "/:wsid/select/:groupId/:artifactId/:newVersion" )
    public void select( final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final String id = params.get( p_wsid.key() );
        final String gid = params.get( p_groupId.key() );
        final String aid = params.get( p_artifactId.key() );
        final String newVer = params.get( p_newVersion.key() );
        final String oldVer = params.get( q_for.key() );

        try
        {
            final boolean modified = controller.select( id, gid, aid, newVer, oldVer, new VertXUriFormatter() );
            setStatus( modified ? ApplicationStatus.OK : ApplicationStatus.NOT_MODIFIED, request );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request.response() );
        }
    }

    @Route( path = "/:wsid", contentType = application_json )
    public void get( final HttpServerRequest request )
    {
        final String id = request.params()
                                 .get( p_wsid.key() );
        try
        {
            final String json = controller.get( id );
            if ( json == null )
            {
                setStatus( ApplicationStatus.NOT_FOUND, request );
            }
            else
            {
                formatOkResponseWithJsonEntity( request, json );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request.response() );
        }
    }

    @Route( contentType = application_json )
    public void list( final HttpServerRequest request )
    {
        final String json = controller.list();
        if ( json == null )
        {
            setStatus( ApplicationStatus.NOT_FOUND, request );
        }
        else
        {
            formatOkResponseWithJsonEntity( request, json );
        }
    }

    @Route( path = "/:wsid/source/:source", method = Method.PUT, contentType = application_json )
    public void addSource( final HttpServerRequest request )
    {
        final String id = request.params()
                                 .get( p_wsid.key() );
        final String src = request.params()
                                  .get( p_source.key() );

        try
        {
            final boolean modified = controller.addSource( id, src, new VertXUriFormatter() );
            setStatus( modified ? ApplicationStatus.OK : ApplicationStatus.NOT_MODIFIED, request );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request.response() );
        }
    }

    @Route( path = "/:wsid/profile/:profile", method = Method.PUT, contentType = application_json )
    public void addPomLocation( final HttpServerRequest request )
    {
        final String id = request.params()
                                 .get( p_wsid.key() );
        final String prfl = request.params()
                                   .get( p_profile.key() );

        try
        {
            final boolean modified = controller.addPomLocation( id, prfl, new VertXUriFormatter() );
            setStatus( modified ? ApplicationStatus.OK : ApplicationStatus.NOT_MODIFIED, request );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request.response() );
        }
    }
}
