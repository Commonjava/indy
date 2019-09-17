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
package org.commonjava.indy.ftest.core.content;

import org.apache.http.HttpResponse;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.helper.HttpResources;
import org.commonjava.indy.client.core.module.IndyRawHttpModule;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.RemoteRepository;
import org.junit.Test;

import javax.activation.MimetypesFileTypeMap;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Verifies that the content type header will be set via JDK's mime-typing classes if there is no one from upstream.
 * <br/>
 * Given:
 * <ul>
 *     <li>Content is stored in a remote repository</li>
 * </ul>
 * <br/>
 * When:
 * <ul>
 *     <li>Request the remote content and the response doesn't contain the header Content-Type.</li>
 * </ul>
 * <br/>
 * Then:
 * <ul>
 *     <li>The Content-Type will be set via JDK's mime-typing classes and return to user.</li>
 * </ul>
 */
public class SetContentTypeNotExistsTest
                extends AbstractContentManagementTest
{

    @Test
    public void run() throws Exception
    {
        final String content = "This is some content " + System.currentTimeMillis() + "." + System.nanoTime();
        final String path = "org/foo/foo-project/1/foo-1.jar";

        server.expect( "GET", server.formatUrl( STORE, path ), ( request, response ) -> {
            response.setStatus( 200 );
            response.setHeader( "Content-Length", Integer.toString( content.length() ) );
            PrintWriter writer = response.getWriter();

            writer.write( content );
        } );

        client.stores()
              .create( new RemoteRepository( STORE, server.formatUrl( STORE ) ), "adding remote",
                       RemoteRepository.class );

        try (HttpResources httpResources = client.module( IndyRawHttpModule.class )
                                                 .getHttp()
                                                 .getRaw( client.content().contentPath( remote, STORE, path ) ))
        {
            HttpResponse response = httpResources.getResponse();

            String contentType = response.getFirstHeader( "Content-Type" ).getValue();
            assertThat( contentType, equalTo( new MimetypesFileTypeMap().getContentType( path ) ) );
        }
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
