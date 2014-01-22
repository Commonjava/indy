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
package org.commonjava.aprox.depgraph.vertx.calc;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.rest.util.ApplicationContent.application_json;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.commonjava.aprox.bind.vertx.util.VertXInputStream;
import org.commonjava.aprox.depgraph.rest.CalculatorController;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.util.logging.Logger;
import org.commonjava.vertx.vabr.anno.PathPrefix;
import org.commonjava.vertx.vabr.anno.Route;
import org.vertx.java.core.http.HttpServerRequest;

@PathPrefix( "/depgraph/calc" )
@RequestScoped
public class CalculatorResource
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private CalculatorController controller;

    @Route( path = "/diff", contentType = application_json )
    public void difference( final HttpServerRequest request )
    {
        try
        {
            // FIXME Figure out the character encoding!
            final String json = controller.difference( new VertXInputStream( request ), null );
            formatOkResponseWithJsonEntity( request, json );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request.response() );
        }
    }

    @Route( contentType = application_json )
    public void calculate( final HttpServerRequest request )
        throws AproxWorkflowException
    {
        try
        {
            // FIXME Figure out the character encoding!
            final String json = controller.calculate( new VertXInputStream( request ), null );
            formatOkResponseWithJsonEntity( request, json );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            formatResponse( e, request.response() );
        }
    }
}
