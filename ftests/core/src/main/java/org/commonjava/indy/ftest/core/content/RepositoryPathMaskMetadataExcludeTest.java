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
import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class RepositoryPathMaskMetadataExcludeTest
        extends AbstractContentManagementTest
{

    @Rule
    public ExpectationServer server = new ExpectationServer();

    final static String meta1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<metadata>\n" +
            "  <groupId>org.bar</groupId>\n" +
            "  <artifactId>bar-project</artifactId>\n" +
            "  <versioning>\n" +
            "    <versions>\n" +
            "      <version>1.0</version>\n" +
            "    </versions>\n" +
            "    <lastUpdated>20161027054508</lastUpdated>\n" +
            "  </versioning>\n" +
            "</metadata>";

    final static String meta2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<metadata>\n" +
            "  <groupId>org.bar</groupId>\n" +
            "  <artifactId>bar-project</artifactId>\n" +
            "  <versioning>\n" +
            "    <versions>\n" +
            "      <version>2.0</version>\n" +
            "    </versions>\n" +
            "    <lastUpdated>20161028054508</lastUpdated>\n" +
            "  </versioning>\n" +
            "</metadata>";

    final static String aggregatedMeta = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<metadata>\n" +
            "  <groupId>org.bar</groupId>\n" +
            "  <artifactId>bar-project</artifactId>\n" +
            "  <versioning>\n" +
            "    <latest>2.0</latest>\n" +
            "    <release>2.0</release>\n" +
            "    <versions>\n" +
            "      <version>2.0</version>\n" +
            "    </versions>\n" +
            "    <lastUpdated>20161028054508</lastUpdated>\n" +
            "  </versioning>\n" +
            "</metadata>";

    /**
     *  Case where user requests maven-metadata.xml from a group. The group contains two repositories.
     *  One repository have path masks to exclude the metadata paths, but the other repository shouldn't mask any paths.
     *
     *  The group maven-metadata.xml should exclude versions from the member that un-mask the path
     */
    @Test
    public void run()
            throws Exception
    {
        final String path_metadata = "org/bar/bar-project/maven-metadata.xml";

        final String remote1 = "remote1";
        final String hosted1 = "hosted1";

        server.expect( server.formatUrl( remote1, path_metadata ), 200, meta1 );

        RemoteRepository remoteRepo1 = new RemoteRepository( remote1, server.formatUrl( remote1 ) );
        Set<String> pathMaskPatterns = new HashSet<>();
        pathMaskPatterns.add("r|org/foo.*|"); // regex patterns, un-mask org/bar
        remoteRepo1.setPathMaskPatterns(pathMaskPatterns);
        remoteRepo1 = client.stores().create( remoteRepo1, "adding remote 1", RemoteRepository.class );

        HostedRepository hostedRepo1 = new HostedRepository( hosted1 );
        hostedRepo1 = client.stores().create( hostedRepo1, "adding hosted 1", HostedRepository.class );
        client.content().store( hosted, hosted1, path_metadata, new ByteArrayInputStream( meta2.getBytes() ) );

        Group g = new Group( "group1", remoteRepo1.getKey(), hostedRepo1.getKey() );
        g = client.stores().create( g, "adding group1", Group.class );
        System.out.printf( "\n\nGroup constituents are:\n  %s\n\n", StringUtils.join( g.getConstituents(), "\n  " ) );

        InputStream stream = null;
        String str = null;

        // get metadata from hosted1
        stream = client.content().get( hosted, hostedRepo1.getName(), path_metadata );
        assertThat( stream, notNullValue() );

        str = IOUtils.toString( stream );
        System.out.println("hosted1.metadata >>>> " + str);
        stream.close();

        // get metadata from remote1
        stream = client.content().get( remote, remoteRepo1.getName(), path_metadata );
        assertThat( stream, nullValue() );

        // get metadata from group1
        stream = client.content().get( group, g.getName(), path_metadata );
        assertThat( stream, notNullValue() );

        // return aggregated versions from all repositories with valid mask
        str = IOUtils.toString( stream );
        System.out.println("group1.metadata >>>> " + str);

        assertThat( str.trim(), equalTo( aggregatedMeta ));
        stream.close();
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
