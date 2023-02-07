/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.promote.ftest;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.module.IndyContentClientModule;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.promote.client.IndyPromoteClientModule;
import org.commonjava.indy.promote.model.PathsPromoteRequest;
import org.commonjava.indy.promote.model.PathsPromoteResult;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * GIVEN:
 * <ul>
 *     <li>HostedRepository A with version V1 of x.tgz</li>
 *     <li>Group G contains HostedRepository A</li>
 *     <li>RemoteRepository B with version V2 of x.tgz</li>
 * </ul>
 * <br/>
 * WHEN/THEN:
 * <ul>
 *     <li>Get package.json from G, it contains V1</li>
 *     <li>RemoteRepository B with path x.tgz is promoted to A</li>
 *     <li>Get package.json from G, it contains both V1 and V2</li>
 * </ul>
 */
public class PromoteNpmAndUpdateMetadataTest
                extends AbstractIndyFunctionalTest
{
    private static final String REMOTE = "remote";

    private final String TGZ_V1 = "json-ext-0.5.6.tgz";
    private final String PATH_V1 = "/@discoveryjs/json-ext/-/" + TGZ_V1;

    private final String TGZ_V2 = "json-ext-0.5.7.tgz";
    private final String PATH_V2 = "/@discoveryjs/json-ext/-/" + TGZ_V2;

    private final String metadataPath = "/@discoveryjs/json-ext";

    protected RemoteRepository source;

    protected HostedRepository target;

    protected Group group;

    protected final IndyPromoteClientModule promotions = new IndyPromoteClientModule();

    @Rule
    public ExpectationServer server = new ExpectationServer( "repos" );

    @Before
    public void setup() throws Exception
    {
        final String RES_ROOT = this.getClass().getSimpleName();

        final String changelog = "Setup " + name.getMethodName();

        server.expect( server.formatUrl( REMOTE, PATH_V2 ), 200,
                readTestResourceAsStream( Paths.get(RES_ROOT, TGZ_V2).toString() ) );

        source = new RemoteRepository( NPM_PKG_KEY, "source", server.formatUrl( REMOTE ) );
        source = client.stores().create( source, changelog, RemoteRepository.class );

        target = new HostedRepository( NPM_PKG_KEY, "target" );
        target = client.stores().create( target, changelog, HostedRepository.class );

        group = new Group( NPM_PKG_KEY, "test", target.getKey() );
        client.stores().create( group, changelog, Group.class );

        // Store version V1 of x.tgz to HostedRepository A
        IndyContentClientModule c = client.content();
        c.store(target.getKey(), PATH_V1, readTestResourceAsStream( Paths.get(RES_ROOT, TGZ_V1).toString() ));

        System.out.println(">>>> Prepare done!");
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Collections.singletonList( promotions );
    }

    @Test
    public void run() throws Exception
    {
        IndyContentClientModule c = client.content();

        // Check the version before promotion
        try (InputStream is = c.get(group.getKey(), metadataPath ))
        {
            String json = IOUtils.toString(is);
            assertThat( json.contains(TGZ_V1), equalTo( true ) );
        }

        // Promote
        IndyPromoteClientModule promoteClient = client.module(IndyPromoteClientModule.class);
        PathsPromoteResult result = promoteClient.promoteByPath( new PathsPromoteRequest( source.getKey(), target.getKey(), PATH_V2 ) );

        // Normal post promotion checks
        Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 1 ) );
        assertThat( result.getError(), nullValue() );

        // Check the versions after promotion. Both versions should be there.
        try (InputStream is = c.get(group.getKey(), metadataPath ))
        {
            String json = IOUtils.toString(is);
            assertThat( json.contains(TGZ_V1), equalTo( true ) );
            assertThat( json.contains(TGZ_V2), equalTo( true ) );
        }
    }
}
