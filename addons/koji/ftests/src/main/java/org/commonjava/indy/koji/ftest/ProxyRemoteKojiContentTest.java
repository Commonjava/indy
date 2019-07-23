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
package org.commonjava.indy.koji.ftest;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class ProxyRemoteKojiContentTest
                extends ExternalKojiTest
{
    final Logger logger = LoggerFactory.getLogger( getClass() );

    @Ignore
    @Test
    public void proxyRemoteKojiArtifact() throws Exception
    {
        final String path = "org/dashbuilder/dashbuilder-all/0.4.0.Final-redhat-10/dashbuilder-all-0.4.0.Final-redhat-10.pom";

        // would be slow the first time to get an artifact
        long elapse = contentDownloadTime( pseudoGroupName, path );
        logger.debug( "Get (first) use " + elapse + " milliseconds" );

        // the following get should have been cached and fast
        elapse = contentDownloadTime( pseudoGroupName, path );
        logger.debug( "Get (second) use " + elapse + " milliseconds" );

        getKojiRemoteRepositories();
    }

    @Ignore
    @Test
    public void proxyNotExistingKojiArtifact() throws Exception
    {
        final String path = "non/existing/thing/0.1/thing-0.1.pom";

        InputStream stream = client.content().get( group, pseudoGroupName, path );
        assertThat( stream, nullValue() );
    }

    @Ignore
    @Test
    public void proxyBinaryRemoteKojiArtifact() throws Exception
    {
        final String path = "org/apache/apache/18/apache-18.pom";

        contentDownloadTime( pseudoGroupName, path );

        getKojiRemoteRepositories();
    }

}
