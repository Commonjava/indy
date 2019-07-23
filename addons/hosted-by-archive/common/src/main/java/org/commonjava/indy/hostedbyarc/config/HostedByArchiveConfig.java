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
package org.commonjava.indy.hostedbyarc.config;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;

@SectionName( HostedByArchiveConfig.SECTION )
@ApplicationScoped
public class HostedByArchiveConfig
        implements IndyConfigInfo
{
    public static final String SECTION = "hosted-by-archive";

    public static final String ENABLED_PARAM = "enabled";

    private static final String LOCK_TIMEOUT_MINS_PARAM = "lock.timeout.minutes";

    public static final long DEFAULT_LOCK_TIMEOUT_MINS = 30;

    public static final boolean DEFAULT_ENABLED = false;

    private Boolean enabled;

    private Long lockTimeoutMins;

    public HostedByArchiveConfig()
    {
    }

    public HostedByArchiveConfig( Boolean enabled, Long lockTimeoutMins )
    {
        this.enabled = enabled;
        this.lockTimeoutMins = lockTimeoutMins;
    }

    public Boolean isEnabled()
    {
        return enabled == null ? DEFAULT_ENABLED : enabled;
    }

    @ConfigName( ENABLED_PARAM )
    public void setEnabled( Boolean enabled )
    {
        this.enabled = enabled;
    }

    public Long getLockTimeoutMins()
    {
        return lockTimeoutMins == null ? DEFAULT_LOCK_TIMEOUT_MINS : lockTimeoutMins;
    }

    @ConfigName( LOCK_TIMEOUT_MINS_PARAM )
    public void setLockTimeoutMins( Long lockTimeoutMins )
    {
        this.lockTimeoutMins = lockTimeoutMins;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return "conf.d/hosted-by-archive.conf";
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream( "default-hosted-by-archive.conf" );
    }
}
