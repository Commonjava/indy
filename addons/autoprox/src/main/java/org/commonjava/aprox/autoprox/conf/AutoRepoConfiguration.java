package org.commonjava.aprox.autoprox.conf;

public interface AutoRepoConfiguration
{

    /**
     * @return NEVER null
     */
    String getBaseUrl();

    boolean isPassthroughEnabled();

    Integer getTimeoutSeconds();

    Integer getCacheTimeoutSeconds();
}
