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

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.setStatus;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_artifactId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_groupId;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_key;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParam.p_version;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.rest.MetadataController;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.util.logging.Logger;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( prefix = "/depgraph/meta" )
@ApplicationScoped
public class MetadataResource
    implements RequestHandler
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private MetadataController controller;

    @Route( path = "/batch", method = Method.POST )
    public void batchUpdate( final Buffer body, final HttpServerRequest request )
    {
        try
        {
            // FIXME: Figure out character encoding parse.
            controller.batchUpdate( body.getString( 0, body.length() ) );
            setStatus( ApplicationStatus.OK, request );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

    @Route( "/for/:groupId/:artifactId/:version" )
    public void getMetadata( final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final String gid = params.get( p_groupId.key() );
        final String aid = params.get( p_artifactId.key() );
        final String ver = params.get( p_version.key() );

        String json = null;
        try
        {
            json = controller.getMetadata( gid, aid, ver );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }

        if ( json == null )
        {
            setStatus( ApplicationStatus.NOT_FOUND, request );
        }
        else
        {
            formatOkResponseWithJsonEntity( request, json );
        }
    }

    @Route( "/forkey/:groupId/:artifactId/:version/:key" )
    public void getMetadataValue( final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final String gid = params.get( p_groupId.key() );
        final String aid = params.get( p_artifactId.key() );
        final String ver = params.get( p_version.key() );
        final String k = params.get( p_key.key() );

        String json = null;
        try
        {
            json = controller.getMetadataValue( gid, aid, ver, k );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }

        if ( json == null )
        {
            setStatus( ApplicationStatus.NOT_FOUND, request );
        }
        else
        {
            formatOkResponseWithJsonEntity( request, json );
        }
    }

    @Route( path = "/:groupId/:artifactId/:version", method = Method.POST )
    public void updateMetadata( final Buffer body, final HttpServerRequest request )
    {
        final MultiMap params = request.params();
        final String gid = params.get( p_groupId.key() );
        final String aid = params.get( p_artifactId.key() );
        final String ver = params.get( p_version.key() );

        try
        {
            // FIXME: Figure out character encoding parse.
            controller.updateMetadata( gid, aid, ver, body.getString( 0, body.length() ) );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }

        setStatus( ApplicationStatus.OK, request );
    }

    @Route( path = "/collate", method = Method.POST )
    public void getCollation( final Buffer body, final HttpServerRequest request )
    {
        String json = null;
        try
        {
            // FIXME: Figure out character encoding parse.
            json = controller.getCollation( body.getString( 0, body.length() ) );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }

        if ( json == null )
        {
            setStatus( ApplicationStatus.NOT_FOUND, request );
        }
        else
        {
            formatOkResponseWithJsonEntity( request, json );
        }
    }
}
