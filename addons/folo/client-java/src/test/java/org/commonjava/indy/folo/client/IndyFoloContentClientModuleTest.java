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
package org.commonjava.indy.folo.client;

import org.commonjava.indy.client.core.Indy;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.helper.PathInfo;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.commonjava.indy.model.core.StoreType.remote;

/**
 * Created by jdcasey on 5/15/17.
 */
public class IndyFoloContentClientModuleTest
{
    @Rule
    public ExpectationServer server = new ExpectationServer();

    private Indy indy;

    private IndyFoloContentClientModule module;

    @Before
    public void setup()
            throws IndyClientException
    {
        module = new IndyFoloContentClientModule();
        indy = new Indy( server.formatUrl( "api" ), module ).connect();
    }

    @Test
    public void deprecatedContentStore()
            throws Exception
    {
        String tid = "foo";
        String repo = "central";
        String path = "org/foo/bar/1/bar-1.pom";
        String fullPath = String.format( "/api/folo/track/%s/maven/remote/%s/%s", tid, repo, path );

        server.expect( "PUT", server.formatUrl( fullPath ), 201, "" );

        String value =
                "<project><modelVersion>4.0.0</modelVersion><groupId>org.foo</groupId><artifactId>bar</artifactId>"
                        + "<version>1</version><packaging>pom</packaging></project>";

        PathInfo info = module.store( tid, remote, "central", path, new ByteArrayInputStream( value.getBytes() ) );

    }
}
