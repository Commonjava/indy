package org.commonjava.aprox.autoprox.conf;

import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.ConfigNames;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "repository" )
public class DefaultAutoRepoConfiguration
    implements AutoRepoConfiguration
{

    private final String baseUrl;

    private boolean passthroughEnabled = true;

    private Integer timeoutSeconds;

    private Integer cacheTimeoutSeconds;

    @ConfigNames( "base.url" )
    public DefaultAutoRepoConfiguration( final String baseUrl )
    {
        this.baseUrl = baseUrl;
    }

    @Override
    public boolean isPassthroughEnabled()
    {
        return passthroughEnabled;
    }

    @Override
    public Integer getTimeoutSeconds()
    {
        return timeoutSeconds;
    }

    @Override
    public Integer getCacheTimeoutSeconds()
    {
        return cacheTimeoutSeconds;
    }

    @ConfigName( "passthrough" )
    public void setPassthroughEnabled( final boolean passthrough )
    {
        this.passthroughEnabled = passthrough;
    }

    @ConfigName( "timeout.seconds" )
    public void setTimeoutSeconds( final int timeoutSeconds )
    {
        this.timeoutSeconds = timeoutSeconds;
    }

    @ConfigName( "cache.timeout.seconds" )
    public void setCacheTimeoutSeconds( final int cacheTimeoutSeconds )
    {
        this.cacheTimeoutSeconds = cacheTimeoutSeconds;
    }

    @Override
    public String getBaseUrl()
    {
        return baseUrl;
    }

}
