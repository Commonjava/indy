/**
 * Copyright (C) 2020 Red Hat, Inc.
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
package org.commonjava.indy.repo.proxy.ftest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.indy.content.browse.model.ContentBrowseResult;
import org.commonjava.indy.model.core.AbstractRepository;
import org.commonjava.maven.galley.util.PathUtils;
import org.commonjava.test.http.expect.ExpectationServer;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestUtils
{
    static String getExpectedRemoteContent( ExpectationServer server, AbstractRepository repo, String rootPath,
                                     ObjectMapper mapper )
            throws IOException
    {
        final String url = server.formatUrl( "" );
        ContentBrowseResult result = new ContentBrowseResult();
        result.setStoreKey( repo.getKey() );
        final String rootStorePath =
                PathUtils.normalize( url, "api/browse", repo.getKey().toString().replaceAll( ":", "/" ) );
        result.setParentUrl( PathUtils.normalize( rootStorePath, "foo", "/" ) );
        result.setParentPath( "foo/" );
        result.setPath( "foo/bar/" );
        result.setStoreBrowseUrl( rootStorePath );
        result.setStoreContentUrl(
                PathUtils.normalize( url, "api/content", repo.getKey().toString().replaceAll( ":", "/" ) ) );
        result.setSources( Collections.singletonList( rootStorePath ) );
        ContentBrowseResult.ListingURLResult listResult = new ContentBrowseResult.ListingURLResult();
        final String path = rootPath + "foo-bar.txt";
        listResult.setListingUrl( PathUtils.normalize( rootStorePath, path ) );
        listResult.setPath( path );
        Set<String> sources = new HashSet<>();
        sources.add( "indy:" + repo.getKey().toString() + path );
        listResult.setSources( sources );
        result.setListingUrls( Collections.singletonList( listResult ) );

        return mapper.writeValueAsString( result );
    }
}
