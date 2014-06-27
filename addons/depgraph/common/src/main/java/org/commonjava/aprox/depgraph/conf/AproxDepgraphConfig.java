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

    private static final String DEFAULT_DEF_WEBFILTER_PRESET = "build-requires";

    private File dataBasedir;

    private String defaultWebFilterPreset = DEFAULT_DEF_WEBFILTER_PRESET;
    
    private boolean passiveParsingEnabled = false;

    private File workBasedir;

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
        this.dataBasedir = new File( dataBasedir, "depgraph" );
        this.workBasedir = new File( workBasedir, "depgraph" );
        return this;
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

}
