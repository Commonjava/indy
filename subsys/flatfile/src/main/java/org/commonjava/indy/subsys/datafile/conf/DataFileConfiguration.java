/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.subsys.datafile.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.indy.conf.SystemPropertyProvider;
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;
import java.util.Properties;

@SectionName( DataFileConfiguration.SECTION_NAME )
@ApplicationScoped
public class DataFileConfiguration
    implements IndyConfigInfo, SystemPropertyProvider
{

    public static final String SECTION_NAME = "flatfiles";

    private static final String INDY_WORK_BASEDIR = "indy.work";

    private static final String INDY_DATA_BASEDIR = "indy.data";

    public Properties getSystemPropertyAdditions()
    {
        Properties properties = new Properties();
        properties.setProperty( INDY_DATA_BASEDIR, getDataBasedir().getAbsolutePath() );
        properties.setProperty( INDY_WORK_BASEDIR, getWorkBasedir().getAbsolutePath() );

        return properties;
    }

    public static final String DEFAULT_ROOT_DIR = "/var/lib/indy";

    public static final String DEFAULT_DATA_SUBDIR = "data";

    public static final String DEFAULT_WORK_SUBDIR = "work";

    private File getDefaultDataBasedir()
    {
        return new File( System.getProperty( "indy.home", DEFAULT_ROOT_DIR ),
                DEFAULT_DATA_SUBDIR );
    }

    private File getDefaultWorkBasedir()
    {
        return new File( System.getProperty( "indy.home", DEFAULT_ROOT_DIR ),
                DEFAULT_WORK_SUBDIR );
    }

    private File dataBasedir;

    private File workBasedir;

    public DataFileConfiguration()
    {
    }

    public DataFileConfiguration( final File rootDir )
    {
        this.dataBasedir = new File( rootDir, DEFAULT_DATA_SUBDIR );
        this.workBasedir = new File( rootDir, DEFAULT_WORK_SUBDIR );
    }

    public DataFileConfiguration( final File dataDir, final File workDir )
    {
        this.dataBasedir = dataDir;
        this.workBasedir = workDir;
    }

    public File getDataBasedir()
    {
        return dataBasedir == null ? getDefaultDataBasedir() : dataBasedir;
    }

    @ConfigName( "data.dir" )
    public void setDataBasedir( final File dataBasedir )
    {
        this.dataBasedir = dataBasedir;
    }

    public DataFileConfiguration withDataBasedir( final File dataBasedir )
    {
        this.dataBasedir = dataBasedir;
        return this;
    }

    public File getDataDir( final String name )
    {
        final File d = new File( getDataBasedir(), name );
        d.mkdirs();

        return d;
    }

    public File getWorkBasedir()
    {
        return workBasedir == null ? getDefaultWorkBasedir() : workBasedir;
    }

    @ConfigName( "work.dir" )
    public void setWorkBasedir( final File workBasedir )
    {
        this.workBasedir = workBasedir;
    }

    public DataFileConfiguration withWorkBasedir( final File workBasedir )
    {
        this.workBasedir = workBasedir;
        return this;
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
                     .getResourceAsStream( "default-flatfiles.conf" );
    }

}
