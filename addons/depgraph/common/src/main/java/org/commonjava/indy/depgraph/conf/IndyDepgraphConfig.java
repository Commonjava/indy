/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.depgraph.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.ConfigurationListener;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;
import org.commonjava.web.config.section.ConfigurationSectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

@SectionName( "depgraph" )
@ApplicationScoped
public class IndyDepgraphConfig
    implements IndyConfigInfo
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

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

    public IndyDepgraphConfig setDirectories( final File dataBasedir, final File workBasedir )
    {
        this.dataBasedir = new File( dataBasedir, "depgraph" );
        this.workBasedir = new File( workBasedir, "depgraph" );

        logger.debug( "Set depgraph-data-basedir to: {}\nSet depgraph-work-basedir to: {}", dataBasedir, workBasedir );
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

    @Override
    public String getDefaultConfigFileName()
    {
        return new File( IndyConfigInfo.CONF_INCLUDES_DIR, "depgraph.conf" ).getPath();
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread()
                     .getContextClassLoader()
                     .getResourceAsStream( "default-depgraph.conf" );
    }

}
