/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.filer.ispn.fileio;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.CustomStoreConfigurationBuilder;
import org.infinispan.io.GridFile;
import org.infinispan.io.GridFilesystem;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Created by jdcasey on 3/11/16.
 */
public class StorageFileIOTest
{
    private static EmbeddedCacheManager CACHE_MANAGER;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Rule
    public TestName name = new TestName();

    private File storageRoot;

    private GridFilesystem fs;

    private Cache<String, byte[]> dataCache;

    @BeforeClass
    public static void setupClass()
    {
        CACHE_MANAGER = new DefaultCacheManager( new ConfigurationBuilder().build() );
    }

    @Before
    public void setup()
            throws IOException
    {
        storageRoot = temp.newFolder( "data" );

        Properties storageProps = new Properties();
        storageProps.setProperty( StorageFileIO.STORAGE_ROOT_DIR, storageRoot.getAbsolutePath() );

        Configuration dataConfig = new ConfigurationBuilder().persistence()
                                                             .passivation( true )
                                                             .addStore( CustomStoreConfigurationBuilder.class )
                                                             .customStoreClass( StorageFileIO.class )
                                                             .properties( storageProps )
                                                             .build();

        String dataName = name.getMethodName() + "-data";
        CACHE_MANAGER.defineConfiguration( dataName, dataConfig );

        Configuration mdConfig = new ConfigurationBuilder().build();

        String metadataName = name.getMethodName() + "-metadata";
        CACHE_MANAGER.defineConfiguration( metadataName, mdConfig );

        dataCache = CACHE_MANAGER.getCache( dataName );
        fs = new GridFilesystem( dataCache, CACHE_MANAGER.getCache( metadataName ) );
    }

    @Test
    public void readWriteGridFilesystem()
            throws IOException
    {
        String file = "test.txt";

        String src = "This is a test.";
        String src2 = "This is another test.";

        Arrays.asList(src, src2).stream().forEach( (str)->{
            try
            {
                write( file, str );
                assertThat( read(file), equalTo( str ) );
            }
            catch ( IOException e )
            {
                e.printStackTrace();
                fail( "Failed to read/write: '" + str + "'" );
            }
        });
    }

    private String read( String file )
            throws IOException
    {
        try(InputStream in = fs.getInput( file ))
        {
            return IOUtils.toString( in );
        }
    }

    private void write( String file, String str )
            throws IOException
    {
        try(OutputStream out = fs.getOutput( file ) )
        {
            IOUtils.write( str, out );
        }
    }
}
