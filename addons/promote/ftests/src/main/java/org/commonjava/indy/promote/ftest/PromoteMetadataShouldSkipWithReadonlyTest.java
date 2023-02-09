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
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Check that metadata should be skipped correctly during promotion with no error if the target repo is set to read-only for path promotion
 * <br/>
 * GIVEN:
 * <ul>
 *     <li>HostedRepository target contains paths for http metadata and signature metadata for a maven metadata.</li>
 *     <li>HostedRepository target set to readonly</li>
 * </ul>
 * <br/>
 * WHEN:
 * <ul>
 *     <li>Repository source will promote same paths to target</li>
 * </ul>
 * <br/>
 * THEN:
 * <ul>
 *     <li>These meta files can be promoted to target of skipping with no error</li>
 * </ul>
 */
public class PromoteMetadataShouldSkipWithReadonlyTest
        extends AbstractContentManagementTest
{
    private static final String HOSTED_TARGET_NAME = "target";

    private static final String HOSTED_SOURCE_NAME = "source";

    private HostedRepository target;

    private HostedRepository source;

    private static final String ARTIFACT_PATH = "/org/foo/bar/maven-metadata.xml";

    private static final String HTTP_META_PATH = "/org/foo/bar/maven-metadata.xml.http-metadata.json";

    private static final String MD5_META_PATH = "/org/foo/bar/maven-metadata.xml.md5";

    private static final String HTTP_META_CONTENT_TARGET = "{\"test\":\"target\"}";

    private static final String HTTP_META_CONTENT_SOURCE = "{\"test\":\"source\"}";

    private static final String MD5_META_CONTENT_TARGET = "testmd5intarget";

    private static final String MD5_META_CONTENT_SOURCE = "testmd5insource";

    private static final String ARTIFACT_CONTENT = "This is a test metadata";

    private final IndyPromoteClientModule promote = new IndyPromoteClientModule();

    @Before
    public void setupRepos()
            throws Exception
    {
        String message = "test setup";

        target = client.stores()
                       .create( new HostedRepository( PKG_TYPE_MAVEN, HOSTED_TARGET_NAME ), message,
                                HostedRepository.class );
        source = client.stores()
                       .create( new HostedRepository( PKG_TYPE_MAVEN, HOSTED_SOURCE_NAME ), message,
                                HostedRepository.class );

        client.content()
              .store( target.getKey(), HTTP_META_PATH,
                      new ByteArrayInputStream( HTTP_META_CONTENT_TARGET.getBytes() ) );
        client.content()
              .store( target.getKey(), MD5_META_PATH, new ByteArrayInputStream( MD5_META_CONTENT_TARGET.getBytes() ) );

        client.content()
              .store( source.getKey(), HTTP_META_PATH,
                      new ByteArrayInputStream( HTTP_META_CONTENT_SOURCE.getBytes() ) );
        client.content()
              .store( source.getKey(), MD5_META_PATH, new ByteArrayInputStream( MD5_META_CONTENT_SOURCE.getBytes() ) );
        client.content()
              .store( source.getKey(), ARTIFACT_PATH, new ByteArrayInputStream( ARTIFACT_CONTENT.getBytes() ) );

        target.setReadonly( true );
        client.stores().update( target, message );

    }

    @Test
    @Category( EventDependent.class )
    public void run()
            throws Exception
    {
        PathsPromoteRequest request =
                new PathsPromoteRequest( source.getKey(), target.getKey(), ARTIFACT_PATH, HTTP_META_PATH,
                                         MD5_META_PATH );

        request.setFailWhenExists( true );

        PathsPromoteResult response = promote.promoteByPath( request );

        assertThat( response.succeeded(), equalTo( true ) );
        assertThat( response.getSkippedPaths().contains( HTTP_META_PATH ), equalTo( true ) );
        assertThat( response.getSkippedPaths().contains( MD5_META_PATH ), equalTo( true ) );
        assertThat( response.getSkippedPaths().contains( ARTIFACT_PATH ), equalTo( true ) );

        waitForEventPropagation();
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
