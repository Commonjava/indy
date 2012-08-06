/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.core.conf;

import java.io.File;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;
import javax.inject.Singleton;

import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.ConfigNames;
import org.commonjava.web.config.annotation.SectionName;
import org.commonjava.web.config.section.ConfigurationSectionListener;

@SectionName( ConfigurationSectionListener.DEFAULT_SECTION )
@Alternative
@Named( "not-used-directly" )
public class DefaultAproxConfiguration
    implements AproxConfiguration
{

    @Singleton
    public static final class ConfigSet
        extends AproxConfigSet<AproxConfiguration, DefaultAproxConfiguration>
    {
        public ConfigSet()
        {
            super( DefaultAproxConfiguration.class );
        }
    }

    public static final File DEFAULT_STORAGE_ROOT_DIR = new File( "/var/lib/aprox/storage" );

    public static final int DEFAULT_PASSTHROUGH_TIMEOUT_SECONDS = 300;

    private File storageRootDirectory = DEFAULT_STORAGE_ROOT_DIR;

    private int passthroughTimeoutSeconds = DEFAULT_PASSTHROUGH_TIMEOUT_SECONDS;

    public DefaultAproxConfiguration()
    {
    }

    @ConfigNames( "storage.dir" )
    public DefaultAproxConfiguration( final File repoRootDir )
    {
        this.storageRootDirectory = repoRootDir;
    }

    @Override
    public File getStorageRootDirectory()
    {
        return storageRootDirectory;
    }

    public void setStorageRootDirectory( final File repositoryRootDirectory )
    {
        this.storageRootDirectory = repositoryRootDirectory;
    }

    @Override
    public int getPassthroughTimeoutSeconds()
    {
        return passthroughTimeoutSeconds;
    }

    @ConfigName( "passthrough.timeout" )
    public void setPassthroughTimeoutSeconds( final int seconds )
    {
        passthroughTimeoutSeconds = seconds;
    }

}
