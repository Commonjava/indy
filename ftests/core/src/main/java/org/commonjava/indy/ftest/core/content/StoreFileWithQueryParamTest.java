/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.ftest.core.content;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.test.http.expect.ExpectationServer;
import org.commonjava.test.http.util.UrlUtils;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_PKG_KEY;
import static org.commonjava.maven.galley.util.LocationUtils.ATTR_PATH_ENCODE;
import static org.commonjava.maven.galley.util.LocationUtils.PATH_ENCODE_BASE64;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class StoreFileWithQueryParamTest
        extends AbstractContentManagementTest
{
    private final String repo1 = "repo1";
    private final String path = "org/foo/bar";
    private final String type = GENERIC_PKG_KEY;

    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Test
    public void run()
        throws Exception
    {
        // CASE 1: store and retrieve from remote repo by the encoded path.

        // Prepare expectation for the path with query param '?version=2.0'
        Map<String, String> params = new HashMap<>();
        params.put("version", "2.0");
        String url = formatUrlWithQueryParam(repo1, params, path);
        //System.out.println(">>>url: " + url);
        server.expect( url, 200, "This is 2.0" );

        // Create remote repo and config it to accept base64 encoded path
        RemoteRepository remote1 = new RemoteRepository(type , repo1, server.formatUrl( repo1 ) );
        remote1.setMetadata( ATTR_PATH_ENCODE, PATH_ENCODE_BASE64 );
        remote1 = client.stores().create( remote1, "adding remote", RemoteRepository.class );

        // Get version 2.0 file by the encoded path+query
        final String encodedPath = base64url(path + "?version=2.0");
        try(InputStream is = client.content().get( remote1.getKey(), encodedPath ))
        {
            assertThat( is, notNullValue() );
            String content = IOUtils.toString(is, StandardCharsets.UTF_8);
            //System.out.println(">>> " + content);
            assertThat( content, equalTo("This is 2.0"));
        }

        // CASE 2: store and retrieve from hosted repo by the encoded path. The hosted repo do not know raw path+query after promotion.

        // Prepare file in hosted repo with encoded path
        HostedRepository hosted1 = new HostedRepository(type, repo1);
        hosted1.setMetadata( ATTR_PATH_ENCODE, PATH_ENCODE_BASE64 );
        client.stores().create( hosted1, "adding hosted", HostedRepository.class );
        client.content().store( hosted1.getKey(), encodedPath, new ByteArrayInputStream( "This is 2.0 on hosted".getBytes() ) );

        // Get from hosted
        try(InputStream is = client.content().get(hosted1.getKey(), encodedPath ))
        {
            assertThat( is, notNullValue() );
            String content = IOUtils.toString(is, StandardCharsets.UTF_8);
            //System.out.println(">>> " + content);
            assertThat( content, equalTo("This is 2.0 on hosted"));
        }
    }

    private String formatUrlWithQueryParam(String repo, Map<String, String> params, String path)
            throws MalformedURLException
    {
        return UrlUtils.buildUrl("http://127.0.0.1:" + server.getPort(), params, server.getBaseUri(), repo, path);
    }

    private String base64url(String path)
    {
        return Base64.encodeBase64URLSafeString(path.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
