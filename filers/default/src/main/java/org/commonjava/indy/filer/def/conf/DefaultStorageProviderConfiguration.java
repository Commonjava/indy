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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
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

    private final Map<String, Object> cassandraProps = new HashMap<>(); // for path mapped storage

    @PostConstruct
    public void setUpPathMappedStorage()
    {
        cassandraProps.put( PROP_CASSANDRA_HOST, DEFAULT_CASSANDRA_HOST );
        cassandraProps.put( PROP_CASSANDRA_PORT, DEFAULT_CASSANDRA_PORT );
        cassandraProps.put( PROP_CASSANDRA_KEYSPACE, DEFAULT_CASSANDRA_KEYSPACE );
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
    public void setNFSStorageRootDirectory( final File nfsStorageRootDirectory )
    {
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

    private static final String DEFAULT_CASSANDRA_HOST = "localhost";

    private static final int DEFAULT_CASSANDRA_PORT = 9042;

    private static final String DEFAULT_CASSANDRA_KEYSPACE = "indy";

    @ConfigName( "storage.cassandra.host" )
    public void setCassandraHost( String host )
    {
        cassandraProps.put( PROP_CASSANDRA_HOST, host );
    }

    @ConfigName( "storage.cassandra.port" )
    public void setCassandraPort( int port )
    {
        cassandraProps.put( PROP_CASSANDRA_PORT, port );
    }

    @ConfigName( "storage.cassandra.keyspace" )
    public void setCassandraKeyspace( String keyspace )
    {
        cassandraProps.put( PROP_CASSANDRA_KEYSPACE, keyspace );
    }

    private final DefaultPathMappedStorageConfig pathMappedStorageConfig = new DefaultPathMappedStorageConfig();

    @Override
    public int getGCIntervalInMinutes()
    {
        return pathMappedStorageConfig.getGCIntervalInMinutes();
    }

    @ConfigName( "storage.gc.intervalinminutes" )
    public void setGCIntervalInMinutes( int gcIntervalInMinutes )
    {
        pathMappedStorageConfig.setGcIntervalInMinutes( gcIntervalInMinutes );
    }

    @Override
    public int getGCGracePeriodInHours()
    {
        return pathMappedStorageConfig.getGCGracePeriodInHours();
    }

    @ConfigName( "storage.gc.graceperiodinhours" )
    public void setGCGracePeriodInHours( int gcGracePeriodInHours )
    {
        pathMappedStorageConfig.setGcGracePeriodInHours( gcGracePeriodInHours );
    }

    private List<String> subsystemEnabledFileSystems = new ArrayList<>();

    @Override
    public boolean isSubsystemEnabled( String fileSystem )
    {
        return subsystemEnabledFileSystems.contains( fileSystem );
    }

    // comma separated file system names
    @ConfigName( "storage.subsystem.enabled.filesystems" )
    public void setSubsystemEnabledFileSystems( String fileSystems )
    {
        if ( fileSystems != null && isNotBlank( fileSystems.trim() ) )
        {
            this.subsystemEnabledFileSystems = Arrays.asList( fileSystems.split( "," ) );
        }
    }

    @Override
    public Object getProperty( String key )
    {
        return cassandraProps.get( key );
    }
}
