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
package org.commonjava.aprox.subsys.infinispan.conf;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.conf.AbstractAproxConfigInfo;
import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.aprox.conf.AbstractAproxFeatureConfig;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "infinispan" )
@Named( "use-factory-instead" )
@Alternative
public class CacheConfiguration
{
    @javax.enterprise.context.ApplicationScoped
    public static class CacheFeatureConfig
        extends AbstractAproxFeatureConfig<CacheConfiguration, CacheConfiguration>
    {
        @Inject
        private CacheConfigInfo info;

        public CacheFeatureConfig()
        {
            super( CacheConfiguration.class );
        }

        @Produces
        @Default
        public CacheConfiguration getCacheConfig()
            throws ConfigurationException
        {
            return getConfig();
        }

        @Override
        public AproxConfigInfo getInfo()
        {
            return info;
        }
    }

    @javax.enterprise.context.ApplicationScoped
    public static class CacheConfigInfo
        extends AbstractAproxConfigInfo
    {
        public CacheConfigInfo()
        {
            super( CacheConfiguration.class );
        }
    }

    public static final String DEFAULT_PATH = "/etc/aprox/infinispan.xml";

    private String path;

    @ConfigName( "path" )
    public void setPath( final String path )
    {
        this.path = path;
    }

    public String getPath()
    {
        return path == null ? DEFAULT_PATH : path;
    }

}
