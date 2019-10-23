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
import org.commonjava.storage.pathmapped.config.DefaultPathMappedStorageConfig;
import org.commonjava.storage.pathmapped.config.PathMappedStorageConfig;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.commonjava.storage.pathmapped.util.CassandraPathDBUtils.PROP_CASSANDRA_HOST;
import static org.commonjava.storage.pathmapped.util.CassandraPathDBUtils.PROP_CASSANDRA_KEYSPACE;
import static org.commonjava.storage.pathmapped.util.CassandraPathDBUtils.PROP_CASSANDRA_PORT;

@SectionName( "storage-default" )
@ApplicationScoped
public class DefaultStorageProviderConfiguration
    implements IndyConfigInfo, SystemPropertyProvider, PathMappedStorageConfig
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
        this( storageBasedir, null );
    }

    public DefaultStorageProviderConfiguration( final File storageBasedir, final File nfsStoreBasedir )
    {
        this.storageBasedir = storageBasedir;
        this.nfsStoreBasedir = nfsStoreBasedir;
        this.setUpPathMappedStorage();
    }

    private final Map<String, Object> cassandraProps = new HashMap<>();

    @PostConstruct
    public void setUpPathMappedStorage()
    {
        cassandraProps.put( PROP_CASSANDRA_HOST, cassandraHost );
        cassandraProps.put( PROP_CASSANDRA_PORT, cassandraPort );
        cassandraProps.put( PROP_CASSANDRA_KEYSPACE, cassandraKeyspace );
        this.pathMappedStorageConfig = new DefaultPathMappedStorageConfig( cassandraProps );
    }

    public File getStorageRootDirectory()
    {
        return storageBasedir == null ? DEFAULT_BASEDIR : storageBasedir;
    }

    @Deprecated
    public File getNFSStorageRootDirectory()
    {
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


    // Path mapped storage config
    private String cassandraHost = "localhost";

    @ConfigName( "storage.cassandra.host" )
    public void setCassandraHost( String host )
    {
        cassandraHost = host;
        cassandraProps.put( PROP_CASSANDRA_HOST, cassandraHost );
    }

    private int cassandraPort = 9042;

    @ConfigName( "storage.cassandra.port" )
    public void setCassandraPort( int port )
    {
        cassandraPort = port;
        cassandraProps.put( PROP_CASSANDRA_PORT, cassandraPort );
    }

    private String cassandraKeyspace = "indy";

    @ConfigName( "storage.cassandra.keyspace" )
    public void setCassandraKeyspace( String keyspace )
    {
        cassandraKeyspace = keyspace;
        cassandraProps.put( PROP_CASSANDRA_KEYSPACE, cassandraKeyspace );
    }

    private PathMappedStorageConfig pathMappedStorageConfig;

    @Override
    public int getGCIntervalInMinutes()
    {
        return pathMappedStorageConfig.getGCIntervalInMinutes();
    }

    @Override
    public int getGCGracePeriodInHours()
    {
        return pathMappedStorageConfig.getGCGracePeriodInHours();
    }

    @Override
    public boolean isSubsystemEnabled( String fileSystem )
    {
        return pathMappedStorageConfig.isSubsystemEnabled( fileSystem );
    }

    @Override
    public Object getProperty( String key )
    {
        return pathMappedStorageConfig.getProperty( key );
    }
}
