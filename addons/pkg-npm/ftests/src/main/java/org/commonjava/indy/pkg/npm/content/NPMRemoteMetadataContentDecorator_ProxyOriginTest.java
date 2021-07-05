/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.pkg.npm.content;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.client.core.IndyClientHttp;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.helper.HttpResources;
import org.commonjava.indy.client.core.module.IndyRawHttpModule;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * This case tests if package.json metadata can be retrieved and DECORATED.
 * when: <br />
 * <ul>
 *      <li>creates remote repo A and expect metadata file in it</li>
 *      <li>creates group G containing A</li>
 *      <li>set request header proxy-origin, retrieve the metadata file from the remote repo A and G</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the metadata content can be retrieved with all tarball urls decorated to proxy-origin urls</li>
 * </ul>
 */
public class NPMRemoteMetadataContentDecorator_ProxyOriginTest
                extends AbstractContentManagementTest
{
    protected static final String GROUP = "G";

    private final IndyRawHttpModule httpModule = new IndyRawHttpModule();

    private final String proxyOrigin = "http://indy-gateway.svc.cluster.local";

    @Test
    public void test() throws Exception
    {
        final String packageContent = IOUtils.toString(
                        Thread.currentThread().getContextClassLoader().getResourceAsStream( "package-1.5.1.json" ) );

        final String packagePath = "jquery";
        final String tarballUrl = "https://registry.npmjs.org/jquery/-/jquery-1.5.1.tgz";

        server.expect( server.formatUrl( STORE, packagePath ), 200,
                       new ByteArrayInputStream( packageContent.getBytes() ) );

        final RemoteRepository remoteRepository = new RemoteRepository( NPM_PKG_KEY, STORE, server.formatUrl( STORE ) );
        client.stores().create( remoteRepository, "adding npm remote repo", RemoteRepository.class );

        final Group group = new Group( NPM_PKG_KEY, GROUP, remoteRepository.getKey() );
        client.stores().create( group, "adding group", Group.class );

        // prepare
        IndyClientHttp http = client.module( IndyRawHttpModule.class ).getHttp();
        Map<String, String> headers = new HashMap<>();
        headers.put( "proxy-origin", proxyOrigin );

        // retrieve from remote repo
        HttpResources ret = http.getRaw( client.content().contentPath( remoteRepository.getKey(), packagePath ), headers );
        String content = HttpResources.entityToString( ret.getResponse() );
        //logger.debug( "Get file content:\n{}", content );
        String expected = packageContent.replaceAll( tarballUrl, proxyOrigin + "/api/content/npm/remote/test/jquery/-/jquery-1.5.1.tgz" );
        assertThat( content, equalTo( expected ) );

        // retrieve from group
        ret = http.getRaw( client.content().contentPath( group.getKey(), packagePath ), headers );
        content = HttpResources.entityToString( ret.getResponse() );
        Object obj = new JSONParser().parse(content);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject versions = (JSONObject) jsonObject.get( "versions" );
        JSONObject v151 = (JSONObject) versions.get( "1.5.1" );
        JSONObject dist = (JSONObject) v151.get( "dist" );
        String tarball = (String) dist.get( "tarball" );
        expected = proxyOrigin + "/api/content/npm/group/G/jquery/-/jquery-1.5.1.tgz";
        assertThat( tarball, equalTo( expected ) );
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Arrays.asList( httpModule );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
