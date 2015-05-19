/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.aprox.implrepo.skim;

import static org.commonjava.aprox.model.core.StoreType.group;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.boot.AproxBootException;
import org.commonjava.aprox.ftest.core.AbstractAproxFunctionalTest;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.test.fixture.core.CoreServerFixture;
import org.commonjava.aprox.test.fixture.core.TestHttpServer;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.maven.parse.PomPeek;
import org.junit.Before;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractSkimFunctionalTest
    extends AbstractAproxFunctionalTest
{

    protected static final String TEST_REPO = "test";

    protected static final String PUBLIC = "public";

    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    @Rule
    public TestHttpServer server = new TestHttpServer( "repos" );

    @Before
    public void setupTestStores()
        throws Exception
    {
        final String changelog = "Create test structures";

        final String url = server.formatUrl( TEST_REPO );
        final RemoteRepository testRepo =
            client.stores()
                  .create( new RemoteRepository( TEST_REPO, url ), changelog, RemoteRepository.class );

        Group g;
        if ( client.stores()
                   .exists( group, PUBLIC ) )
        {
            System.out.println( "Loading pre-existing public group." );
            g = client.stores()
                      .load( group, PUBLIC, Group.class );
        }
        else
        {
            System.out.println( "Creating new group 'public'" );
            g = client.stores()
                      .create( new Group( PUBLIC ), changelog, Group.class );
        }

        g.setConstituents( Arrays.asList( testRepo.getKey() ) );
        client.stores()
              .update( g, changelog );
    }

    @Override
    protected CoreServerFixture newServerFixture()
        throws AproxBootException, IOException
    {
        final CoreServerFixture fixture = new CoreServerFixture();

        final File confFile = new File( fixture.getBootOptions()
                                               .getAproxHome(), "etc/aprox/conf.d/implied-repos.conf" );

        confFile.getParentFile()
                .mkdirs();

        logger.info( "Writing implied-repos configuration to: {}", confFile );
        FileUtils.write( confFile, "[implied-repos]\nenabled=true" );

        return fixture;
    }

    protected PomRef loadPom( final String name, final Map<String, String> substitutions )
    {
        try
        {
            final InputStream stream = Thread.currentThread()
                                             .getContextClassLoader()
                                             .getResourceAsStream( name + ".pom" );

            String pom = IOUtils.toString( stream );
            IOUtils.closeQuietly( stream );

            for ( final Map.Entry<String, String> entry : substitutions.entrySet() )
            {
                pom = pom.replace( "@" + entry.getKey() + "@", entry.getValue() );
            }

            final PomPeek peek = new PomPeek( pom, false );
            final ProjectVersionRef gav = peek.getKey();

            final String path =
                String.format( "%s/%s/%s/%s-%s.pom", gav.getGroupId()
                                                        .replace( '.', '/' ), gav.getArtifactId(),
                               gav.getVersionString(), gav.getArtifactId(), gav.getVersionString() );

            return new PomRef( pom, path );
        }
        catch ( final Exception e )
        {
            e.printStackTrace();
            fail( "Failed to read POM from: " + name );
        }

        return null;
    }

    protected static final class PomRef
    {
        PomRef( final String pom, final String path )
        {
            this.pom = pom;
            this.path = path;
        }

        protected final String pom;

        protected final String path;
    }
}
