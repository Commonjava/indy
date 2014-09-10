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
package org.commonjava.aprox.live;

import java.io.File;

import javax.inject.Inject;

import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.live.fixture.ProxyConfigProvider;
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

    protected ChangeSummary summary = new ChangeSummary( "test-user", "test" );

    @Before
    public final void setupAProxLiveTest()
        throws Exception
    {
        proxyManager.install();
        proxyManager.clear( summary );
    }

    protected static TestWarArchiveBuilder createWar( final Class<?> testClass )
    {
        return new TestWarArchiveBuilder( new File( "target/test-assembly.war" ), testClass ).withExtraClasses( AbstractAProxLiveTest.class,
                                                                                                                ProxyConfigProvider.class )
                                                                                             .withLog4jProperties()
                                                                                             .withBeansXml( "META-INF/beans.live.xml" );
    }

}
