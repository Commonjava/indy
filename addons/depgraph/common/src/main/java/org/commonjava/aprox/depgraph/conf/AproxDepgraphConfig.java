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

@SectionName( "depgraph" )
@Named( "use-factory-instead" )
@Alternative
public class AproxDepgraphConfig
{

    private static final int DEFAULT_TENSOR_DISCOVERY_TIMEOUT_MILLIS = 30000;

    private static final String DEFAULT_DB_DIRNAME = "depgraph";

    // shipping-oriented builds
    private static final String DEFAULT_DEF_WEBFILTER_PRESET = "sob";

    private Long discoveryTimeoutMillis;

    private String dbDir;

    private File dataBasedir;

    private String defaultWebFilterPreset = DEFAULT_DEF_WEBFILTER_PRESET;
    
    private boolean passiveParsingEnabled = false;

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

    public AproxDepgraphConfig setDataBasedir( final File basedir )
    {
        this.dataBasedir = new File( basedir, getDbDir() );
        return this;
    }

    private String getDbDir()
    {
        return dbDir == null ? DEFAULT_DB_DIRNAME : dbDir;
    }

    @ConfigName( "database.dirName" )
    public void setDbDir( final String dbDir )
    {
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
    public void setPassiveParsingEnabled( boolean passiveParsingEnabled )
    {
        this.passiveParsingEnabled = passiveParsingEnabled;
    }

}
