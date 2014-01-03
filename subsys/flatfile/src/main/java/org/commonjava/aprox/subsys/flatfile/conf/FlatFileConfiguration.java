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
package org.commonjava.aprox.subsys.flatfile.conf;

import java.io.File;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.conf.AbstractAproxConfigInfo;
import org.commonjava.aprox.conf.AbstractAproxFeatureConfig;
import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigNames;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "flatfiles" )
@Alternative
@Named( "unused" )
public class FlatFileConfiguration
{

    @javax.enterprise.context.ApplicationScoped
    public static class FlatFileFeatureConfig
        extends AbstractAproxFeatureConfig<FlatFileConfiguration, FlatFileConfiguration>
    {
        @Inject
        private FlatFileConfigInfo info;

        public FlatFileFeatureConfig()
        {
            super( FlatFileConfiguration.class );
        }

        @Produces
        @Default
        public FlatFileConfiguration getFlatFileConfig()
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
    public static class FlatFileConfigInfo
        extends AbstractAproxConfigInfo
    {
        public FlatFileConfigInfo()
        {
            super( FlatFileConfiguration.class );
        }
    }

    public static final File DEFAULT_BASEDIR = new File( "/var/lib/aprox/db" );

    private File dataBasedir;

    public FlatFileConfiguration()
    {
    }

    @ConfigNames( "data.dir" )
    public FlatFileConfiguration( final File dataBasedir )
    {
        this.dataBasedir = dataBasedir;
    }

    public File getDataBasedir()
    {
        return dataBasedir == null ? DEFAULT_BASEDIR : dataBasedir;
    }

    public void setDataBasedir( final File dataBasedir )
    {
        this.dataBasedir = dataBasedir;
    }

    public File getStorageDir( final String name )
    {
        final File d = new File( getDataBasedir(), name );
        d.mkdirs();

        return d;
    }

}
