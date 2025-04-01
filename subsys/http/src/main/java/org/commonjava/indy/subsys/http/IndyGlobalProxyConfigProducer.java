/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.subsys.http;

import org.commonjava.indy.subsys.http.util.IndySiteConfigLookup;
import org.commonjava.maven.galley.transport.htcli.conf.GlobalProxyConfig;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.commonjava.indy.subsys.http.conf.IndyHttpConfig.DEFAULT_SITE;

@ApplicationScoped
public class IndyGlobalProxyConfigProducer
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private IndySiteConfigLookup siteConfigLookup;

    private GlobalProxyConfig globalProxyConfig;

    private SiteConfig defaultSiteConfig;

    @Produces
    public GlobalProxyConfig getGlobalProxyConfig()
    {
        defaultSiteConfig = siteConfigLookup.lookup( DEFAULT_SITE );
        if ( defaultSiteConfig != null && defaultSiteConfig.getProxyHost() != null )
        {
            setupGlobalProxyConfig();
        }
        return globalProxyConfig;
    }

    private void setupGlobalProxyConfig()
    {
        logger.debug( "Setup global proxy config, host: {}", defaultSiteConfig.getProxyHost() );
        globalProxyConfig = new GlobalProxyConfig()
        {
            @Override
            public String getHost()
            {
                return defaultSiteConfig.getProxyHost();
            }

            @Override
            public int getPort()
            {
                return defaultSiteConfig.getProxyPort();
            }

            @Override
            public String getUser()
            {
                return defaultSiteConfig.getProxyUser();
            }

            @Override
            public List<String> getAllowHttpJobTypes()
            {
                return getList( defaultSiteConfig.getProxyAllowHttpJobTypes() );
            }

            @Override
            public List<String> getEgressSites()
            {
                return getList( defaultSiteConfig.getEgressSites() );
            }

            @Override
            public String toString()
            {
                return String.format( "GlobalProxyConfig [host=%s, port=%s, allowHttpJobTypes=%s]", getHost(),
                                      getPort(), defaultSiteConfig.getProxyAllowHttpJobTypes() );
            }
        };
        logger.debug( "Global proxy config produced: {}", globalProxyConfig );
    }

    private List<String> getList( String value )
    {
        final List<String> list = new ArrayList<>();
        if ( isNotBlank( value ) )
        {
            String[] toks = value.split( "," );
            for ( String s : toks )
            {
                s = s.trim();
                if ( isNotBlank( s ) )
                {
                    list.add( s );
                }
            }
        }
        return list;
    }
}
