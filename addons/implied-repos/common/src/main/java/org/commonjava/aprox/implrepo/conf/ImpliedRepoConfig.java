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
package org.commonjava.aprox.implrepo.conf;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.commonjava.aprox.conf.AbstractAproxFeatureConfig;
import org.commonjava.aprox.conf.AbstractAproxMapConfig;
import org.commonjava.aprox.conf.AproxConfigClassInfo;
import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( ImpliedRepoConfig.SECTION_NAME )
@Alternative
@Named
public class ImpliedRepoConfig
    extends AbstractAproxMapConfig
    implements AproxConfigClassInfo
{

    public static final String SECTION_NAME = "implied-repos";

    public static final String ENABLED_KEY = "enabled";

    public static final String INCLUDE_SNAPSHOTS_KEY = "include.snapshots";

    public static final String DISABLED_HOST_KEY = "disable";

    public static final boolean DEFAULT_ENABLED = false;

    public static final boolean DEFAULT_INCLUDE_SNAPSHOT_REPOS = false;

    private Boolean enabled;

    private Boolean includeSnapshotRepos;

    private List<String> blacklist = new ArrayList<>();

    public ImpliedRepoConfig()
    {
    }

    public boolean isEnabled()
    {
        return enabled == null ? DEFAULT_ENABLED : enabled;
    }

    public void setEnabled( final Boolean enabled )
    {
        this.enabled = enabled;
    }

    public boolean isIncludeSnapshotRepos()
    {
        return includeSnapshotRepos == null ? DEFAULT_INCLUDE_SNAPSHOT_REPOS : includeSnapshotRepos;
    }

    public void setIncludeSnapshotRepos( final Boolean includeSnapshotRepos )
    {
        this.includeSnapshotRepos = includeSnapshotRepos;
    }
    
    public void addBlacklist( final String host )
    {
        this.blacklist.add( host );
    }

    public boolean isBlacklisted( final URL url )
    {
        final String proto = url.getProtocol();
        final String host = url.getHost();
        int port = url.getPort();
        if ( port < 0 )
        {
            port = "https".equals( proto ) ? 443 : 80;
        }

        return blacklist.contains( host ) || blacklist.contains( host + ":" + port )
            || blacklist.contains( proto + "://" + host ) || blacklist.contains( proto + "://" + host + ":" + port );
    }

    public List<String> getBlacklist()
    {
        return blacklist;
    }

    public void setBlacklist( final List<String> blacklist )
    {
        this.blacklist = blacklist;
    }

    @Override
    public void parameter( final String name, final String value )
        throws ConfigurationException
    {
        switch ( name )
        {
            case ENABLED_KEY:
            {
                this.enabled = Boolean.parseBoolean( value );
                break;
            }
            case INCLUDE_SNAPSHOTS_KEY:
            {
                this.includeSnapshotRepos = Boolean.parseBoolean( value );
                break;
            }
            case DISABLED_HOST_KEY:
            {
                this.blacklist.add( value );
                break;
            }
            default:
            {
                throw new ConfigurationException(
                                                  "Invalid value: '{}' for parameter: '{}'. Only numeric values are accepted for section: '{}'.",
                                                  value, name, SECTION_NAME );
            }
        }
    }

    @javax.enterprise.context.ApplicationScoped
    public static class FeatureConfig
        extends AbstractAproxFeatureConfig<ImpliedRepoConfig, ImpliedRepoConfig>
    {
        public FeatureConfig()
        {
            super( new ImpliedRepoConfig() );
        }

        @Produces
        @Default
        @ApplicationScoped
        public ImpliedRepoConfig getImpliedRepoConfig()
            throws ConfigurationException
        {
            return getPrefabInstance();
        }

        @Override
        public AproxConfigClassInfo getInfo()
        {
            return getPrefabInstance();
        }
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return new File( AproxConfigInfo.CONF_INCLUDES_DIR, "implied-repos.conf" ).getPath();
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread()
                     .getContextClassLoader()
                     .getResourceAsStream( "default-implied-repos.conf" );
    }

    @Override
    public Class<?> getConfigurationClass()
    {
        return getClass();
    }

    public boolean isBlacklisted( final String url )
        throws MalformedURLException
    {
        final URL u = new URL( url );
        return isBlacklisted( u );
    }

}
