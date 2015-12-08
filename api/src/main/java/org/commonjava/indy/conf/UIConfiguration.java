/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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

import java.io.File;
import java.io.InputStream;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigNames;
import org.commonjava.web.config.annotation.SectionName;

/**
 * Configuration class that tells Indy where its UI files are. These are static resources that are served up via servlet or similar, and which are
 * designed to be the default UI for Indy (talking to its REST resources).
 */
@SectionName( "ui" )
@Alternative
@Named( "unused" )
public class UIConfiguration
{

    @javax.enterprise.context.ApplicationScoped
    public static class UIFeatureConfig
        extends AbstractIndyFeatureConfig<UIConfiguration, UIConfiguration>
    {
        @Inject
        private UIConfigInfo info;

        public UIFeatureConfig()
        {
            super( UIConfiguration.class );
        }

        @Produces
        @Default
        public UIConfiguration getFlatFileConfig()
            throws ConfigurationException
        {
            return getConfig();
        }

        @Override
        public IndyConfigClassInfo getInfo()
        {
            return info;
        }
    }

    @javax.enterprise.context.ApplicationScoped
    public static class UIConfigInfo
        extends AbstractIndyConfigInfo
    {
        public UIConfigInfo()
        {
            super( UIConfiguration.class );
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
    }

    public static final File DEFAULT_DIR = new File( System.getProperty( "indy.home", "/var/lib/indy" ), "ui" );

    private File uiDir;

    public UIConfiguration()
    {
    }

    @ConfigNames( "ui.dir" )
    public UIConfiguration( final File uiDir )
    {
        this.uiDir = uiDir;
    }

    public File getUIDir()
    {
        return uiDir == null ? DEFAULT_DIR : uiDir;
    }

    public void setUIDir( final File uiDir )
    {
        this.uiDir = uiDir;
    }

}
