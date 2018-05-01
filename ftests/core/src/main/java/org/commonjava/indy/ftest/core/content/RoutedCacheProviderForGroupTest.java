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
package org.commonjava.indy.ftest.core.content;

import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class RoutedCacheProviderForGroupTest
        extends AbstractContentManagementTest
{
    @Test
    public void addGroupAndNFSSetup()
            throws Exception
    {
        final String content = "This is a test: " + System.nanoTime();
        final InputStream stream = new ByteArrayInputStream( content.getBytes() );

        final String path = "/path/to/foo.class";

        final String hostedName1 = "test1";
        final HostedRepository hostedRepo1 = this.client.stores()
                                                       .create( new HostedRepository( hostedName1 ), "create group",
                                                                HostedRepository.class );

        final File nfsStorage1 = Paths.get( fixture.getBootOptions().getIndyHome(), NFS_BASE, MAVEN_PKG_KEY,
                                            hosted.singularEndpointName() + "-" + hostedName1, path ).toFile();


        final String gname = newName();
        Group g = this.client.stores().create( new Group( gname ), "create group", Group.class );
        g.addConstituent( hostedRepo1 );

        this.client.stores().update( g, "update group" );

        assertThat( client.content().exists( hosted, hostedName1, path ), equalTo( false ) );
        assertThat( nfsStorage1.exists(), equalTo( false ) );

        client.content().store( group, gname, path, stream );

        assertThat( client.content().exists( hosted, hostedName1, path ), equalTo( true ) );
        //TODO: seems that the nfs store is created here even if the store operation is on a group repo with a hosted repo in
        //      doubt that the store operation is recursively called on the sub constituent of this group, so the hosted repo
        //      in this group then do the store through the FastLocal, so the nfs one is created. Not sure if this is a right
        //      case.
        assertThat( nfsStorage1.exists(), equalTo( true ) );
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        writeConfigFile( "main.conf", readTestResource( "default-test-main.conf" ) );
    }

}
