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

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.HostedRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;

import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;

/**
 * Given two snapshot pom paths and test the metadata merge.
 */
public class HostedMetadataMergeSnapshotTest
                extends AbstractContentManagementTest
{
    private static final String HOSTED_A_NAME= "A";

    private static final String A_VERSION = "1.0-SNAPSHOT";
    private static final String B_VERSION = "1.1-SNAPSHOT";

    private static final String PATH = "/org/foo/bar/maven-metadata.xml";

    private static final String SNAPSHOT_METADATA_PATH = "/org/foo/bar/%version%/maven-metadata.xml";

    private static final String POM_PATH_TEMPLATE = "/org/foo/bar/%version%/bar-%version%.pom";

    /* @formatter:off */
    private static final String POM_CONTENT_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<project>\n" +
        "  <modelVersion>4.0.0</modelVersion>\n" +
        "  <groupId>org.foo</groupId>\n" +
        "  <artifactId>bar</artifactId>\n" +
        "  <version>%version%</version>\n" +
        "  <name>Bar</name>\n" +
        "  <dependencies>\n" +
        "    <dependency>\n" +
        "      <groupId>org.something</groupId>\n" +
        "      <artifactId>oh</artifactId>\n" +
        "      <version>1.0.1</version>\n" +
        "    </dependency>\n" +
        "  </dependencies>\n" +
        "</project>\n";

    private static final String AFTER_PROMOTE_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<metadata>\n" +
        "  <groupId>org.foo</groupId>\n" +
        "  <artifactId>bar</artifactId>\n" +
        "  <versioning>\n" +
        "    <latest>1.1-SNAPSHOT</latest>\n" +
        "    <versions>\n" +
        "      <version>1.0-SNAPSHOT</version>\n" +
        "      <version>1.1-SNAPSHOT</version>\n" +
        "    </versions>\n" +
        "    <lastUpdated>20150722164334</lastUpdated>\n" +
        "  </versioning>\n" +
        "</metadata>\n";

    private static final String SNAPSHOT_METADATA_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<metadata>\n"
        + "  <groupId>org.foo</groupId>\n"
        + "  <artifactId>bar</artifactId>\n"
        + "  <version>1.1-SNAPSHOT</version>\n"
        + "  <versioning>\n"
        + "    <snapshot>\n"
        + "      <localCopy>true</localCopy>\n"
        + "    </snapshot>\n"
        + "    <snapshotVersions>\n"
        + "      <snapshotVersion>\n"
        + "        <extension>pom</extension>\n"
        + "        <value>1.1-SNAPSHOT</value>\n"
        + "        <updated>20190629074244</updated>\n"
        + "      </snapshotVersion>\n"
        + "    </snapshotVersions>\n"
        + "  </versioning>\n"
        + "</metadata>\n";
    /* @formatter:on */

    private HostedRepository hosted;

    private String aPomPath;
    private String aPomContent;

    private String bPomPath;
    private String bPomContent;

    @Before
    public void setupRepos()
                    throws IndyClientException
    {
        String message = "test setup";

        hosted = new HostedRepository( PKG_TYPE_MAVEN, HOSTED_A_NAME );
        hosted.setAllowSnapshots( true );
        hosted = client.stores().create( hosted, message, HostedRepository.class );

        aPomPath = POM_PATH_TEMPLATE.replaceAll( "%version%", A_VERSION );
        aPomContent = POM_CONTENT_TEMPLATE.replaceAll( "%version%", A_VERSION );
        client.content()
              .store( hosted.getKey(), aPomPath, new ByteArrayInputStream(
                              aPomContent.getBytes() ) );

        bPomPath = POM_PATH_TEMPLATE.replaceAll( "%version%", B_VERSION );
        bPomContent = POM_CONTENT_TEMPLATE.replaceAll( "%version%", B_VERSION );
        client.content()
              .store( hosted.getKey(), bPomPath, new ByteArrayInputStream(
                              bPomContent.getBytes() ) );
    }

    @Test
    @Category( EventDependent.class )
    public void run()
                    throws Exception
    {
        assertMetadataContent( hosted, PATH, AFTER_PROMOTE_CONTENT );

        assertMetadataContent( hosted, SNAPSHOT_METADATA_PATH.replaceAll( "%version%", B_VERSION ),
                               SNAPSHOT_METADATA_CONTENT );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }


}