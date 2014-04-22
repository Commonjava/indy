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
package org.commonjava.aprox.bind.jaxrs;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

@Path( "/" )
@javax.enterprise.context.ApplicationScoped
public class RootResource
{

    @GET
    public void rootStats( @Context final HttpServletResponse response, @Context final UriInfo info )
        throws ServletException, IOException
    {
        response.sendRedirect( info.getBaseUriBuilder()
                                   .path( "/stats/version-info" )
                                   .build()
                                   .toURL()
                                   .toExternalForm() );
    }

}
