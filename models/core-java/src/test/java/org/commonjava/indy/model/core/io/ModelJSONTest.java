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
package org.commonjava.indy.model.core.io;

import static org.junit.Assert.fail;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.model.core.HostedRepository;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ModelJSONTest
{

    private final ObjectMapper mapper = new IndyObjectMapper( true );

    String loadJson( final String resource )
        throws Exception
    {
        final InputStream is = Thread.currentThread()
                                     .getContextClassLoader()
                                     .getResourceAsStream( "model-io/" + resource );
        if ( is == null )
        {
            fail( "Cannot find classpath resource: model-io/" + resource );
        }

        return IOUtils.toString( is );
    }

    @Test
    public void deserializeHostedRepo()
        throws Exception
    {
        final String json = loadJson( "hosted-with-storage.json" );
        System.out.println( json );
        final HostedRepository repo = mapper.readValue( json, HostedRepository.class );
        System.out.println( repo );
    }

}
