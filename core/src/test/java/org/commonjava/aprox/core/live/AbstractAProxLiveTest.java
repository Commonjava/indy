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
package org.commonjava.aprox.core.live;

import java.io.File;

import javax.inject.Inject;

import org.commonjava.aprox.core.data.StoreDataManager;
import org.commonjava.aprox.core.live.fixture.ProxyConfigProvider;
import org.commonjava.web.json.test.WebFixture;
import org.commonjava.web.test.fixture.TestWarArchiveBuilder;
import org.junit.Before;
import org.junit.Rule;

public class AbstractAProxLiveTest
{

    @Inject
    protected StoreDataManager proxyManager;

    @Rule
    public WebFixture webFixture = new WebFixture();

    @Before
    public final void setupAProxLiveTest()
        throws Exception
    {
        proxyManager.install();
    }

    protected static TestWarArchiveBuilder createWar( final Class<?> testClass )
    {
        return new TestWarArchiveBuilder( new File( "target/test.war" ), testClass ).withExtraClasses( AbstractAProxLiveTest.class,
                                                                                                       ProxyConfigProvider.class )
                                                                                    .withLog4jProperties();
    }

}
