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

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.inject.TestData;
import org.commonjava.aprox.sec.conf.DefaultAdminConfiguration;
import org.commonjava.badgr.conf.AdminConfiguration;
import org.commonjava.web.json.test.WebFixture;
import org.junit.Before;
import org.junit.Rule;

public class AbstractAProxSecLiveTest
{

    @Inject
    protected StoreDataManager proxyManager;

    @Rule
    public WebFixture http = new WebFixture();

    @Before
    public final void setupAProxLiveTest()
        throws Exception
    {
        proxyManager.install();
    }

    @Singleton
    public static final class ConfigProvider
    {
        private AdminConfiguration adminConfig;

        @Produces
        @Default
        @TestData
        public synchronized AdminConfiguration getAdminConfig()
        {
            if ( adminConfig == null )
            {
                adminConfig = new DefaultAdminConfiguration();
            }

            return adminConfig;
        }
    }

}
