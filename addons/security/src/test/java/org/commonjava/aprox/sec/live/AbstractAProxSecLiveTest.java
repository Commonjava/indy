/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.sec.live;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.inject.TestData;
import org.commonjava.badgr.conf.AdminConfiguration;
import org.commonjava.badgr.conf.DefaultAdminConfiguration;
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
        proxyManager.clear();
    }

    @javax.enterprise.context.ApplicationScoped
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
