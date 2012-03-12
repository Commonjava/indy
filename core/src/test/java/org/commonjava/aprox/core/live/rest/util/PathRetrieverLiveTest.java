/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.core.live.rest.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.core.live.AbstractAProxLiveTest;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.rest.StoreInputStream;
import org.commonjava.aprox.core.rest.util.FileManager;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( Arquillian.class )
public class PathRetrieverLiveTest
    extends AbstractAProxLiveTest
{

    @Deployment
    public static WebArchive createWar()
    {
        return createWar( PathRetrieverLiveTest.class ).build();
    }

    @Inject
    private FileManager downloader;

    @Test
    public void downloadOnePOMFromSingleRepository()
        throws Exception
    {
        final Repository repo = modelFactory.createRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        final String path = "/org/apache/maven/maven-model/3.0.3/maven-model-3.0.3.pom";

        final StoreInputStream stream = downloader.retrieve( repo, path );
        final String pom = IOUtils.toString( stream );

        assertThat( pom.contains( "<artifactId>maven-model</artifactId>" ), equalTo( true ) );
    }

    @Test
    public void downloadOnePOMFromSecondRepositoryAfterDummyRepoFails()
        throws Exception
    {
        final Repository repo = modelFactory.createRepository( "dummy", "http://www.nowhere.com/" );
        final Repository repo2 = modelFactory.createRepository( "central", "http://repo1.maven.apache.org/maven2/" );

        final String path = "/org/apache/maven/maven-model/3.0.3/maven-model-3.0.3.pom";

        final List<ArtifactStore> repos = new ArrayList<ArtifactStore>();
        repos.add( repo );
        repos.add( repo2 );

        final StoreInputStream stream = downloader.retrieveFirst( repos, path );
        assertThat( stream, notNullValue() );

        final String pom = IOUtils.toString( stream );

        assertThat( pom.contains( "<artifactId>maven-model</artifactId>" ), equalTo( true ) );
    }

}
