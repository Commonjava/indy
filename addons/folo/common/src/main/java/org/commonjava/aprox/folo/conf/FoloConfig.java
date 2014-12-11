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
package org.commonjava.aprox.folo.conf;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.conf.AbstractAproxConfigInfo;
import org.commonjava.aprox.conf.AbstractAproxFeatureConfig;
import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "folo" )
@Alternative
@Named
public class FoloConfig
{

    @javax.enterprise.context.ApplicationScoped
    public static class ConfigInfo
        extends AbstractAproxConfigInfo
    {
        public ConfigInfo()
        {
            super( FoloConfig.class );
        }
    }

    public static final int DEFAULT_CACHE_TIMEOUT_SECONDS = 120;

    private Integer cacheTimeoutSeconds;

    public FoloConfig()
    {
    }

    public FoloConfig( final Integer cacheTimeoutSeconds )
    {
    }

    public Integer getCacheTimeoutSeconds()
    {
        return cacheTimeoutSeconds == null ? DEFAULT_CACHE_TIMEOUT_SECONDS : cacheTimeoutSeconds;
    }

    @ConfigName( "cache.timeout.seconds" )
    public void setCacheTimeoutSeconds( final Integer cacheTimeoutSeconds )
    {
        this.cacheTimeoutSeconds = cacheTimeoutSeconds;
    }

    @javax.enterprise.context.ApplicationScoped
    public static class FeatureConfig
        extends AbstractAproxFeatureConfig<FoloConfig, FoloConfig>
    {
        @Inject
        private ConfigInfo info;

        public FeatureConfig()
        {
            super( FoloConfig.class );
        }

        @Produces
        @Default
        @ApplicationScoped
        public FoloConfig getFlatFileConfig()
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

}
