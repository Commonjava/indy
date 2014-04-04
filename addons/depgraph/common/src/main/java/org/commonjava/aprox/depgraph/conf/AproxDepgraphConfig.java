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
