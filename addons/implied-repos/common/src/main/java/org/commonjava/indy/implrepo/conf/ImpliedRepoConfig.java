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
package org.commonjava.indy.implrepo.conf;

import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.propulsor.config.ConfigurationException;
import org.commonjava.propulsor.config.annotation.SectionName;
import org.commonjava.propulsor.config.section.MapSectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.PatternSyntaxException;

@ApplicationScoped
@SectionName( ImpliedRepoConfig.SECTION_NAME)
public class ImpliedRepoConfig
        extends MapSectionListener
    implements IndyConfigInfo
{

    public static final String SECTION_NAME = "implied-repos";

    public static final String ENABLED_KEY = "enabled";

    public static final String ENABLE_GROUP_KEY = "enabled.group";

    public static final String INCLUDE_SNAPSHOTS_KEY = "include.snapshots";

    public static final String DISABLED_HOST_KEY = "disable";

    public static final boolean DEFAULT_INCLUDE_SNAPSHOT_REPOS = false;

    public static final boolean DEFAULT_ENABLED = false;

    private Boolean enabled;

    private List<String> enabledGroupNamePatterns;

    private Boolean includeSnapshotRepos;

    private List<String> blacklistedHosts = new ArrayList<>();

    public ImpliedRepoConfig()
    {
    }

    public boolean isEnabled()
    {
        return enabled == null ? DEFAULT_ENABLED : enabled;
    }

    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }

    public List<String> getEnabledGroupNamePatterns()
    {
        return enabledGroupNamePatterns;
    }

    public void setEnabledGroupNamePatterns( List<String> enabledGroupNamePatterns )
    {
        this.enabledGroupNamePatterns = enabledGroupNamePatterns;
    }

    public boolean isIncludeSnapshotRepos()
    {
        return includeSnapshotRepos == null ? DEFAULT_INCLUDE_SNAPSHOT_REPOS : includeSnapshotRepos;
    }

    public void setIncludeSnapshotRepos( final Boolean includeSnapshotRepos )
    {
        this.includeSnapshotRepos = includeSnapshotRepos;
    }
    
    public void addBlacklistedHost( final String host )
    {
        this.blacklistedHosts.add( host );
    }

    public synchronized void addEnabledGroupNamePattern( String groupName )
    {
        if ( this.enabledGroupNamePatterns == null )
        {
            this.enabledGroupNamePatterns = new ArrayList<>();
        }

        this.enabledGroupNamePatterns.add( groupName );
    }

    public boolean isEnabledForGroup( String name )
    {
        if ( enabledGroupNamePatterns == null || StringUtils.isEmpty( name ) )
        {
            return false;
        }

        Optional<String> found =
                enabledGroupNamePatterns.stream().filter( ( pattern ) -> name.matches( pattern ) ).findFirst();

        return found.isPresent();
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

        String hostAndPort = host + ":" + port;
        String hostAndPortAndProto = proto + "://" + hostAndPort;
        String hostAndProto = proto + "://" + host;
        String u = url.toString();

        if ( blacklistedHosts.contains( host ) || blacklistedHosts.contains( hostAndPort ) || blacklistedHosts.contains(
                hostAndPortAndProto ) || blacklistedHosts.contains( hostAndProto ) || blacklistedHosts.contains( u ) )
        {
            return true;
        }

        for ( String bl : blacklistedHosts )
        {
            try
            {
                if ( u.matches( bl ) )
                {
                    return true;
                }
            }
            catch ( PatternSyntaxException e )
            {
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.warn( "[BLACKLIST SKIP] Regex comparison failed on pattern: " + bl, e);
            }
        }

        return false;
    }

    public List<String> getBlacklistedHosts()
    {
        return blacklistedHosts;
    }

    public void setBlacklistedHosts( final List<String> blacklistedHosts )
    {
        this.blacklistedHosts = blacklistedHosts;
    }

    @Override
    public synchronized void parameter( final String name, final String value )
            throws ConfigurationException
    {
        switch ( name )
        {
            case ENABLED_KEY:
            {
                this.enabled = Boolean.parseBoolean( value );
                break;
            }
            case ENABLE_GROUP_KEY:
            {
                if ( enabledGroupNamePatterns == null )
                {
                    enabledGroupNamePatterns = new ArrayList<>();
                }

                this.enabledGroupNamePatterns.add( value );
                break;
            }
            case INCLUDE_SNAPSHOTS_KEY:
            {
                this.includeSnapshotRepos = Boolean.parseBoolean( value );
                break;
            }
            case DISABLED_HOST_KEY:
            {
                this.blacklistedHosts.add( value );
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

//    @javax.enterprise.context.ApplicationScoped
//    public static class FeatureConfig
//        extends AbstractIndyFeatureConfig<ImpliedRepoConfig, ImpliedRepoConfig>
//    {
//        public FeatureConfig()
//        {
//            super( new ImpliedRepoConfig() );
//        }
//
//        @Produces
//        @Default
//        @ApplicationScoped
//        public ImpliedRepoConfig getImpliedRepoConfig()
//            throws ConfigurationException
//        {
//            return getPrefabInstance();
//        }
//
//        @Override
//        public IndyConfigClassInfo getInfo()
//        {
//            return getPrefabInstance();
//        }
//    }

    @Override
    public String getDefaultConfigFileName()
    {
        return new File( IndyConfigInfo.CONF_INCLUDES_DIR, "implied-repos.conf" ).getPath();
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread()
                     .getContextClassLoader()
                     .getResourceAsStream( "default-implied-repos.conf" );
    }

    //    @Override
    //    public Class<?> getConfigurationClass()
    //    {
    //        return getClass();
    //    }

    public boolean isBlacklisted( final String url )
        throws MalformedURLException
    {
        final URL u = new URL( url );
        return isBlacklisted( u );
    }
}
