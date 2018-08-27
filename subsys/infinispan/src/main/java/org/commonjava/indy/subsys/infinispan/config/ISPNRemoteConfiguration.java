/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.subsys.infinispan.config;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@SectionName( "infinispan-remote" )
@ApplicationScoped
public class ISPNRemoteConfiguration
        implements IndyConfigInfo
{
    private static final String DEFAULT_REMOTE_SERVER = "localhost";

    private static final Boolean DEFAULT_ENABLED = Boolean.FALSE;

    private Boolean enabled;

    private String remoteServer;

    private Integer hotrodPort;

    private String remotePatterns;

    public ISPNRemoteConfiguration()
    {
    }

    private List<String> remotePatternsList;

    @PostConstruct
    private void init()
    {
        if ( isNotBlank( remotePatterns ) )
        {
            remotePatternsList = new ArrayList<>();
            String[] patterns = remotePatterns.split( "," );
            for ( String pattern : patterns )
            {
                remotePatternsList.add( pattern );
            }
        }
    }

    public Boolean isEnabled()
    {
        return enabled == null ? DEFAULT_ENABLED : enabled;
    }

    @ConfigName( "enabled" )
    public void setEnabled( Boolean enabled )
    {
        this.enabled = enabled;
    }

    public String getRemoteServer()
    {
        return remoteServer == null ? DEFAULT_REMOTE_SERVER : remoteServer;
    }

    @ConfigName( "remote.server" )
    public void setRemoteServer( String remoteServer )
    {
        this.remoteServer = remoteServer;
    }

    public Integer getHotrodPort()
    {
        return hotrodPort == null ? ConfigurationProperties.DEFAULT_HOTROD_PORT : hotrodPort;
    }

    @ConfigName( "hotrod.port" )
    public void setHotrodPort( Integer hotrodPort )
    {
        this.hotrodPort = hotrodPort;
    }

    public String getRemotePatterns()
    {
        return remotePatterns;
    }

    @ConfigName( "remote.patterns" )
    public void setRemotePatterns( String remotePatterns )
    {
        this.remotePatterns = remotePatterns;
    }

    // utils

    public boolean isRemoteCache( String cacheName )
    {
        if ( remotePatternsList == null )
        {
            return false;
        }
        for ( String pattern : remotePatternsList )
        {
            if ( cacheName.matches( pattern ) || cacheName.equals( pattern ) )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return new File( IndyConfigInfo.CONF_INCLUDES_DIR, "infinispan-remote.conf" ).getPath();
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread()
                     .getContextClassLoader()
                     .getResourceAsStream( "default-infinispan-remote.conf" );
    }
}
