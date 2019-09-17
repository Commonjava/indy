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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.promote.client.IndyPromoteClientModule;
import org.commonjava.indy.promote.model.PathsPromoteRequest;
import org.commonjava.indy.promote.model.PathsPromoteResult;
import org.commonjava.indy.promote.model.ValidationResult;
import org.commonjava.test.http.expect.ExpectationServer;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This test case is going to prove following scenario: <br />
 * When:
 * <ul>
 *     <li>A group "group:g" with two constituents: "remote:r" and "hosted:h"</li>
 *     <li>Client http:get to "group:g" for the pomPath in "remote:r"</li>
 *     <li>Client promote pomPath from "remote:r" to "hosted:h"</li>
 * </ul>
 * Then:
 * <ul>
 *     <li>Cache content for pomPath for "hosted:g" should exist</li>
 * </ul>
 *
 */
public class CacheCheckingforPromoteWithRemoteTest
        extends AbstractIndyFunctionalTest
{

    @Rule
    public ExpectationServer server = new ExpectationServer( "repos" );

    private HostedRepository h;

    private RemoteRepository r;

    private final String hId = "h";

    private final String rId = "r";

    private Group g;

    private final String gId = "g";

    protected IndyPromoteClientModule module;

    private final String pomPath = "org/foo/bar/1/foobar-1.pom";

    @Test
    //    @Category( EventDependent.class )
    public void run()
            throws Exception
    {

        /* @formatter:off */
        final String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project>\n" +
            "  <modelVersion>4.0.0</modelVersion>\n" +
            "  <groupId>org.foo</groupId>\n" +
            "  <artifactId>bar</artifactId>\n" +
            "  <version>1</version>\n" +
            "</project>\n";
        /* @formatter:on */

        final String pomUrl = server.formatUrl( rId, pomPath );
        server.expect( pomUrl, 200, content );

        assertContent( getContent( g ), content );

        assertTrue( StringUtils.isBlank( getContent( h ) ) );

        PathsPromoteRequest request = new PathsPromoteRequest( r.getKey(), h.getKey(), pomPath );
        PathsPromoteResult result = module.promoteByPath( request );
        assertThat( result, notNullValue() );

        ValidationResult validations = result.getValidations();
        assertThat( validations, notNullValue() );

        Map<String, String> validatorErrors = validations.getValidatorErrors();
        assertThat( validatorErrors, notNullValue() );

        System.out.println( String.format( "[errors] validation errors: %s", validatorErrors ) );

        //FIXME: some way to check cache directly but not through client
        assertContent( getContent( h ), content );
    }

    @Before
    public void start()
            throws Throwable
    {
        super.start();
        Logger logger = LoggerFactory.getLogger( getClass() );

        logger.debug( "client:{}", client );
        module = client.module( IndyPromoteClientModule.class );

        h = new HostedRepository( hId );
        h = client.stores().create( h, "Creating h", HostedRepository.class );

        r = new RemoteRepository( rId, server.formatUrl( rId ) );
        r = client.stores().create( r, "Creating r", RemoteRepository.class );

        g = new Group( gId, h.getKey(), r.getKey() );
        g = client.stores().create( g, "Creating group", Group.class );

        logger.info( "{} contains members: {}", g, g.getConstituents() );
    }

    private String getContent( ArtifactStore store )
            throws IndyClientException, IOException
    {
        try (InputStream stream = client.content().get( store.getKey(), pomPath ))
        {
            return stream == null ? null : IOUtils.toString( stream );
        }
    }

    private void assertContent( String actual, String expectedXml )
            throws IndyClientException, IOException
    {

        logger.debug( "Comparing downloaded XML:\n\n{}\n\nTo expected XML:\n\n{}\n\n", actual, expectedXml );

        try
        {
            XMLUnit.setIgnoreWhitespace( true );
            XMLUnit.setIgnoreDiffBetweenTextAndCDATA( true );
            XMLUnit.setIgnoreAttributeOrder( true );
            XMLUnit.setIgnoreComments( true );

            assertXMLEqual( actual, expectedXml );
        }
        catch ( SAXException e )
        {
            e.printStackTrace();
            fail( "Downloaded XML not equal to expected XML" );
        }
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Collections.singletonList( new IndyPromoteClientModule() );
    }

}
