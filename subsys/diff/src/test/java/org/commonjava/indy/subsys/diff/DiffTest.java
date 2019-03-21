/**
 * Copyright (C) 2013~2019 Red Hat, Inc.
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
package org.commonjava.indy.subsys.diff;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import org.apache.commons.io.IOUtils;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class DiffTest
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

        // Generate diff patch
        String s = objectMapper.writeValueAsString( repoNew );
        List<String> repoNewStrings = Arrays.asList( s.split( "\n" ) );
        s = objectMapper.writeValueAsString( repoOld );
        List<String> repoOldStrings = Arrays.asList( s.split( "\n" ) );
        Patch<String> patch = DiffUtils.diff( repoOldStrings, repoNewStrings );
        patch.getDeltas().forEach( System.out::println );

        // Convert diff into patch text
        List<String> patchDiff =
                UnifiedDiffUtils.generateUnifiedDiff( "central.json", "central.json", repoOldStrings, patch, 3 );
        StringBuilder builder = new StringBuilder();
        patchDiff.forEach( ps -> builder.append( ps ).append( "\n" ) );
        String patchString = builder.toString();
        System.out.println( patchString );

        // Convert patch text back to diff patch
        List<String> patchStrings = Arrays.asList( patchString.split( "\n" ) );
        Patch<String> patchGen = UnifiedDiffUtils.parseUnifiedDiff(patchStrings);
        List<String> result = DiffUtils.patch(repoOldStrings, patchGen);
        StringBuilder newBuilder = new StringBuilder(  );
        result.forEach( newBuilder::append );
        RemoteRepository newRepoAgain = objectMapper.readValue( newBuilder.toString(), RemoteRepository.class );
        Assert.assertTrue( newRepoAgain.isPassthrough());
        Assert.assertFalse(newRepoAgain.isAllowReleases());

    }
}
