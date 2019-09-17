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
package org.commonjava.indy.changelog;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;

public class DiffUtilTest
{
    private static final ObjectMapper objectMapper = new IndyObjectMapper( true );

    @BeforeClass
    public static void prepare()
    {
        //TODO: not sure if this is needed for string as multi-line
        objectMapper.configure( JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true );
    }

    @Test
    public void test()
            throws Exception
    {
        RemoteRepository repoOld = null;
        RemoteRepository repoNew = null;
        try (InputStream in = this.getClass().getResourceAsStream( "/central.json" ))
        {
            String json = IOUtils.toString( in );
            repoOld = objectMapper.readValue( json, RemoteRepository.class );
            repoNew = objectMapper.readValue( json, RemoteRepository.class );
        }

        repoNew.setPassthrough( true );
        repoNew.setAllowReleases( false );

        final String newRepoString = objectMapper.writeValueAsString( repoNew );
        final String oldRepoString = objectMapper.writeValueAsString( repoOld );
        final String patchString = DiffUtil.diffPatch( "central.json", newRepoString, oldRepoString );
        Assert.assertTrue( patchString.contains( "-  \"allow_releases\" : true" ) );
        Assert.assertTrue( patchString.contains( "+  \"allow_releases\" : false" ) );
        Assert.assertTrue( patchString.contains( "-  \"is_passthrough\" : false" ) );
        Assert.assertTrue( patchString.contains( "+  \"is_passthrough\" : true" ) );

        final String patchedRepoString = DiffUtil.recoverFromPatch( oldRepoString, patchString );
        RemoteRepository newRepoAgain = objectMapper.readValue( patchedRepoString, RemoteRepository.class );
        Assert.assertTrue( newRepoAgain.isPassthrough() );
        Assert.assertFalse( newRepoAgain.isAllowReleases() );
    }
}
