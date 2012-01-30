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
package org.commonjava.aprox.depbase.live;

import static org.apache.commons.io.FileUtils.forceDelete;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.commonjava.aprox.core.data.ProxyDataManager;
import org.commonjava.aprox.core.model.ModelFactory;
import org.commonjava.aprox.depbase.fixture.TestConfigProvider;
import org.commonjava.depbase.data.DepbaseDataManager;
import org.commonjava.web.test.fixture.TestWarArchiveBuilder;
import org.commonjava.web.test.fixture.WebFixture;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;

public class AbstractAProxDepbaseLiveTest
{

    private static File repoRoot;

    @Inject
    protected ProxyDataManager proxyManager;

    @Inject
    protected ModelFactory modelFactory;

    @Inject
    protected DepbaseDataManager dataManager;

    @Rule
    public WebFixture webFixture = new WebFixture();

    @BeforeClass
    public static void setRepoRootDir()
        throws IOException
    {
        repoRoot = File.createTempFile( "repo.root.", ".dir" );
        System.setProperty( TestConfigProvider.REPO_ROOT_DIR, repoRoot.getAbsolutePath() );
    }

    @AfterClass
    public static void clearRepoRootDir()
        throws IOException
    {
        if ( repoRoot != null && repoRoot.exists() )
        {
            forceDelete( repoRoot );
        }
    }

    @Before
    public final void setupAProxLiveTest()
        throws Exception
    {
        dataManager.install();
        proxyManager.install();
        webFixture.getSerializer()
                  .registerSerializationAdapters( modelFactory );
    }

    protected static WebArchive createWar( final Class<?> testClass )
    {
        return new TestWarArchiveBuilder( new File( "target/test.war" ), testClass ).withExtraClasses( AbstractAProxDepbaseLiveTest.class,
                                                                                                       TestConfigProvider.class )
                                                                                    .withLog4jProperties()
                                                                                    .build();
    }

}
