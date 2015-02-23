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
package org.commonjava.aprox.core.bind.jaxrs;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatRedirect;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;

import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.bind.jaxrs.AproxResources;

//@Path( "/" )
public class RootResource
    implements AproxResources
{

    @GET
    public Response rootStats( @Context final UriInfo uriInfo )
    {
        Response response;
        try
        {
            response = formatRedirect( uriInfo.getBaseUriBuilder()
                                              .path( "stats/version-info" )
                                              .build() );
        }
        catch ( UriBuilderException | URISyntaxException e )
        {
            response = formatResponse( e );
        }

        return response;
    }

}
