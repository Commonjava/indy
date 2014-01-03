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
package org.commonjava.aprox.live;

import java.io.File;

import javax.inject.Inject;

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

    @Before
    public final void setupAProxLiveTest()
        throws Exception
    {
        proxyManager.install();
        proxyManager.clear();
    }

    protected static TestWarArchiveBuilder createWar( final Class<?> testClass )
    {
        return new TestWarArchiveBuilder( new File( "target/test-assembly.war" ), testClass ).withExtraClasses( AbstractAProxLiveTest.class,
                                                                                                                ProxyConfigProvider.class )
                                                                                             .withLog4jProperties()
                                                                                             .withBeansXml( "META-INF/beans.live.xml" );
    }

}
