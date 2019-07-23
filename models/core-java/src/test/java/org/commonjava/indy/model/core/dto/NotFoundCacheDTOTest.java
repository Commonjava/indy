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
public class NotFoundCacheDTOTest
{
    @Test
    public void jsonRoundTrip()
            throws IOException
    {
        String firstSectionOnePath = "/path/to/first/file.pom";
        String secondSectionOnePath = "/path/to/another/path.pom";
        NotFoundCacheSectionDTO sectionOne = new NotFoundCacheSectionDTO( new StoreKey( StoreType.remote, "test" ),
                                                                  Arrays.asList( firstSectionOnePath, secondSectionOnePath ) );

        String firstSectionTwoPath = "/path/to/third/file.pom";
        String secondSectionTwoPath = "/path/to/fourth/path.pom";
        NotFoundCacheSectionDTO sectionTwo = new NotFoundCacheSectionDTO( new StoreKey( StoreType.remote, "test2" ),
                                                                          Arrays.asList( firstSectionTwoPath, secondSectionTwoPath ) );

        NotFoundCacheDTO in = new NotFoundCacheDTO();
        in.addSection( sectionOne );
        in.addSection( sectionTwo );

        IndyObjectMapper mapper = new IndyObjectMapper( true );

        String json = mapper.writeValueAsString( in );

        NotFoundCacheDTO out = mapper.readValue( json, NotFoundCacheDTO.class );

        assertThat( out, notNullValue() );

        Set<NotFoundCacheSectionDTO> sections = out.getSections();
        assertThat( sections, notNullValue() );
        assertThat( sections.size(), equalTo( 2 ) );

        assertThat( sections.contains( sectionOne ), equalTo( true ) );
        assertThat( sections.contains( sectionTwo ), equalTo( true ) );

        sections.forEach( (section)->{
            StoreKey testKey;
            Set<String> testPaths;
            if ( section.equals( sectionOne ) )
            {
                testKey = sectionOne.getKey();
                testPaths = sectionOne.getPaths();
            }
            else
            {
                testKey = sectionTwo.getKey();
                testPaths = sectionTwo.getPaths();
            }

            assertThat( section.getKey(), equalTo( testKey ) );

            Set<String> paths = section.getPaths();
            assertThat( paths, notNullValue() );
            assertThat( paths.size(), equalTo( testPaths.size() ) );

            testPaths.forEach( (path)->{
                assertThat( path + " NOT found in results for key: " + section.getKey(), paths.contains( path ),
                            equalTo( true ) );
            } );
        } );
    }
}
