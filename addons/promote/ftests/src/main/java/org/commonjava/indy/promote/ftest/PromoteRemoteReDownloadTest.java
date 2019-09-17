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
package org.commonjava.indy.promote.ftest;

import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.promote.client.IndyPromoteClientModule;
import org.commonjava.indy.promote.model.PathsPromoteRequest;
import org.commonjava.indy.promote.model.PathsPromoteResult;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * GIVEN:
 * <ul>
 *     <li>RemoteRepository A with path P but never retrieved (same to retrieved & expired, to simplify the test)</li>
 *     <li>HostedRepository B</li>
 * </ul>
 * <br/>
 * WHEN:
 * <ul>
 *     <li>RemoteRepository A with path P is promoted to B</li>
 * </ul>
 * <br/>
 * THEN:
 * <ol>
 *     <li>Promotion succeeds with path P being copied to B</li>
 * </ol>
 */
public class PromoteRemoteReDownloadTest
                extends AbstractIndyFunctionalTest
{

    private static final String CONTENT = "This is a test";

    private static final String REMOTE = "remote";

    protected final String PATH = "/path/to/something";

    protected RemoteRepository source;

    protected HostedRepository target;

    protected final IndyPromoteClientModule promotions = new IndyPromoteClientModule();

    @Rule
    public ExpectationServer server = new ExpectationServer( "repos" );

    @Before
    public void setupRepos() throws Exception
    {
        final String changelog = "Setup " + name.getMethodName();
        final IndyPromoteClientModule module = client.module( IndyPromoteClientModule.class );
        System.out.printf( "\n\n\n\nBASE-URL: %s\nPROMOTE-URL: %s\nROLLBACK-URL: %s\n\n\n\n", client.getBaseUrl(),
                           module.promoteUrl(), module.rollbackUrl() );

        server.expect( server.formatUrl( REMOTE, PATH ), 200, CONTENT );

        source = new RemoteRepository( MAVEN_PKG_KEY, "source", server.formatUrl( REMOTE ) );
        source = client.stores().create( source, changelog, RemoteRepository.class );

        target = new HostedRepository( MAVEN_PKG_KEY, "target" );
        target = client.stores().create( target, changelog, HostedRepository.class );
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Collections.singletonList( promotions );
    }

    @Test
    public void run() throws Exception
    {
        PathsPromoteResult result = client.module( IndyPromoteClientModule.class )
                                          .promoteByPath( new PathsPromoteRequest( source.getKey(), target.getKey(),
                                                                                   PATH ) );

        Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 1 ) );
        assertThat( result.getError(), nullValue() );
    }
}
