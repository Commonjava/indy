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
package org.commonjava.indy.implrepo.skim;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.ftest.core.category.TimingDependent;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.maven.parse.PomPeek;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Category( { EventDependent.class, TimingDependent.class } )
public class AbstractSkimFunctionalTest
    extends AbstractIndyFunctionalTest
{

    protected static final String TEST_REPO = "test";

    protected static final String PUBLIC = "public";

    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    @Rule
    public ExpectationServer server = new ExpectationServer( "repos" );

    @Before
    public void setupTestStores()
        throws Exception
    {
        final String changelog = "Create test structures";

        final String url = server.formatUrl( TEST_REPO );
        final RemoteRepository testRepo = client.stores()
                                                .create( new RemoteRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY,
                                                                               TEST_REPO, url ), changelog,
                                                         RemoteRepository.class );

        Group g;
        final StoreKey groupKey = new StoreKey( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, group, PUBLIC );
        if ( client.stores().exists( groupKey ) )
        {
            System.out.println( "Loading pre-existing public group." );
            g = client.stores()
                      .load( groupKey, Group.class );
        }
        else
        {
            System.out.println( "Creating new group 'public'" );
            g = client.stores()
                      .create( new Group( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, PUBLIC ), changelog, Group.class );
        }

        g.setConstituents( Collections.singletonList( testRepo.getKey() ) );
        client.stores()
              .update( g, changelog );
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
        throws IOException
    {
        writeConfigFile( "conf.d/implied-repos.conf", "[implied-repos]\nenabled=true\nenabled.group=" + PUBLIC );
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
