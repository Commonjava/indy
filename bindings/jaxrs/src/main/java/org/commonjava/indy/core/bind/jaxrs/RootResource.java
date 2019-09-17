/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.core.bind.jaxrs;

import java.net.URISyntaxException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;

import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.util.REST;
import org.commonjava.indy.bind.jaxrs.util.ResponseHelper;

//@Path( "/" )
@REST
@ApplicationScoped
public class RootResource
    implements IndyResources
{

    @Inject
    private ResponseHelper responseHelper;

    @GET
    public Response rootStats( @Context final UriInfo uriInfo )
    {
        Response response;
        try
        {
            response = responseHelper.formatRedirect( uriInfo.getBaseUriBuilder()
                                              .path( "stats/version-info" )
                                              .build() );
        }
        catch ( UriBuilderException e )
        {
            response = responseHelper.formatResponse( e );
        }

        return response;
    }

}
