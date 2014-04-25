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
package org.commonjava.aprox.depgraph.conf;

import java.io.File;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;

import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SectionName( "depgraph" )
@Named( "use-factory-instead" )
@Alternative
public class AproxDepgraphConfig
{

    private static final int DEFAULT_TENSOR_DISCOVERY_TIMEOUT_MILLIS = 30000;

    private static final String DEFAULT_DB_DIRNAME = "depgraph";

    private static final String DEFAULT_WORK_DIRNAME = "depgraph";

    // shipping-oriented builds
    private static final String DEFAULT_DEF_WEBFILTER_PRESET = "sob";


    private Long discoveryTimeoutMillis;

    private String dbDir;

    private File dataBasedir;

    private String defaultWebFilterPreset = DEFAULT_DEF_WEBFILTER_PRESET;
    
    private boolean passiveParsingEnabled = false;

    private String workDir;

    private File workBasedir;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public long getDiscoveryTimeoutMillis()
    {
        return discoveryTimeoutMillis == null ? DEFAULT_TENSOR_DISCOVERY_TIMEOUT_MILLIS : discoveryTimeoutMillis;
    }

    @ConfigName( "discoveryTimeoutMillis" )
    public void setDiscoveryTimeoutMillis( final long discoveryTimeoutMillis )
    {
        this.discoveryTimeoutMillis = discoveryTimeoutMillis;
    }

    public File getDataBasedir()
    {
        return dataBasedir;
    }

    public File getWorkBasedir()
    {
        return workBasedir;
    }

    public AproxDepgraphConfig setDirectories( final File dataBasedir, final File workBasedir )
    {
        logger.info( "Setting depgraph data basedir {}/{}", dataBasedir, getDbDir() );
        this.dataBasedir = new File( dataBasedir, getDbDir() );

        logger.info( "Setting depgraph work basedir {}/{}", workBasedir, getWorkDir() );
        this.workBasedir = new File( workBasedir, getWorkDir() );
        return this;
    }

    private String getDbDir()
    {
        return dbDir == null ? DEFAULT_DB_DIRNAME : dbDir;
    }

    @ConfigName( "database.dirName" )
    public void setDbDir( final String dbDir )
    {
        if ( dbDir == null || "null".equals( dbDir ) )
        {
            return;
        }

        this.dbDir = dbDir;
    }

    public String getDefaultWebFilterPreset()
    {
        return defaultWebFilterPreset;
    }

    @ConfigName( "default.webfilter.preset" )
    public void setDefaultWebFilterPreset( final String preset )
    {
        this.defaultWebFilterPreset = preset;
    }

    public boolean isPassiveParsingEnabled()
    {
        return passiveParsingEnabled;
    }

    @ConfigName( "passive.parsing" )
    public void setPassiveParsingEnabled( final boolean passiveParsingEnabled )
    {
        this.passiveParsingEnabled = passiveParsingEnabled;
    }

    public String getWorkDir()
    {
        return workDir == null ? DEFAULT_WORK_DIRNAME : workDir;
    }

    @ConfigName( "work.dirName" )
    public void setWorkDir( final String workDir )
    {
        if ( workDir == null || "null".equals( workDir ) )
        {
            return;
        }

        this.workDir = workDir;
    }

}
