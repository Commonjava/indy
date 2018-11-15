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
package org.commonjava.indy.relate.ftest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.atlas.maven.graph.jackson.ProjectRelationshipSerializerModule;
import org.commonjava.atlas.maven.graph.model.EProjectDirectRelationships;
import org.commonjava.atlas.maven.ident.jackson.ProjectVersionRefSerializerModule;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by ruhan on 2/17/17.
 *
 * <b>GIVEN:</b>
 * <ul>
 *     <li>{@link RemoteRepository} A proxy an upstream server</li>
 *     <li>Path P points to a parent POM file in {@link RemoteRepository} A</li>
 *     <li>Path C points to a consumer POM file in {@link RemoteRepository} A</li>
 *     <li>Path CR points to the Rel file of the consumer POM</li>
 *     <li>Path PR points to the Rel file of the parent POM</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Path C is requested from {@link RemoteRepository} A</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>{@link RemoteRepository} A returns notNull (exists) for Path C</li>
 *     <li>{@link RemoteRepository} A returns notNull (exists) for Path CR</li>
 *     <li>{@link RemoteRepository} A returns notNull (exists) for Path P</li>
 *     <li>{@link RemoteRepository} A returns notNull (exists) for Path PR</li>
 *     <li>Rel file from Path CR is identical to expected output file</li>
 * </ul>
 */
public class PomDownloadDepFromParentTest
                extends AbstractRelateFunctionalTest
{
    private static final String resource = "dep-from-parent";

    private static final String repo = resource + "/repo";

    private static final String pathDep = "org/bar/dep/1.1/dep-1.1.pom";

    private static final String pathConsumer = "org/foo/consumer/1/consumer-1.pom";

    private static final String pathConsumerParent = "org/foo/parent/1/parent-1.pom";

    private static final String pathConsumerRel = pathConsumer + ".rel";

    private static final String pathConsumerParentRel = pathConsumerParent + ".rel";

    private static final String repo1 = "repo1";

    @Rule
    public ExpectationServer server = new ExpectationServer();

    ObjectMapper mapper;

    @Before
    public void init()
    {
        mapper = new ObjectMapper();
        mapper.registerModules( new ProjectVersionRefSerializerModule(), new ProjectRelationshipSerializerModule() );
    }

    @Test
    public void run() throws Exception
    {
        String depPom = readTestResource( repo + "/" + pathDep );
        server.expect( server.formatUrl( repo1, pathDep ), 200, depPom );

        String consumerPom = readTestResource( repo + "/" + pathConsumer );
        server.expect( server.formatUrl( repo1, pathConsumer ), 200, consumerPom );

        String consumerParentPom = readTestResource( repo + "/" + pathConsumerParent );
        server.expect( server.formatUrl( repo1, pathConsumerParent ), 200, consumerParentPom );

        // Create remote repositories
        RemoteRepository remote1 = new RemoteRepository( repo1, server.formatUrl( repo1 ) );
        client.stores().create( remote1, "adding remote1", RemoteRepository.class );

        // Get consumer pom
        InputStream is = client.content().get( remote, repo1, pathConsumer );
        assertThat( is, notNullValue() );
        String s = IOUtils.toString( is );
        logger.debug( ">>> " + s );
        assertThat( s, equalTo( consumerPom ) );

        waitForEventPropagation();

        boolean exists = false;

        // Check consumer rel exists
        exists = client.content().exists( remote, repo1, pathConsumerRel, true );
        assertThat( exists, equalTo( true ) );

        // Check consumer's parent pom exists
        exists = client.content().exists( remote, repo1, pathConsumerParent, true );
        assertThat( exists, equalTo( true ) );

        // Check consumer's parent rel exists
        exists = client.content().exists( remote, repo1, pathConsumerParentRel, true );
        assertThat( exists, equalTo( true ) );

        // Check consumer rel content
        InputStream ris = client.content().get( remote, repo1, pathConsumerRel );
        assertThat( ris, notNullValue() );
        String rel = IOUtils.toString( ris );
        logger.debug( ">>> " + rel );
        String output = readTestResource( resource + "/output/rel.json" );
        EProjectDirectRelationships eRel = mapper.readValue( rel, EProjectDirectRelationships.class );
        EProjectDirectRelationships eRelOutput = mapper.readValue( output, EProjectDirectRelationships.class );
        assertThat( eRel.getParent(), equalTo( eRelOutput.getParent() ) );
    }
}
