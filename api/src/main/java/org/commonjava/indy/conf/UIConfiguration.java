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
package org.commonjava.indy.conf;

import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;

/**
 * Configuration class that tells Indy where its UI files are. These are static resources that are served up via servlet or similar, and which are
 * designed to be the default UI for Indy (talking to its REST resources).
 */
@SectionName( "ui" )
@ApplicationScoped
public class UIConfiguration
    implements IndyConfigInfo
{

    public static final File DEFAULT_DIR = new File( System.getProperty( "indy.home", "/var/lib/indy" ), "ui" );

    private static final Boolean DEFAULT_ENABLED = Boolean.TRUE;

    private Boolean enabled;

    private File uiDir;

    public UIConfiguration()
    {
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return IndyConfigInfo.APPEND_DEFAULTS_TO_MAIN_CONF;
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread()
                     .getContextClassLoader()
                     .getResourceAsStream( "default-ui.conf" );
    }

    public UIConfiguration( final File uiDir )
    {
        this.uiDir = uiDir;
    }

    public File getUIDir()
    {
        return uiDir == null ? DEFAULT_DIR : uiDir;
    }

    @ConfigName( "ui.dir" )
    public void setUIDir( final File uiDir )
    {
        this.uiDir = uiDir;
    }

    public Boolean getEnabled()
    {
        return enabled == null ? DEFAULT_ENABLED : enabled;
    }

    public void setEnabled( final Boolean enabled )
    {
        this.enabled = enabled;
    }
}
