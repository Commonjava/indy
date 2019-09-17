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
package org.commonjava.indy.ftest.core.jaxrs;

import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.util.ResponseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path( "/api/test" )
public class VersioningTestHandlerDeprecated
                implements IndyResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ResponseHelper responseHelper;

    @Path( "/info" )
    @GET
    @Produces( "application/indy-v0.9+json" )
    public Response getTestInfo()
    {
        logger.debug( "Accessing deprecated getTestInfo..." );
        return responseHelper.formatOkResponseWithJsonEntity( new TestInfo( "This is a test." ) );
    }

    static class TestInfo
    {
        private String description;

        public TestInfo()
        {
        }

        public TestInfo( String desc )
        {
            this.description = desc;
        }

        public void setDescription( String description )
        {
            this.description = description;
        }

        public String getDescription()
        {
            return description;
        }
    }
}
