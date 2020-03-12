/**
 * Copyright (C) 2013 Red Hat, Inc.
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
package org.commonjava.indy.repo.proxy.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.indy.repo.proxy.RepoProxyAddon;
import org.commonjava.propulsor.config.ConfigurationException;
import org.commonjava.propulsor.config.annotation.SectionName;
import org.commonjava.propulsor.config.section.MapSectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.commonjava.indy.repo.proxy.RepoProxyAddon.ADDON_NAME;

@SectionName( RepoProxyConfig.SECTION )
@ApplicationScoped
public class RepoProxyConfig
        extends MapSectionListener
        implements IndyConfigInfo
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String DEFAULT_CONFIG_FILE_NAME = "default-repo-proxy.conf";

    public static final String SECTION = "repo-proxy";

    private static final String ENABLED_PARAM = "enabled";

    private static final String API_PATTERNS_PARAM = "api.url.patterns";

    private static final String API_METHODS_PARAM = "api.methods";

    private static final String NPM_META_REWRITE_ENABLE_PARAM = "npm.meta.rewrite.enabled";

    private static final Boolean DEFAULT_ENABLED = Boolean.FALSE;

    private static final String DEFAULT_API_PATTERNS = "/api/content/*, /api/folo/track/*";

    private static final String DEFAULT_API_METHODS = "GET,HEAD";

    private static final String PROXY_KEY_PREFIX = "proxy.";

    private static final Boolean DEFAULT_NPM_META_REWRITE_ENABLE = Boolean.TRUE;

    private final Map<String, String> proxyRules = new LinkedHashMap<>();

    private final Set<String> apiPatterns = new HashSet<>();

    private final Set<String> apiMethods = new HashSet<>();

    private Boolean enabled;

    private Boolean npmMetaRewriteEnabled;

    public RepoProxyConfig()
    {
    }

    public boolean getEnabled()
    {
        return this.enabled == null ? DEFAULT_ENABLED : this.enabled;
    }

    public boolean isEnabled()
    {
        return getEnabled();
    }

    public void setEnabled( Boolean enabled )
    {
        this.enabled = enabled;
    }

    public Boolean getNpmMetaRewriteEnabled()
    {
        return this.npmMetaRewriteEnabled == null ? DEFAULT_NPM_META_REWRITE_ENABLE : this.npmMetaRewriteEnabled;
    }

    public Boolean isNpmMetaRewriteEnabled()
    {
        return getNpmMetaRewriteEnabled();
    }

    public Map<String, String> getProxyRules()
    {
        return proxyRules;
    }

    public Set<String> getApiPatterns()
    {
        if ( apiPatterns.isEmpty() )
        {
            for ( String pattern : DEFAULT_API_PATTERNS.split( "," ) )
            {
                apiPatterns.add( pattern.trim() );
            }
        }
        return apiPatterns;
    }

    public Set<String> getApiMethods()
    {
        if ( apiMethods.isEmpty() )
        {
            for ( String method : DEFAULT_API_METHODS.split( "," ) )
            {
                apiMethods.add( method.trim().toUpperCase() );
            }
        }
        return apiMethods;
    }

    @Override
    public void parameter( final String name, final String value )
            throws ConfigurationException
    {
        logger.trace( "{}: config tracing: name({})-value({}) ", ADDON_NAME, name, value );
        switch ( name )
        {
            case ENABLED_PARAM:
                this.enabled = Boolean.valueOf( value.trim() );
                break;
            case API_PATTERNS_PARAM:
                apiPatterns.clear();
                for ( String pattern : value.split( "," ) )
                {
                    apiPatterns.add( pattern.trim() );
                }
                break;
            case API_METHODS_PARAM:
                apiMethods.clear();
                for ( String method : value.split( "," ) )
                {
                    apiMethods.add( method.trim().toUpperCase() );
                }
                break;
            case NPM_META_REWRITE_ENABLE_PARAM:
                this.npmMetaRewriteEnabled = Boolean.valueOf( value.trim() );
                break;
            default:
            {
                if ( name.startsWith( PROXY_KEY_PREFIX ) && name.length() > PROXY_KEY_PREFIX.length() )
                {
                    String source = name.substring( PROXY_KEY_PREFIX.length() );
                    String sourceRepoPath = source.replaceAll( "\\.", "/" );
                    logger.trace( "{}: Repo {} proxy target is repo {}", ADDON_NAME, source, value );
                    proxyRules.put( sourceRepoPath, value.replaceAll( ":", "/" ) );
                }
                else
                {
                    throw new ConfigurationException( "Invalid parameter: '%s'.", value, name, SECTION );
                }
            }
        }
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return new File( IndyConfigInfo.CONF_INCLUDES_DIR, DEFAULT_CONFIG_FILE_NAME ).getPath();
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream( DEFAULT_CONFIG_FILE_NAME );
    }
}
