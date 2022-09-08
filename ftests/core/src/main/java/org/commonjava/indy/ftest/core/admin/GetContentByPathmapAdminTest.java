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
package org.commonjava.indy.ftest.core.admin;

import org.commonjava.indy.client.core.IndyClientHttp;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.helper.HttpResources;
import org.commonjava.indy.client.core.module.IndyRawHttpModule;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.ftest.core.category.ClusterTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@Category( ClusterTest.class )
public class GetContentByPathmapAdminTest
                extends AbstractIndyFunctionalTest
{
    final String STORE = "test";

    final String path = "org/foo/foo-project/1/foo-1.txt";

    final String adminPath = "/admin/pathmapped/content/maven/hosted/test/" + path;

    final String adminNoSuchPath = "/admin/pathmapped/content/maven/hosted/test/no/such/path";

    @Test
    public void run() throws Exception
    {
        HostedRepository hosted = client.stores()
                                        .create( new HostedRepository( MAVEN_PKG_KEY, STORE ), "test",
                                                 HostedRepository.class );

        StoreKey storeKey = hosted.getKey();

        byte[] data = ( "This is a test" ).getBytes();

        client.content().store( storeKey, path, new ByteArrayInputStream( data ) );

        IndyClientHttp http = client.module( IndyRawHttpModule.class ).getHttp();
        HttpResources ret = http.getRaw( adminPath );
        String content = HttpResources.entityToString( ret.getResponse() );
        logger.debug( "Get file content: " + content );
        assertThat( "content wrong", content, equalTo( new String( data ) ) );

        ret = http.getRaw( adminNoSuchPath );
        assertThat( "status wrong", ret.getResponse().getStatusLine().toString(), containsString( "404 Not Found" ) );
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        List<IndyClientModule> mods = new ArrayList<>();
        Collection<IndyClientModule> fromParent = super.getAdditionalClientModules();

        if ( fromParent != null )
        {
            mods.addAll( fromParent );
        }

        mods.add( new IndyRawHttpModule() );

        return mods;
    }
}
