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
package org.commonjava.aprox.sec.live;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.aprox.core.data.ProxyDataManager;
import org.commonjava.aprox.core.inject.AproxData;
import org.commonjava.auth.couch.inject.UserData;
import org.commonjava.couch.change.CouchChangeListener;
import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.couch.conf.DefaultCouchDBConfiguration;
import org.commonjava.couch.db.CouchManager;
import org.commonjava.couch.user.web.test.AbstractUserRESTCouchTest;
import org.commonjava.web.test.fixture.WebFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

public class AbstractAProxSecLiveTest
    extends AbstractUserRESTCouchTest
{

    @Inject
    protected ProxyDataManager proxyManager;

    @Inject
    @AproxData
    protected CouchChangeListener changeListener;

    @Inject
    @AproxData
    private CouchManager couch;

    @Rule
    public WebFixture http = new WebFixture();

    @Before
    public final void setupAProxLiveTest()
        throws Exception
    {
        proxyManager.install();
        changeListener.startup();
    }

    @After
    public final void teardownAProxLiveTest()
        throws Exception
    {
        changeListener.shutdown();
        while ( changeListener.isRunning() )
        {
            synchronized ( changeListener )
            {
                System.out.println( "Waiting 2s for change listener to shutdown..." );
                changeListener.wait( 2000 );
            }
        }

        couch.dropDatabase();
    }

    @Override
    protected CouchManager getCouchManager()
    {
        return couch;
    }

    @Singleton
    public static final class ConfigProvider
    {
        @Inject
        @UserData
        private CouchDBConfiguration userConfig;

        private DefaultCouchDBConfiguration conf;

        @Produces
        @AproxData
        public synchronized CouchDBConfiguration getAproxCouchConfig()
        {
            if ( conf == null )
            {
                conf = new DefaultCouchDBConfiguration( userConfig, "test-aprox" );
            }

            return conf;
        }
    }

}
