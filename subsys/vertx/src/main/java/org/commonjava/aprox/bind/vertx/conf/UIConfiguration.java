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
package org.commonjava.aprox.bind.vertx.conf;

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

@SectionName( "ui" )
@Alternative
@Named( "unused" )
public class UIConfiguration
{

    @javax.enterprise.context.ApplicationScoped
    public static class UIFeatureConfig
        extends AbstractAproxFeatureConfig<UIConfiguration, UIConfiguration>
    {
        @Inject
        private UIConfigInfo info;

        public UIFeatureConfig()
        {
            super( UIConfiguration.class );
        }

        @Produces
        @Default
        public UIConfiguration getFlatFileConfig()
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
    public static class UIConfigInfo
        extends AbstractAproxConfigInfo
    {
        public UIConfigInfo()
        {
            super( UIConfiguration.class );
        }
    }

    public static final File DEFAULT_DIR = new File( System.getProperty( "aprox.home", "/var/lib/aprox" ), "ui" );

    private File uiDir;

    public UIConfiguration()
    {
    }

    @ConfigNames( "ui.dir" )
    public UIConfiguration( final File uiDir )
    {
        this.uiDir = uiDir;
    }

    public File getUIDir()
    {
        return uiDir == null ? DEFAULT_DIR : uiDir;
    }

    public void setUIDir( final File uiDir )
    {
        this.uiDir = uiDir;
    }

}
