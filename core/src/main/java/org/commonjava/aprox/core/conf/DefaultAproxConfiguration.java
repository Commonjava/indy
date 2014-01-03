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
package org.commonjava.aprox.core.conf;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.conf.AbstractAproxConfigInfo;
import org.commonjava.aprox.conf.AbstractAproxFeatureConfig;
import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.aprox.conf.AproxConfiguration;
import org.commonjava.aprox.inject.Production;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;
import org.commonjava.web.config.section.ConfigurationSectionListener;

@SectionName( ConfigurationSectionListener.DEFAULT_SECTION )
@Alternative
@Named( "not-used-directly" )
public class DefaultAproxConfiguration
    implements AproxConfiguration
{

    @javax.enterprise.context.ApplicationScoped
    public static class FeatureConfig
        extends AbstractAproxFeatureConfig<AproxConfiguration, DefaultAproxConfiguration>
    {
        @Inject
        private ConfigInfo info;

        public FeatureConfig()
        {
            super( DefaultAproxConfiguration.class );
        }

        @Produces
        @Production
        @Default
        public AproxConfiguration getAproxConfig()
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
    public static class ConfigInfo
        extends AbstractAproxConfigInfo
    {
        public ConfigInfo()
        {
            super( DefaultAproxConfiguration.class );
        }
    }

    public static final int DEFAULT_PASSTHROUGH_TIMEOUT_SECONDS = 300;

    public static final int DEFAULT_NOT_FOUND_CACHE_TIMEOUT_SECONDS = 3600;

    private Integer passthroughTimeoutSeconds;

    private Integer notFoundCacheTimeoutSeconds;

    public DefaultAproxConfiguration()
    {
    }

    @Override
    public int getPassthroughTimeoutSeconds()
    {
        return passthroughTimeoutSeconds == null ? DEFAULT_PASSTHROUGH_TIMEOUT_SECONDS : passthroughTimeoutSeconds;
    }

    @ConfigName( "passthrough.timeout" )
    public void setPassthroughTimeoutSeconds( final int seconds )
    {
        passthroughTimeoutSeconds = seconds;
    }

    @ConfigName( "nfc.timeout" )
    public void setNotFoundCacheTimeoutSeconds( final int seconds )
    {
        notFoundCacheTimeoutSeconds = seconds;
    }

    @Override
    public int getNotFoundCacheTimeoutSeconds()
    {
        return notFoundCacheTimeoutSeconds == null ? DEFAULT_NOT_FOUND_CACHE_TIMEOUT_SECONDS
                        : notFoundCacheTimeoutSeconds;
    }

}
