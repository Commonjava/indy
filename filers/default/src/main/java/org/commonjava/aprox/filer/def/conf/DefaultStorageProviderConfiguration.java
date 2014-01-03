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
package org.commonjava.aprox.filer.def.conf;

import java.io.File;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.conf.AbstractAproxConfigInfo;
import org.commonjava.aprox.conf.AbstractAproxFeatureConfig;
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
        public AproxConfigInfo getInfo()
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
