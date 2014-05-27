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
package org.commonjava.aprox.depgraph.vertx.calc;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.depgraph.vertx.util.DepgraphParamUtils.getWorkspaceId;
import static org.commonjava.aprox.util.ApplicationContent.application_json;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.rest.CalculatorController;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( prefix = "/depgraph/calc" )
@ApplicationScoped
public class CalculatorResource
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private CalculatorController controller;

    @Route( path = "/diff", contentType = application_json )
    public void difference( final Buffer body, final HttpServerRequest request )
    {
        try
        {
            // FIXME Figure out the character encoding!
            final String json = controller.difference( body.getString( 0, body.length() ), getWorkspaceId( request ) );
            formatOkResponseWithJsonEntity( request, json );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }

    @Route( contentType = application_json )
    public void calculate( final Buffer body, final HttpServerRequest request )
    {
        try
        {
            // FIXME Figure out the character encoding!
            final String json = controller.calculate( body.getString( 0, body.length() ), getWorkspaceId( request ) );
            formatOkResponseWithJsonEntity( request, json );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request );
        }
    }
}
