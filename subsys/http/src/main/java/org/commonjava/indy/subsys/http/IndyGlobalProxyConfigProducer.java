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
        final String allowTypes = defaultSiteConfig.getProxyAllowHttpJobTypes();
        final List<String> list = new ArrayList<>();
        if ( isNotBlank( allowTypes ) )
        {
            String[] toks = allowTypes.split( "," );
            for ( String s : toks )
            {
                s = s.trim();
                if ( isNotBlank( s ) )
                {
                    list.add( s );
                }
            }
        }
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
                return list;
            }
        };
    }
}
