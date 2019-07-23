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

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.client.core.module.IndyMaintenanceClientModule;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

public class HostedContentIndexRescanTest
        extends AbstractContentManagementTest
{
    @Test
    public void rescanHostedAndReindexContent()
            throws Exception
    {
        final String hostedName = "hosted";
        final String path1 = "org/for/bar/1.0/foo-bar-1.0.txt";
        final String path2 = "org/for/bar/2.0/foo-bar-2.0.txt";
        final String path3 = "org/for/bar/3.0/foo-bar-3.0.txt";
        final String content1 = "content1";
        final String content2 = "content2";
        final String content3 = "content3";

        HostedRepository hosted = new HostedRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, hostedName );
        hosted.setAuthoritativeIndex( true );

        hosted = client.stores().create( hosted, "create hosted", HostedRepository.class );

        client.content().store( hosted.getKey(), path1, new ByteArrayInputStream( content1.getBytes() ) );
        client.content().store( hosted.getKey(), path2, new ByteArrayInputStream( content2.getBytes() ) );
        client.content().store( hosted.getKey(), path3, new ByteArrayInputStream( content3.getBytes() ) );

        client.module( IndyMaintenanceClientModule.class )
              .rescan( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, StoreType.hosted, hostedName );

        try (InputStream in = client.content().get( hosted.getKey(), path1 ))
        {
            assertThat( IOUtils.toString( in ), equalTo( content1 ) );
        }

        try (InputStream in = client.content().get( hosted.getKey(), path2 ))
        {
            assertThat( IOUtils.toString( in ), equalTo( content2 ) );
        }

        try (InputStream in = client.content().get( hosted.getKey(), path3 ))
        {
            assertThat( IOUtils.toString( in ), equalTo( content3 ) );
        }

    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        super.initTestConfig( fixture );
        writeConfigFile( "conf.d/content-index.conf", "[content-index]\nsupport.authoritative.indexes=true" );
    }
}
