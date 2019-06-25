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
package org.commonjava.indy.promote.ftest;

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.promote.client.IndyPromoteClientModule;
import org.commonjava.indy.promote.model.PathsPromoteRequest;
import org.commonjava.indy.promote.model.PathsPromoteResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Collections;

import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Check that metadata in a hosted repo is merged with content from a new hosted repository when it is promoted by path.
 * <br/>
 * GIVEN:
 * <ul>
 *     <li>HostedRepositories A and B</li>
 *     <li>HostedRepositories A and B both contain metadata path P</li>
 *     <li>Each metadata file contains different versions of the same project</li>
 * </ul>
 * <br/>
 * WHEN:
 * <ul>
 *     <li>HostedRepository B is merged into HostedRepository A</li>
 *     <li>Metadata path P is requested from HostedRepository A <b>after promotion events have settled</b></li>
 * </ul>
 * <br/>
 * THEN:
 * <ul>
 *     <li>HostedRepository A's metadata path P should reflect values in HostedRepository B's metadata path P</li>
 * </ul>
 */
public class HostedMetadataRemergedOnPathPromoteTest
        extends AbstractContentManagementTest
{
    private static final String HOSTED_A_NAME= "A";
    private static final String HOSTED_B_NAME= "B";

    private static final String A_VERSION = "1.0";
    private static final String B_VERSION = "1.1";

    private static final String PATH = "/org/foo/bar/maven-metadata.xml";

    /* @formatter:off */
    private static final String REPO_CONTENT_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<metadata>\n" +
        "  <groupId>org.foo</groupId>\n" +
        "  <artifactId>bar</artifactId>\n" +
        "  <versioning>\n" +
        "    <latest>%version%</latest>\n" +
        "    <release>%version%</release>\n" +
        "    <versions>\n" +
        "      <version>%version%</version>\n" +
        "    </versions>\n" +
        "    <lastUpdated>20150722164334</lastUpdated>\n" +
        "  </versioning>\n" +
        "</metadata>\n";
    /* @formatter:on */

    /* @formatter:off */
    private static final String AFTER_PROMOTE_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<metadata>\n" +
        "  <groupId>org.foo</groupId>\n" +
        "  <artifactId>bar</artifactId>\n" +
        "  <versioning>\n" +
        "    <latest>1.1</latest>\n" +
        "    <release>1.1</release>\n" +
        "    <versions>\n" +
        "      <version>1.0</version>\n" +
        "      <version>1.1</version>\n" +
        "    </versions>\n" +
        "    <lastUpdated>20150722164334</lastUpdated>\n" +
        "  </versioning>\n" +
        "</metadata>\n";
    /* @formatter:on */

    private HostedRepository a;
    private HostedRepository b;

    private String aPreContent;
    private String bContent;

    private final IndyPromoteClientModule promote = new IndyPromoteClientModule();

    @Before
    public void setupRepos()
            throws IndyClientException
    {
        String message = "test setup";

        a = client.stores().create( new HostedRepository( PKG_TYPE_MAVEN, HOSTED_A_NAME ), message, HostedRepository.class );
        b = client.stores().create( new HostedRepository( PKG_TYPE_MAVEN, HOSTED_B_NAME ), message, HostedRepository.class );

        aPreContent = REPO_CONTENT_TEMPLATE.replaceAll( "%version%", A_VERSION );

        client.content()
              .store( a.getKey(), PATH, new ByteArrayInputStream(
                      aPreContent.getBytes() ) );

        bContent = REPO_CONTENT_TEMPLATE.replaceAll( "%version%", B_VERSION );

        client.content()
              .store( b.getKey(), PATH, new ByteArrayInputStream(
                      bContent.getBytes() ) );
    }

    @Test
    @Category( EventDependent.class )
    public void run()
            throws Exception
    {
        // verify our initial state
        assertContent( a, PATH, aPreContent );

        PathsPromoteRequest request = new PathsPromoteRequest( a.getKey(), b.getKey(), PATH );

        // Pre-existing maven-metadata.xml should NOT cause a failure!
        request.setFailWhenExists( true );

        PathsPromoteResult response = promote.promoteByPath( request );

        assertThat( response.succeeded(), equalTo( true ) );

        waitForEventPropagation();

        // Promotion to repo A should trigger re-merge of maven-metadata.xml, adding the version from repo B to that in A.
        assertContent( a, PATH, AFTER_PROMOTE_CONTENT );
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Collections.singleton( promote );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
