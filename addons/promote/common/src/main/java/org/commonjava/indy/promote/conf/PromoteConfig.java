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
package org.commonjava.indy.promote.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;

@SectionName( PromoteConfig.SECTION )
@ApplicationScoped
public class PromoteConfig
        implements IndyConfigInfo
{
    public static final String SECTION = "promote";

    public static final String DEFAULT_DIR = "promote";

    public static final String BASEDIR_PARAM = "basedir";

    public static final String ENABLED_PARAM = "enabled";

    private static final String LOCK_TIMEOUT_SECONDS_PARAM = "lock.timeout.seconds";

    private static final String AUTOLOCK_HOSTED_REPOS = "autolock.hosted.repos";

    public static final int DEFAULT_PARALLELED_BATCH_SIZE = 100;

    public static final long DEFAULT_LOCK_TIMEOUT_SECONDS = 30;

    public static final boolean DEFAULT_AUTOLOCK = true;

    public static final boolean DEFAULT_ENABLED = true;

    private String basedir;

    private Boolean enabled;

    private Boolean autoLockHostedRepos;

    private Long lockTimeoutSeconds;

    private Integer paralleledBatchSize;

    public PromoteConfig()
    {
    }

    public PromoteConfig( final String basedir, final boolean enabled )
    {
        this.basedir = basedir;
        this.enabled = enabled;
    }

    public boolean isAutoLockHostedRepos()
    {
        return autoLockHostedRepos == null ? DEFAULT_AUTOLOCK : autoLockHostedRepos;
    }

    public Boolean getAutoLockHostedRepos()
    {
        return autoLockHostedRepos;
    }

    @ConfigName( PromoteConfig.AUTOLOCK_HOSTED_REPOS )
    public void setAutoLockHostedRepos( final Boolean autoLockHostedRepos )
    {
        this.autoLockHostedRepos = autoLockHostedRepos;
    }

    @ConfigName( PromoteConfig.BASEDIR_PARAM)
    public void setBasedir( final String basedir )
    {
        this.basedir = basedir;
    }

    public String getBasedir()
    {
        return basedir == null ? DEFAULT_DIR : basedir;
    }

    public boolean isEnabled()
    {
        return enabled == null ? DEFAULT_ENABLED : enabled;
    }

    public Boolean getEnabled()
    {
        return enabled;
    }

    @ConfigName(PromoteConfig.ENABLED_PARAM)
    public void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }

    public Long getLockTimeoutSeconds()
    {
        return lockTimeoutSeconds == null ? DEFAULT_LOCK_TIMEOUT_SECONDS : lockTimeoutSeconds;
    }

    @ConfigName( PromoteConfig.LOCK_TIMEOUT_SECONDS_PARAM )
    public void setLockTimeoutSeconds( Long lockTimeoutSeconds )
    {
        this.lockTimeoutSeconds = lockTimeoutSeconds;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return "conf.d/promote.conf";
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread()
                     .getContextClassLoader()
                     .getResourceAsStream( "default-promote.conf" );
    }

    @ConfigName( "paralleled.batch.size" )
    public void setParalleledBatchSize( Integer paralleledBatchSize )
    {
        this.paralleledBatchSize = paralleledBatchSize;
    }

    public int getParalleledBatchSize()
    {
        int ret = paralleledBatchSize == null ? DEFAULT_PARALLELED_BATCH_SIZE : paralleledBatchSize;
        if ( ret <= 0 )
        {
            ret = DEFAULT_PARALLELED_BATCH_SIZE;
        }
        return ret;
    }
}
