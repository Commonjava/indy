/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.cassandra.data;

import org.apache.commons.lang3.RandomStringUtils;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.commonjava.indy.conf.DefaultIndyConfiguration;
import org.commonjava.indy.core.conf.IndyStoreManagerConfig;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.pkg.PackageTypeConstants;
import org.commonjava.indy.cassandra.testcat.CassandraTest;
import org.commonjava.indy.subsys.cassandra.CassandraClient;
import org.commonjava.indy.subsys.cassandra.config.CassandraConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@Category( CassandraTest.class )
public class CassandraStoreTest
{

    CassandraClient client;

    CassandraStoreQuery storeQuery;

    @Before
    public void start() throws Exception
    {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();

        CassandraConfig config = new CassandraConfig();
        config.setEnabled( true );
        config.setCassandraHost( "localhost" );
        config.setCassandraPort( 9142 );

        client = new CassandraClient( config );
        IndyStoreManagerConfig storeConfig = new IndyStoreManagerConfig( "noncontent", 1);

        DefaultIndyConfiguration indyConfig = new DefaultIndyConfiguration();
        indyConfig.setKeyspaceReplicas( 1 );

        storeQuery = new CassandraStoreQuery( client, storeConfig, indyConfig );

    }

    @After
    public void stop()
    {
        client.close();
        EmbeddedCassandraServerHelper.getSession();
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
    }

    @Test
    public void testQuery()
    {
        DtxArtifactStore store = createTestStore( PackageTypeConstants.PKG_TYPE_MAVEN, StoreType.hosted.name() );

        Set<DtxArtifactStore> storeSet = storeQuery.getAllArtifactStores();

        assertThat(storeSet.size(), equalTo( 1 ));

        storeQuery.removeArtifactStore( store.getPackageType(), StoreType.hosted, store.getName() );

        Set<DtxArtifactStore> storeSet2 = storeQuery.getAllArtifactStores();

        assertThat(storeSet2.size(), equalTo( 0 ));

    }

    @Test
    public void testIsEmpty()
    {

        assertThat( storeQuery.isEmpty(), equalTo( Boolean.TRUE ));

        createTestStore( PackageTypeConstants.PKG_TYPE_MAVEN, StoreType.hosted.name() );

        assertThat( storeQuery.isEmpty(), equalTo( Boolean.FALSE ));
    }

    @Test
    public void testGetStoreByPkgAndType()
    {

        createTestStore( PackageTypeConstants.PKG_TYPE_MAVEN, StoreType.hosted.name() );
        Set<DtxArtifactStore> artifactStoreSet =
                        storeQuery.getArtifactStoresByPkgAndType( PackageTypeConstants.PKG_TYPE_MAVEN,
                                                                  StoreType.hosted );
        assertThat(artifactStoreSet.size(), equalTo( 1 ));
    }

    @Test
    public void testHashPrefix()
    {
        for ( int i = 0; i< 50; i++ )
        {
            String generatedName = RandomStringUtils.random( 10, true, false );
            int result = CassandraStoreUtil.getHashPrefix( generatedName );
            assertThat( (0 <= result && result < CassandraStoreUtil.MODULO_VALUE), equalTo( true ) );
        }

    }

    private DtxArtifactStore createTestStore( final String packageType, final String storeType )
    {
        String name = "build-001";
        DtxArtifactStore store = new DtxArtifactStore();
        store.setTypeKey( CassandraStoreUtil.getTypeKey( packageType, storeType ) );
        store.setPackageType( packageType );
        store.setStoreType( storeType );
        store.setNameHashPrefix( CassandraStoreUtil.getHashPrefix( name ) );
        store.setName( name );
        store.setDescription( "test cassandra store" );
        store.setDisabled( true );

        Set<String> maskPatterns = new HashSet<>(  );

        store.setPathMaskPatterns( maskPatterns );
        storeQuery.createDtxArtifactStore( store );

        return store;
    }

}
