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
package org.commonjava.indy.httprox.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;

@SectionName( "httprox" )
@ApplicationScoped
public class HttproxConfig
    implements IndyConfigInfo
{
    private static final int DEFAULT_PORT = 8081;

    private static final boolean DEFAULT_ENABLED = false;

    private static final String DEFAULT_PROXY_REALM = "httprox";

    private static final boolean DEFAULT_SECURED = false;

    private static final boolean DEFAULT_MITM_ENABLED = false;

    private static final String DEFAULT_TRACKING_TYPE = TrackingType.SUFFIX.name();

    private static final int DEFAULT_AUTH_CACHE_EXPIRATION_HOURS = 1;

    private static final int DEFAULT_MITM_SO_TIMEOUT_MINUTES = 30;

    private String proxyRealm;

    private Boolean enabled;

    private Boolean MITMEnabled;

    private Boolean secured;

    private Integer port;

    private Integer authCacheExpirationHours;

    private String trackingType;

    private String noCachePatterns; // if multiple patterns, split by comma

    private String MITMCAKey;

    private String MITMCACert;

    private String MITMDNTemplate;

    private Integer MITMSoTimeoutMinutes;

    public TrackingType getTrackingType()
    {
        return TrackingType.valueOf( trackingType == null ? DEFAULT_TRACKING_TYPE : trackingType.toUpperCase() );
    }

    @ConfigName( "tracking.type" )
    public void setTrackingType( final String option )
    {
        this.trackingType = option;
    }

    public void setTrackingType( final TrackingType type )
    {
        this.trackingType = type.name();
    }

    public boolean isEnabled()
    {
        return enabled == null ? DEFAULT_ENABLED : enabled;
    }

    @ConfigName( "enabled" )
    public void setEnabled( final Boolean enabled )
    {
        this.enabled = enabled;
    }

    public boolean isSecured()
    {
        return secured == null ? DEFAULT_SECURED : secured;
    }

    @ConfigName( "secured" )
    public void setSecured( final Boolean secured )
    {
        this.secured = secured;
    }

    public Integer getPort()
    {
        return port == null ? DEFAULT_PORT : port;
    }

    @ConfigName( "port" )
    public void setPort( final Integer port )
    {
        this.port = port;
    }

    public String getProxyRealm()
    {
        return proxyRealm == null ? DEFAULT_PROXY_REALM : proxyRealm;
    }

    @ConfigName( "proxy.realm" )
    public void setProxyRealm( final String proxyRealm )
    {
        this.proxyRealm = proxyRealm;
    }

    public Integer getAuthCacheExpirationHours()
    {
        return authCacheExpirationHours == null ? DEFAULT_AUTH_CACHE_EXPIRATION_HOURS : authCacheExpirationHours;
    }

    @ConfigName( "auth.cache.expiration.hours" )
    public void setAuthCacheExpirationHours( Integer authCacheExpirationHours )
    {
        this.authCacheExpirationHours = authCacheExpirationHours;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return new File( IndyConfigInfo.CONF_INCLUDES_DIR, "httprox.conf" ).getPath();
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread()
                     .getContextClassLoader()
                     .getResourceAsStream( "default-httprox.conf" );
    }

    public String getNoCachePatterns()
    {
        return noCachePatterns;
    }

    @ConfigName( "nocache.patterns" )
    public void setNoCachePatterns( String noCachePatterns )
    {
        this.noCachePatterns = noCachePatterns;
    }

    @ConfigName( "MITM.enabled" )
    public void setMITMEnabled( Boolean MITMEnabled )
    {
        this.MITMEnabled = MITMEnabled;
    }

    public boolean isMITMEnabled()
    {
        return MITMEnabled == null ? DEFAULT_MITM_ENABLED : MITMEnabled;
    }

    public String getMITMCAKey()
    {
        return MITMCAKey;
    }

    @ConfigName( "MITM.ca.key" )
    public void setMITMCAKey( String MITMCAKey )
    {
        this.MITMCAKey = MITMCAKey;
    }

    public String getMITMCACert()
    {
        return MITMCACert;
    }

    @ConfigName( "MITM.ca.cert" )
    public void setMITMCACert( String MITMCACert )
    {
        this.MITMCACert = MITMCACert;
    }

    public String getMITMDNTemplate()
    {
        return MITMDNTemplate;
    }

    @ConfigName( "MITM.dn.template" )
    public void setMITMDNTemplate( String MITMDNTemplate )
    {
        this.MITMDNTemplate = MITMDNTemplate;
    }

    public Integer getMITMSoTimeoutMinutes()
    {
        return MITMSoTimeoutMinutes == null ? DEFAULT_MITM_SO_TIMEOUT_MINUTES : MITMSoTimeoutMinutes;
    }

    @ConfigName( "MITM.so.timeout.minutes" )
    public void setMITMSoTimeoutMinutes( Integer MITMSoTimeoutMinutes )
    {
        this.MITMSoTimeoutMinutes = MITMSoTimeoutMinutes;
    }
}
