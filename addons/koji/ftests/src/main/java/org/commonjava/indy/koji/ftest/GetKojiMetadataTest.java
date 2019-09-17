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

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class GetKojiMetadataTest
                extends ExternalKojiTest
{
    final Logger logger = LoggerFactory.getLogger( getClass() );

    @Ignore
    @Test
    public void proxyRemoteKojiArtifact() throws Exception
    {
        final String path = "org/apache/curator/curator-framework/maven-metadata.xml";

        try (InputStream stream = client.content().get( new StoreKey( MAVEN_PKG_KEY, group, pseudoGroupName ), path ) )
        {
            Metadata metadata = new MetadataXpp3Reader().read( stream );
            logger.info( "Got versions:\n\n  {}\n\n", join( metadata.getVersioning().getVersions(), "\n  ") );

            assertThat( metadata.getVersioning().getVersions().contains( "2.11.0.redhat-004" ), equalTo( true ) );
        }
    }

}
