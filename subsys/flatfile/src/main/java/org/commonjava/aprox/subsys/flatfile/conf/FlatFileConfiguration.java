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
package org.commonjava.aprox.subsys.flatfile.conf;

import java.io.File;

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
        @ApplicationScoped
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

    public static final File DEFAULT_DATA_BASEDIR = new File( System.getProperty( "aprox.home", "/var/lib/aprox" ),
                                                              "data" );

    private static final File DEFAULT_WORK_BASEDIR = new File( System.getProperty( "aprox.home", "/var/lib/aprox" ),
                                                               "work" );

    private File dataBasedir;

    private File workBasedir;

    public FlatFileConfiguration()
    {
    }

    public File getDataBasedir()
    {
        return dataBasedir == null ? DEFAULT_DATA_BASEDIR : dataBasedir;
    }

    @ConfigName( "data.dir" )
    public void setDataBasedir( final File dataBasedir )
    {
        this.dataBasedir = dataBasedir;
    }

    public FlatFileConfiguration withDataBasedir( final File dataBasedir )
    {
        this.dataBasedir = dataBasedir;
        return this;
    }

    public File getStorageDir( final String name )
    {
        final File d = new File( getDataBasedir(), name );
        d.mkdirs();

        return d;
    }

    public File getWorkBasedir()
    {
        return workBasedir == null ? DEFAULT_WORK_BASEDIR : workBasedir;
    }

    @ConfigName( "work.dir" )
    public void setWorkBasedir( final File workBasedir )
    {
        this.workBasedir = workBasedir;
    }

    public FlatFileConfiguration withWorkBasedir( final File workBasedir )
    {
        this.workBasedir = workBasedir;
        return this;
    }

}
