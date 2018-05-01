/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.autoprox.ftest.content;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 10/6/15.
 */
public class AutoproxDisabledAllowExistingUsageTest
        extends AbstractAutoproxContentTest
{

    private boolean initRule()
    {
        return false;
    }

    @Test
    public void run()
            throws Exception
    {
        String path = "path/to/test.txt";

        String named = "other";
        client.stores().create( new RemoteRepository( MAVEN_PKG_KEY, named, http.formatUrl( named ) ),
                                "Adding pre-existing remote repo", RemoteRepository.class );

        String content = "This is a test";
        http.expect( http.formatUrl( named, path ), 200, content );

        InputStream stream = client.content().get( new StoreKey( MAVEN_PKG_KEY, remote, named ), path );
        assertThat( stream, notNullValue() );

        String result = IOUtils.toString( stream );
        assertThat( result, equalTo( content ) );
        stream.close();
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        super.initTestConfig( fixture );
        writeConfigFile( "conf.d/autoprox.conf", "[autoprox]\nenabled=false" );
    }
}
