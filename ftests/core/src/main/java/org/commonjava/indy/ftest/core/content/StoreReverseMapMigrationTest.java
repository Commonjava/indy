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
package org.commonjava.indy.ftest.core.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.fail;

/**
 * GIVEN:
 * <ul>
 *     <li>Hosted repos A,B and C exist</li>
 *     <li>Hosted repos A, B and C already contain path org/foo/bar/maven-metadata.xml</li>
 *     <li>Group X contains A, B </li>
 *     <li>Group Y contains X</li>
 * </ul>
 *
 * WHEN:
 * <ul>
 *     <li>Path org/foo/bar/maven-metadata.xml is accessed from Group Y, and has its aggregated metadata generated</li>
 *     <li>Hosted repo C is appended to the membership of X</li>
 * </ul>
 *
 * THEN:
 * <ul>
 *    <li>org/foo/bar/maven-metadata.xml should be removed and regenerated for Y</li>
 * </ul>
 *
 */
public class StoreReverseMapMigrationTest
        extends AbstractContentManagementTest
{
    final String repoA = "hostedA";
    final String repoB = "hostedB";
    final String repoC = "hostedC";

    final String groupX = "groupX";
    final String groupY = "groupY";

    final String path = "org/foo/bar/maven-metadata.xml";

    HostedRepository hostedA = new HostedRepository( PKG_TYPE_MAVEN, repoA );
    HostedRepository hostedB = new HostedRepository( PKG_TYPE_MAVEN, repoB );
    HostedRepository hostedC = new HostedRepository( PKG_TYPE_MAVEN, repoC );

    Group gX = new Group( PKG_TYPE_MAVEN, groupX, hostedA.getKey(), hostedB.getKey() );
    Group gY = new Group( PKG_TYPE_MAVEN,groupY, gX.getKey() );

    @Override
    protected void initTestData( CoreServerFixture fixture )
            throws IOException
    {
        ObjectMapper mapper = new IndyObjectMapper( true );

        writeDataFile( "indy/maven/hosted/" + repoA + ".json", mapper.writeValueAsString( hostedA ) );
        writeDataFile( "indy/maven/hosted/" + repoB + ".json", mapper.writeValueAsString( hostedB ) );
        writeDataFile( "indy/maven/hosted/" + repoC + ".json", mapper.writeValueAsString( hostedC ) );

        writeDataFile( "indy/maven/group/" + groupX + ".json", mapper.writeValueAsString( gX ) );
        writeDataFile( "indy/maven/group/" + groupY + ".json", mapper.writeValueAsString( gY ) );

        super.initTestData( fixture );
    }

    @Test
    public void run()
            throws Exception
    {
        /* @formatter:off */
        final String repoAContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<metadata>\n" +
            "  <groupId>org.foo</groupId>\n" +
            "  <artifactId>bar</artifactId>\n" +
            "  <versioning>\n" +
            "    <latest>1.0</latest>\n" +
            "    <release>1.0</release>\n" +
            "    <versions>\n" +
            "      <version>1.0</version>\n" +
            "    </versions>\n" +
            "    <lastUpdated>20150721164334</lastUpdated>\n" +
            "  </versioning>\n" +
            "</metadata>\n";
        /* @formatter:on */

        /* @formatter:off */
        final String repoBContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<metadata>\n" +
            "  <groupId>org.foo</groupId>\n" +
            "  <artifactId>bar</artifactId>\n" +
            "  <versioning>\n" +
            "    <latest>2.0</latest>\n" +
            "    <release>2.0</release>\n" +
            "    <versions>\n" +
            "      <version>2.0</version>\n" +
            "    </versions>\n" +
            "    <lastUpdated>20160722164334</lastUpdated>\n" +
            "  </versioning>\n" +
            "</metadata>\n";
        /* @formatter:on */

        /* @formatter:off */
        final String repoCContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<metadata>\n" +
            "  <groupId>org.foo</groupId>\n" +
            "  <artifactId>bar</artifactId>\n" +
            "  <versioning>\n" +
            "    <latest>3.0</latest>\n" +
            "    <release>3.0</release>\n" +
            "    <versions>\n" +
            "      <version>3.0</version>\n" +
            "    </versions>\n" +
            "    <lastUpdated>20160723164334</lastUpdated>\n" +
            "  </versioning>\n" +
            "</metadata>\n";
        /* @formatter:on */

        createHostedStorePath( hostedA, path, repoAContent );
        try (final InputStream stream = client.content().get( hostedA.getKey(), path ))
        {
            assertContent( repoAContent, IOUtils.toString( stream ) );
        }

        createHostedStorePath( hostedB, path, repoBContent );
        try (final InputStream stream = client.content().get( hostedB.getKey(), path ))
        {
            assertContent( repoBContent, IOUtils.toString( stream ) );
        }

        createHostedStorePath( hostedC, path, repoCContent );
        try (final InputStream stream = client.content().get( hostedC.getKey(), path ))
        {
            assertContent( repoCContent, IOUtils.toString( stream ) );
        }

        /* @formatter:off */
        final String mergedContent1 =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "  <metadata>\n" +
                "    <groupId>org.foo</groupId>\n" +
                "    <artifactId>bar</artifactId>\n" +
                "    <versioning>\n" +
                "      <latest>2.0</latest>\n" +
                "      <release>2.0</release>\n" +
                "      <versions>\n" +
                "        <version>1.0</version>\n" +
                "        <version>2.0</version>\n" +
                "      </versions>\n"  +
                "      <lastUpdated>20160722164334</lastUpdated>\n" +
                "    </versioning>\n" +
                "</metadata>\n";
        /* @formatter:on */
        try (final InputStream stream = client.content().get( group, gY.getName(), path ))
        {
            assertContent( mergedContent1, IOUtils.toString( stream ) );
        }

        gX.addConstituent( hostedC );
        client.stores().update( gX, "test update" );
        gX = client.stores().load( StoreType.group, gX.getName(), Group.class  );
        logger.debug( "\n\nGroup constituents are:\n  {}\n\n", StringUtils.join( gX.getConstituents(), "\n  " ) );

        waitForEventPropagation();

        /* @formatter:off */
        final String mergedContent2 =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "  <metadata>\n" +
                "    <groupId>org.foo</groupId>\n" +
                "    <artifactId>bar</artifactId>\n" +
                "    <versioning>\n" +
                "      <latest>3.0</latest>\n" +
                "      <release>3.0</release>\n" +
                "      <versions>\n" +
                "        <version>1.0</version>\n" +
                "        <version>2.0</version>\n" +
                "        <version>3.0</version>\n" +
                "      </versions>\n"  +
                "      <lastUpdated>20160723164334</lastUpdated>\n" +
                "    </versioning>\n" +
                "</metadata>\n";
        /* @formatter:on */

        try (final InputStream stream = client.content().get( group, gY.getName(), path ))
        {
            assertContent( mergedContent2, IOUtils.toString( stream )  );
        }
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }

    private HostedRepository createHostedStorePath( final HostedRepository hosted, final String path, final String content )
            throws Exception
    {
        if(StringUtils.isNotBlank( content ))
        {
            client.content().store( hosted.getKey(), path, new ByteArrayInputStream( content.getBytes() ) );
        }
        return hosted;
    }

    private void assertContent( String expectedXml, String actual )
            throws IndyClientException, IOException
    {

        logger.debug( "Comparing downloaded XML:\n\n{}\n\nTo expected XML:\n\n{}\n\n", actual, expectedXml );

        try
        {
            XMLUnit.setIgnoreWhitespace( true );
            XMLUnit.setIgnoreDiffBetweenTextAndCDATA( true );
            XMLUnit.setIgnoreAttributeOrder( true );
            XMLUnit.setIgnoreComments( true );

            assertXMLEqual( expectedXml, actual );
        }
        catch ( SAXException e )
        {
            logger.error( e.getMessage(), e );
            fail( "Downloaded XML not equal to expected XML" );
        }
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        writeConfigFile( "main.conf", "store.manager.standalone=true\n" + readTestResource( "default-test-main.conf" ) );
    }
}
