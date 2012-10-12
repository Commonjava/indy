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
package org.commonjava.aprox.sec.conf;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.aprox.conf.AproxFeatureConfig;
import org.commonjava.aprox.inject.Production;
import org.commonjava.badgr.conf.AdminConfiguration;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "admin" )
@Named( "use-factory-instead" )
@Alternative
public class DefaultAdminConfiguration
    implements AdminConfiguration
{

    @Singleton
    public static final class DefaultAdminConfigFeature
        extends AproxFeatureConfig<AdminConfiguration, DefaultAdminConfiguration>
    {
        @Inject
        private DefaultAdminConfigInfo info;

        public DefaultAdminConfigFeature()
        {
            super( DefaultAdminConfiguration.class );
        }

        @Produces
        @Production
        @Default
        public AdminConfiguration getCacheConfig()
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

    @Singleton
    public static final class DefaultAdminConfigInfo
        extends AproxConfigInfo
    {
        public DefaultAdminConfigInfo()
        {
            super( DefaultAdminConfiguration.class );
        }
    }

    private String initialFirstName;

    private String initialLastName;

    private String initialEmail;

    private String initialPassword;

    @Override
    public final String getInitialFirstName()
    {
        return initialFirstName == null ? DEFAULT_INITIAL_FIRST_NAME : initialFirstName;
    }

    @Override
    public final String getInitialLastName()
    {
        return initialLastName == null ? DEFAULT_INITIAL_LAST_NAME : initialLastName;
    }

    @Override
    public final String getInitialEmail()
    {
        return initialEmail == null ? DEFAULT_INITIAL_EMAIL : initialEmail;
    }

    @Override
    public final String getInitialPassword()
    {
        return initialPassword == null ? DEFAULT_INITIAL_PASSWORD : initialPassword;
    }

    public final void setInitialFirstName( final String initialFirstName )
    {
        this.initialFirstName = initialFirstName;
    }

    public final void setInitialLastName( final String initialLastName )
    {
        this.initialLastName = initialLastName;
    }

    public final void setInitialEmail( final String initialEmail )
    {
        this.initialEmail = initialEmail;
    }

    public final void setInitialPassword( final String initialPassword )
    {
        this.initialPassword = initialPassword;
    }

}
