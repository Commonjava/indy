/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.util.ApplicationContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;

@Path( "/api/test" )
public class VersioningTestHandler
                implements IndyResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ObjectMapper objectMapper;

    /**
     * Assuming we want to upgrade this to a new version, we will deprecate old impl by moving old code to somewhere
     * like VersioningTestHandlerDeprecated and keep working on the new impl here.
     */
    @Path( "/info" )
    @GET
    @Produces( ApplicationContent.application_json )
    public Response getTestInfo()
    {
        logger.debug( "Accessing getTestInfo..." );
        return formatOkResponseWithJsonEntity( new TestInfo( "001", "This is a test." ), objectMapper );
    }

    @Path( "/another" )
    @GET
    @Produces( ApplicationContent.application_json )
    public Response getAnotherInfo()
    {
        return formatOkResponseWithJsonEntity( new AnotherInfo( "This is another info." ), objectMapper );
    }

    /** this class changed in new version */
    static class TestInfo
    {
        private String id; // add a new field 'id'

        private String description;

        public TestInfo()
        {
        }

        public TestInfo( String id, String desc )
        {
            this.id = id;
            this.description = desc;
        }

        public void setId( String id )
        {
            this.id = id;
        }

        public void setDescription( String description )
        {
            this.description = description;
        }

        public String getDescription()
        {
            return description;
        }

        public String getId()
        {
            return id;
        }
    }

    /** this class has no change in new version */
    static class AnotherInfo
    {
        private String info;

        public AnotherInfo()
        {
        }

        public AnotherInfo( String info )
        {
            this.info = info;
        }

        public void setInfo( String info )
        {
            this.info = info;
        }

        public String getInfo()
        {
            return info;
        }
    }
}
