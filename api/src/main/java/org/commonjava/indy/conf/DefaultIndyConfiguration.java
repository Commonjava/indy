/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.conf;

import java.io.InputStream;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.indy.inject.Production;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;
import org.commonjava.web.config.section.ConfigurationSectionListener;

@SectionName( ConfigurationSectionListener.DEFAULT_SECTION )
@Alternative
@Named( "not-used-directly" )
public class DefaultIndyConfiguration
    implements IndyConfiguration
{

    @javax.enterprise.context.ApplicationScoped
    public static class FeatureConfig
        extends AbstractIndyFeatureConfig<IndyConfiguration, DefaultIndyConfiguration>
    {
        @Inject
        private ConfigInfo info;

        public FeatureConfig()
        {
            super( DefaultIndyConfiguration.class );
        }

        @Produces
        @Production
        @Default
        public IndyConfiguration getIndyConfig()
            throws ConfigurationException
        {
            return getConfig();
        }

        @Override
        public IndyConfigClassInfo getInfo()
        {
            return info;
        }
    }

    @javax.enterprise.context.ApplicationScoped
    public static class ConfigInfo
        extends AbstractIndyConfigInfo
    {
        public ConfigInfo()
        {
            super( DefaultIndyConfiguration.class );
        }

        @Override
        public String getDefaultConfigFileName()
        {
            return IndyConfigInfo.APPEND_DEFAULTS_TO_MAIN_CONF;
        }

        @Override
        public InputStream getDefaultConfig()
        {
            return Thread.currentThread()
                         .getContextClassLoader()
                         .getResourceAsStream( "default-main.conf" );
        }
    }

    public static final int DEFAULT_PASSTHROUGH_TIMEOUT_SECONDS = 300;

    public static final int DEFAULT_NOT_FOUND_CACHE_TIMEOUT_SECONDS = 300;

    public static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 5;

    public static final int DEFAULT_STORE_DISABLE_TIMEOUT_SECONDS = 1800; // 30 minutes

    private Integer passthroughTimeoutSeconds;

    private Integer notFoundCacheTimeoutSeconds;

    private Integer requestTimeoutSeconds;

    private Integer storeDisableTimeoutSeconds;

    public DefaultIndyConfiguration()
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
        return notFoundCacheTimeoutSeconds == null ? DEFAULT_NOT_FOUND_CACHE_TIMEOUT_SECONDS : notFoundCacheTimeoutSeconds;
    }

    @Override
    public int getRequestTimeoutSeconds()
    {
        return requestTimeoutSeconds == null ? DEFAULT_REQUEST_TIMEOUT_SECONDS : requestTimeoutSeconds;
    }

    @ConfigName( "request.timeout" )
    public void setRequestTimeoutSeconds( final Integer requestTimeoutSeconds )
    {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    @Override
    public int getStoreDisableTimeoutSeconds()
    {
        return storeDisableTimeoutSeconds == null ? DEFAULT_STORE_DISABLE_TIMEOUT_SECONDS : storeDisableTimeoutSeconds;
    }

    @ConfigName( "store.disable.timeout" )
    public void setStoreDisableTimeoutSeconds( final Integer storeDisableTimeoutSeconds )
    {
        this.storeDisableTimeoutSeconds = storeDisableTimeoutSeconds;
    }
}
