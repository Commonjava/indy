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
import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.atlas.maven.graph.jackson.ProjectRelationshipSerializerModule;
import org.commonjava.atlas.maven.graph.model.EProjectDirectRelationships;
import org.commonjava.atlas.maven.ident.jackson.ProjectVersionRefSerializerModule;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by ruhan on 2/17/17.
 *
 * <b>GIVEN:</b>
 * <ul>
 *     <li>{@link Group} A contains {@link RemoteRepository} B which proxy an upstream server</li>
 *     <li>Path PD points to a dependant POM file in {@link RemoteRepository} B</li>
 *     <li>Path PC points to a consumer POM file in {@link RemoteRepository} B</li>
 *     <li>Path R points to the Rel file of the consumer POM</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Path PC is requested from {@link Group} A</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>{@link Group} A returns notNull (exists) for Path PC</li>
 *     <li>{@link Group} A returns notNull (exists) for Path R</li>
 *     <li>Rel file from Path R is identical to expected output file</li>
 * </ul>
 */
public class PomDownloadSimpleDepTest
        extends AbstractRelateFunctionalTest
{
    private static final String resource = "simple-dep";

    private static final String repo = resource + "/repo";

    private static final String pathDep = "org/bar/dep/1.1/dep-1.1.pom";

    private static final String pathConsumer = "org/foo/consumer/1/consumer-1.pom";

    private static final String pathConsumerRel = pathConsumer + ".rel";

    private static final String repo1 = "repo1";

    private static final String group1 = "group1";

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

        // Create remote repository
        RemoteRepository remote1 = new RemoteRepository( repo1, server.formatUrl( repo1 ) );
        client.stores().create( remote1, "adding remote1", RemoteRepository.class );

        // Add to a group
        client.stores().create( new Group( group1, remote1.getKey() ), "adding group", Group.class );

        // Get consumer pom via group
        InputStream is = client.content().get( group, group1, pathConsumer );
        String s = IOUtils.toString( is );
        logger.debug( ">>> " + s );
        assertThat( s, equalTo( consumerPom ) );

        waitForEventPropagation();

        boolean exists = false;

        // Check consumer pom exists on group
        exists = client.content().exists( group, group1, pathConsumer, true );
        assertThat( exists, equalTo( true ) );

        // Check consumer rel exists on group
        exists = client.content().exists( group, group1, pathConsumerRel );
        assertThat( exists, equalTo( true ) );

        // Check consumer rel exists on remote
        exists = client.content().exists( remote, repo1, pathConsumerRel, true );
        assertThat( exists, equalTo( true ) );

        // Check consumer rel content is not empty via group
        InputStream ris = client.content().get( group, group1, pathConsumerRel );
        String rel = IOUtils.toString( ris );
        logger.debug( ">>> " + rel );
        assertThat( StringUtils.isNotEmpty( rel ), equalTo( true ) );

        // Check consumer rel output
        String output = readTestResource( resource + "/output/rel.json" );
        EProjectDirectRelationships eRel = mapper.readValue( rel, EProjectDirectRelationships.class );
        EProjectDirectRelationships eRelOutput = mapper.readValue( output, EProjectDirectRelationships.class );
        assertThat( eRel.getDependencies(), equalTo( eRelOutput.getDependencies() ) );
    }
}
