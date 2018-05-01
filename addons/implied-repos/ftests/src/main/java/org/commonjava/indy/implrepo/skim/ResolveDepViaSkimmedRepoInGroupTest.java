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
package org.commonjava.indy.implrepo.skim;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.ftest.core.category.TimingDependent;
import org.commonjava.indy.model.core.StoreType;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.InputStream;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ResolveDepViaSkimmedRepoInGroupTest
    extends AbstractSkimFunctionalTest
{

    private static final String REPO = "i-repo-one";

    @Test
    public void downloadPomInImpliedRepoViaGroup()
            throws Exception
    {
        final String repoUrl = server.formatUrl( REPO );
        final PomRef pomRef = loadPom( "one-repo", Collections.singletonMap( "one-repo.url", repoUrl ) );
        final PomRef simplePomRef = loadPom( "simple", Collections.<String, String>emptyMap() );

        server.expect( server.formatUrl( TEST_REPO, pomRef.path ), 200, pomRef.pom );
        server.expect( server.formatUrl( REPO, simplePomRef.path ), 200, simplePomRef.pom );

        InputStream stream = client.content().get( StoreType.group, PUBLIC, pomRef.path );

        String downloaded = IOUtils.toString( stream );
        IOUtils.closeQuietly( stream );

        assertThat( "SANITY: downloaded POM with repo declaration is wrong!", downloaded, equalTo( pomRef.pom ) );

        // give the events time to propagate
        waitForEventPropagation();

        stream = client.content().get( StoreType.group, PUBLIC, simplePomRef.path );

        assertNotNull("Stream of content should not be null", stream );

        downloaded = IOUtils.toString( stream );
        IOUtils.closeQuietly( stream );

        assertThat( "SANITY: downloaded dependency POM is wrong!", downloaded, equalTo( simplePomRef.pom ) );
    }

}
