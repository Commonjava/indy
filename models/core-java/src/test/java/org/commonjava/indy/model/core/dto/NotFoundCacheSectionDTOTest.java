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
package org.commonjava.indy.model.core.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 4/25/16.
 */
public class NotFoundCacheSectionDTOTest
{
    @Test
    public void jsonRoundTrip()
            throws IOException
    {
        String firstPath = "/path/to/first/file.pom";
        String secondPath = "/path/to/another/path.pom";
        NotFoundCacheSectionDTO in = new NotFoundCacheSectionDTO( new StoreKey( StoreType.remote, "test" ),
                                                                  Arrays.asList( firstPath, secondPath ) );

        IndyObjectMapper mapper = new IndyObjectMapper( true );

        String json = mapper.writeValueAsString( in );

        NotFoundCacheSectionDTO out = mapper.readValue( json, NotFoundCacheSectionDTO.class );

        assertThat( out, notNullValue() );
        assertThat( out.getKey(), equalTo( in.getKey() ) );

        Set<String> paths = out.getPaths();
        assertThat( paths, notNullValue() );
        assertThat( paths.size(), equalTo( 2 ) );

        assertThat( paths.contains( firstPath ), equalTo( true ) );
        assertThat( paths.contains( secondPath ), equalTo( true ) );
    }
}
