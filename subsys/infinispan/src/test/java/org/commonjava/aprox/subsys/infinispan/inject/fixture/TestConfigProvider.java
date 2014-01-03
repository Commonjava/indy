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
package org.commonjava.aprox.subsys.infinispan.inject.fixture;

import java.net.URL;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.commonjava.aprox.conf.AproxConfigFactory;
import org.commonjava.aprox.inject.TestData;
import org.commonjava.aprox.subsys.infinispan.conf.CacheConfiguration;
import org.commonjava.web.config.ConfigurationException;

@javax.enterprise.context.ApplicationScoped
public class TestConfigProvider
{
    @Produces
    @Default
    public AproxConfigFactory getFactory()
    {
        return new AproxConfigFactory()
        {
            @Override
            public <T> T getConfiguration( final Class<T> configCls )
                throws ConfigurationException
            {
                return null;
            }
        };
    }

    @Produces
    @TestData
    @Default
    public CacheConfiguration getCacheConfig()
    {
        final URL resource = Thread.currentThread()
                                   .getContextClassLoader()
                                   .getResource( "infinispan.xml" );

        final CacheConfiguration cc = new CacheConfiguration();
        cc.setPath( resource.getPath() );

        return cc;
    }
}
