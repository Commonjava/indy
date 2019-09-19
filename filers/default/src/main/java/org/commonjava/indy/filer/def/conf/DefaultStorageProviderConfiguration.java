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
package org.commonjava.indy.filer.def.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.indy.conf.SystemPropertyProvider;
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;

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

    // NOTE: Providing a default value negates the detection of whether the NFS CacheProvider should be used or not, in DefaultGalleyStorageProvider.
//    public static final File DEFAULT_NFS_BASEDIR = new File("/mnt/nfs/var/lib/indy/storage");

    public static final String STORAGE_DIR = "indy.storage.dir";

    public static final String NFS_STORAGE_DIR = "indy.storage.nfs.dir";

    private File storageBasedir;

    private File nfsStoreBasedir;

    public DefaultStorageProviderConfiguration()
    {
    }

    public DefaultStorageProviderConfiguration( final File storageBasedir )
    {
        this.storageBasedir = storageBasedir;
    }

    public DefaultStorageProviderConfiguration( final File storageBasedir, final File nfsStoreBasedir )
    {
        this.storageBasedir = storageBasedir;
        this.nfsStoreBasedir = nfsStoreBasedir;
    }

    public File getStorageRootDirectory()
    {
        return storageBasedir == null ? DEFAULT_BASEDIR : storageBasedir;
    }

    @Deprecated
    public File getNFSStorageRootDirectory()
    {
//        return nfsStoreBasedir == null ? DEFAULT_NFS_BASEDIR : nfsStoreBasedir;
        return nfsStoreBasedir;
    }

    @ConfigName( "storage.dir" )
    @Deprecated
    public void setStorageRootDirectory( final File storageBasedir )
    {
        this.storageBasedir = storageBasedir;
    }

    @ConfigName( "storage.nfs.dir" )
    public void setNFSStorageRootDirectory(final File nfsStorageRootDirectory){
        this.nfsStoreBasedir = nfsStorageRootDirectory;
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
    public Properties getSystemPropertyAdditions()
    {
        Properties p = new Properties();
        p.setProperty( STORAGE_DIR, getStorageRootDirectory().getAbsolutePath() );
        p.setProperty( NFS_STORAGE_DIR, getStorageRootDirectory().getAbsolutePath() );
        return p;
    }
}
