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
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( prefix = "/depgraph/meta" )
@ApplicationScoped
public class MetadataResource
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

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
