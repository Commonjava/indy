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
package org.commonjava.aprox.filer.def.conf;

import java.io.File;
import java.io.InputStream;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.conf.AbstractAproxConfigInfo;
import org.commonjava.aprox.conf.AbstractAproxFeatureConfig;
import org.commonjava.aprox.conf.AproxConfigClassInfo;
import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.aprox.inject.Production;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigNames;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "storage-default" )
@Alternative
@Named( "unused" )
public class DefaultStorageProviderConfiguration
{

    @javax.enterprise.context.ApplicationScoped
    public static class FilerDefaultFeatureConfig
        extends AbstractAproxFeatureConfig<DefaultStorageProviderConfiguration, DefaultStorageProviderConfiguration>
    {
        @Inject
        private FilerDefaultConfigInfo info;

        public FilerDefaultFeatureConfig()
        {
            super( DefaultStorageProviderConfiguration.class );
        }

        @Produces
        @Production
        @Default
        public DefaultStorageProviderConfiguration getFilerDefaultConfig()
            throws ConfigurationException
        {
            return getConfig();
        }

        @Override
        public AproxConfigClassInfo getInfo()
        {
            return info;
        }
    }

    @javax.enterprise.context.ApplicationScoped
    public static class FilerDefaultConfigInfo
        extends AbstractAproxConfigInfo
    {
        public FilerDefaultConfigInfo()
        {
            super( DefaultStorageProviderConfiguration.class );
        }

        @Override
        public String getDefaultConfigFileName()
        {
            return AproxConfigInfo.APPEND_DEFAULTS_TO_MAIN_CONF;
        }

        @Override
        public InputStream getDefaultConfig()
        {
            return Thread.currentThread()
                         .getContextClassLoader()
                         .getResourceAsStream( "default-storage.conf" );
        }
    }

    public static final File DEFAULT_BASEDIR = new File( "/var/lib/aprox/storage" );

    private File storageBasedir;

    public DefaultStorageProviderConfiguration()
    {
    }

    @ConfigNames( "storage.dir" )
    public DefaultStorageProviderConfiguration( final File storageBasedir )
    {
        this.storageBasedir = storageBasedir;
    }

    public File getStorageRootDirectory()
    {
        return storageBasedir == null ? DEFAULT_BASEDIR : storageBasedir;
    }

    public void setStorageRootDirectory( final File storageBasedir )
    {
        this.storageBasedir = storageBasedir;
    }

}
