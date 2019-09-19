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

import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.helper.PathInfo;
import org.commonjava.indy.client.core.module.IndyRawHttpModule;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class DownloadContentHasLengthHeaderHostedTest
        extends AbstractContentManagementTest
{

    @Test
    public void run()
            throws Exception
    {
        byte[] data = ( "This is a test: " + System.nanoTime() ).getBytes();
        String path = "org/foo/foo-project/1/foo-1.txt";

        InputStream stream = new ByteArrayInputStream( data );
        client.content().store( hosted, STORE, path, stream );

        String p = client.content().contentPath(hosted, STORE, path);
        Map<String, String> headers = client.module(IndyRawHttpModule.class).getHttp().head(p);
        logger.debug("Get headers: " + headers);

        PathInfo result = client.content().getInfo( hosted, STORE, path );
        assertThat( "no result", result, notNullValue() );
        assertThat( "doesn't exist", result.exists(), equalTo( true ) );
        assertThat( "content length wrong", new Long(result.getContentLength()).intValue(), equalTo( data.length ) );
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
