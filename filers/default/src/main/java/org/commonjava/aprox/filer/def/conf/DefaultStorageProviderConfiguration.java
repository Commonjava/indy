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
