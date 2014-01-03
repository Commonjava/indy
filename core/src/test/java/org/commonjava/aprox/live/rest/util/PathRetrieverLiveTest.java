/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.live.rest.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.live.AbstractAProxLiveTest;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Repository;
import org.commonjava.maven.galley.model.Transfer;
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
        final Repository repo = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );
        final String path = "/org/apache/maven/maven-model/3.0.3/maven-model-3.0.3.pom";

        final Transfer stream = downloader.retrieve( repo, path );
        final String pom = IOUtils.toString( stream.openInputStream() );

        assertThat( pom.contains( "<artifactId>maven-model</artifactId>" ), equalTo( true ) );
    }

    @Test
    public void downloadOnePOMFromSecondRepositoryAfterDummyRepoFails()
        throws Exception
    {
        final Repository repo = new Repository( "dummy", "http://www.nowhere.com/" );
        final Repository repo2 = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );

        final String path = "/org/apache/maven/maven-model/3.0.3/maven-model-3.0.3.pom";

        final List<ArtifactStore> repos = new ArrayList<ArtifactStore>();
        repos.add( repo );
        repos.add( repo2 );

        final Transfer stream = downloader.retrieveFirst( repos, path );
        final String pom = IOUtils.toString( stream.openInputStream() );

        assertThat( pom.contains( "<artifactId>maven-model</artifactId>" ), equalTo( true ) );
    }

}
