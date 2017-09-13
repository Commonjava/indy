/**
 * Copyright (C) 2011-2017 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.autoprox.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;

@SectionName( AutoProxConfig.SECTION )
@ApplicationScoped
public class AutoProxConfig
        implements IndyConfigInfo
{
    public static final String SECTION = "autoprox";

    public static final String DEFAULT_DIR = "autoprox";

    public static final String BASEDIR_PARAM = "basedir";

    public static final String ENABLED_PARAM = "enabled";

    private String basedir;

    private boolean enabled;

    public AutoProxConfig()
    {
    }

    public AutoProxConfig( final String basedir, final boolean enabled )
    {
        this.basedir = basedir;
        this.enabled = enabled;
    }

    @ConfigName(AutoProxConfig.BASEDIR_PARAM)
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
        return enabled;
    }

    @ConfigName(AutoProxConfig.ENABLED_PARAM)
    public void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return "conf.d/autoprox.conf";
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread()
                     .getContextClassLoader()
                     .getResourceAsStream( "default-autoprox.conf" );
    }

}
