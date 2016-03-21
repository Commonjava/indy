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
package org.commonjava.indy.filer.def.conf;

import org.commonjava.indy.conf.IndyConfigFactory;
import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.conf.SystemPropertyProvider;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;
import java.util.Properties;

@SectionName( "storage-default" )
@ApplicationScoped
public class DefaultStorageProviderConfiguration
    implements IndyConfigInfo, SystemPropertyProvider
{

    public static final File DEFAULT_BASEDIR = new File( "/var/lib/indy/storage" );

    public static final String STORAGE_DIR = "indy.storage.dir";

    private File storageBasedir;

    public DefaultStorageProviderConfiguration()
    {
    }

    public DefaultStorageProviderConfiguration( final File storageBasedir )
    {
        this.storageBasedir = storageBasedir;
    }

    public File getStorageRootDirectory()
    {
        return storageBasedir == null ? DEFAULT_BASEDIR : storageBasedir;
    }

    @ConfigName( "storage.dir" )
    public void setStorageRootDirectory( final File storageBasedir )
    {
        this.storageBasedir = storageBasedir;
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
                     .getResourceAsStream( "default-storage.conf" );
    }

    @Override
    public Properties getSystemProperties()
    {
        Properties p = new Properties();
        p.setProperty( STORAGE_DIR, getStorageRootDirectory().getAbsolutePath() );
        return p;
    }
}
