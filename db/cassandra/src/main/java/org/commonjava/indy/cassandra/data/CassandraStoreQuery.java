/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.core.conf.IndyStoreManagerConfig;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.cassandra.CassandraClient;
import org.commonjava.indy.subsys.cassandra.util.SchemaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

import static org.commonjava.indy.cassandra.data.CassandraStoreUtil.TABLE_AFFECTED_STORE;
import static org.commonjava.indy.cassandra.data.CassandraStoreUtil.TABLE_STORE;

/**
 * @deprecated The store management functions has been extracted into Repository Service, which is maintained in "ServiceStoreDataManager"
 */
@ApplicationScoped
@Deprecated
public class CassandraStoreQuery
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    CassandraClient client;

    @Inject
    IndyStoreManagerConfig config;

    @Inject
    IndyConfiguration indyConfig;

    private Mapper<DtxArtifactStore> storeMapper;

    private Session session;

    private PreparedStatement preparedSingleArtifactStoreQuery;

    private PreparedStatement preparedArtifactStoresQuery;

    private PreparedStatement preparedArtifactStoreDel;

    private PreparedStatement preparedArtifactStoreExistedQuery;

    private PreparedStatement preparedArtifactStoresQueryByKeys;

    private PreparedStatement preparedAffectedStoresQuery;

    private PreparedStatement preparedAffectedStoresIncrement;

    private PreparedStatement preparedAffectedStoresReduction;

    private PreparedStatement preparedAffectedStoreExistedQuery;

    private PreparedStatement preparedAffectedStoreDel;

    public CassandraStoreQuery() {}

    public CassandraStoreQuery( CassandraClient client, IndyStoreManagerConfig config, IndyConfiguration indyConfig )
    {
        this.client = client;
        this.config = config;
        this.indyConfig = indyConfig;
        init();
    }

    @PostConstruct
    public void init()
    {

        String keySpace = config.getKeyspace();

        session = client.getSession( keySpace );

        session.execute( SchemaUtils.getSchemaCreateKeyspace( keySpace, indyConfig.getKeyspaceReplicas() ));
        session.execute( CassandraStoreUtil.getSchemaCreateTableStore( keySpace ) );
        session.execute( CassandraStoreUtil.getSchemaCreateIndex4Store( keySpace ) );
        session.execute( CassandraStoreUtil.getSchemaCreateTableAffectedStore( keySpace ) );

        MappingManager manager = new MappingManager( session );

        storeMapper = manager.mapper( DtxArtifactStore.class, keySpace );

        preparedSingleArtifactStoreQuery = session.prepare(
                        "SELECT packagetype, storeType, namehashprefix, name, description, transientMetadata, metadata, disabled, disableTimeout, pathStyle, pathMaskPatterns, authoritativeIndex, createTime, rescanInProgress, extras FROM "
                                        + keySpace + "." + TABLE_STORE + " WHERE typekey=? AND namehashprefix=? AND name=?" );

        preparedArtifactStoresQuery = session.prepare(
                        "SELECT packagetype, storeType, namehashprefix, name, description, transientMetadata, metadata, disabled, disableTimeout, pathStyle, pathMaskPatterns, authoritativeIndex, createTime, rescanInProgress, extras FROM "
                                        + keySpace + "." + TABLE_STORE );

        preparedArtifactStoresQueryByKeys = session.prepare(
                        "SELECT packagetype, storeType, namehashprefix, name, description, transientMetadata, metadata, disabled, disableTimeout, pathStyle, pathMaskPatterns, authoritativeIndex, createTime, rescanInProgress, extras FROM "
                                        + keySpace + "." + TABLE_STORE + " WHERE typekey=?" );

        preparedArtifactStoreExistedQuery = session.prepare( "SELECT name FROM " + keySpace + "." + TABLE_STORE + " LIMIT 1");

        preparedArtifactStoreDel = session.prepare( "DELETE FROM " + keySpace + "." + TABLE_STORE + " WHERE typekey=? AND namehashprefix=? AND name=? IF EXISTS" );

        preparedAffectedStoresQuery = session.prepare( "SELECT key, affectedStores FROM " + keySpace + "." + TABLE_AFFECTED_STORE + " WHERE key=? ");

        preparedAffectedStoresIncrement =
                        session.prepare( "UPDATE " + keySpace + "." + TABLE_AFFECTED_STORE + " SET affectedStores = affectedStores + ? WHERE key=?" );

        preparedAffectedStoresReduction =
                        session.prepare( "UPDATE " + keySpace + "." + TABLE_AFFECTED_STORE + " SET affectedStores = affectedStores - ? WHERE key=?" );

        preparedAffectedStoreExistedQuery = session.prepare( "SELECT key FROM " + keySpace + "." + TABLE_AFFECTED_STORE + " LIMIT 1");

        preparedAffectedStoreDel = session.prepare( "DELETE FROM " + keySpace + "." + TABLE_AFFECTED_STORE + " WHERE key=? " );
    }

    public DtxArtifactStore getArtifactStore( String packageType, StoreType type, String name )
    {
        BoundStatement bound = preparedSingleArtifactStoreQuery.bind(
                        CassandraStoreUtil.getTypeKey( packageType, type.name() ),
                        CassandraStoreUtil.getHashPrefix( name ), name );
        ResultSet result = session.execute( bound );
        return toDtxArtifactStore( result.one() );
    }

    public Set<DtxArtifactStore> getArtifactStoresByPkgAndType( String packageType, StoreType type )
    {

        BoundStatement bound = preparedArtifactStoresQueryByKeys.bind(
                        CassandraStoreUtil.getTypeKey( packageType, type.name() ) );
        ResultSet result = session.execute( bound );

        Set<DtxArtifactStore> dtxArtifactStoreSet = new HashSet<>(  );
        result.forEach( row -> {
            dtxArtifactStoreSet.add( toDtxArtifactStore( row ) );
        } );

        return dtxArtifactStoreSet;
    }

    public Set<DtxArtifactStore> getAllArtifactStores()
    {

        BoundStatement bound = preparedArtifactStoresQuery.bind();
        ResultSet result = session.execute( bound );

        Set<DtxArtifactStore> dtxArtifactStoreSet = new HashSet<>(  );
        result.forEach( row -> {
            dtxArtifactStoreSet.add( toDtxArtifactStore( row ) );
        } );

        return dtxArtifactStoreSet;
    }

    public Boolean isEmpty()
    {
        BoundStatement bound = preparedArtifactStoresQuery.bind();
        ResultSet result = session.execute( bound );
        return result.one() == null;
    }

    public DtxArtifactStore removeArtifactStore( String packageType, StoreType type, String name )
    {
        DtxArtifactStore dtxArtifactStore = getArtifactStore( packageType, type, name );
        if ( dtxArtifactStore != null )
        {
            BoundStatement bound = preparedArtifactStoreDel.bind( CassandraStoreUtil.getTypeKey( packageType, type.name() ),  CassandraStoreUtil.getHashPrefix( name ), name );
            session.execute( bound );
        }
        return dtxArtifactStore;
    }

    private DtxArtifactStore toDtxArtifactStore( Row row )
    {
        if ( row == null )
        {
            return null;
        }
        DtxArtifactStore store = new DtxArtifactStore();
        store.setPackageType( row.getString( CassandraStoreUtil.PACKAGE_TYPE ) );
        store.setStoreType( row.getString( CassandraStoreUtil.STORE_TYPE ) );
        store.setName( row.getString( CassandraStoreUtil.NAME ) );
        store.setNameHashPrefix( row.getInt( CassandraStoreUtil.NAME_HASH_PREFIX ) );
        store.setPathMaskPatterns( row.getSet( CassandraStoreUtil.PATH_MASK_PATTERNS, String.class ) );
        store.setPathStyle( row.getString( CassandraStoreUtil.PATH_STYLE ) );
        store.setDisabled( row.getBool( CassandraStoreUtil.DISABLED ) );
        store.setDescription( row.getString( CassandraStoreUtil.DESCRIPTION ) );
        store.setAuthoritativeIndex( row.getBool( CassandraStoreUtil.AUTHORITATIVE_INDEX ) );
        store.setCreateTime( row.getString( CassandraStoreUtil.CREATE_TIME ) );
        store.setDisableTimeout( row.getInt( CassandraStoreUtil.DISABLE_TIMEOUT ) );
        store.setMetadata( row.getMap( CassandraStoreUtil.METADATA, String.class, String.class ) );
        store.setRescanInProgress( row.getBool( CassandraStoreUtil.RESCAN_IN_PROGRESS ) );
        store.setTransientMetadata( row.getMap( CassandraStoreUtil.TRANSIENT_METADATA, String.class, String.class ) );
        store.setExtras( row.getMap( CassandraStoreUtil.EXTRAS, String.class, String.class ) );
        return store;
    }

    public void createDtxArtifactStore( DtxArtifactStore dtxArtifactStore )
    {
        storeMapper.save( dtxArtifactStore );
    }

    public DtxAffectedStore getAffectedStore( StoreKey key )
    {
        BoundStatement bound = preparedAffectedStoresQuery.bind(key.toString());
        ResultSet result = session.execute( bound );
        return toDtxAffectedStore( result.one() );
    }

    private DtxAffectedStore toDtxAffectedStore( Row row )
    {
        if ( row == null )
        {
            return null;
        }

        DtxAffectedStore store = new DtxAffectedStore();
        store.setKey( row.getString( CassandraStoreUtil.KEY ) );
        store.setAffectedStores( row.getSet( CassandraStoreUtil.AFFECTED_STORES, String.class ) );

        return store;
    }

    public void addAffectedBy( StoreKey storeKey, StoreKey affected )
    {
        BoundStatement bound = preparedAffectedStoresIncrement.bind();

        Set<String> increment = new HashSet();
        increment.add( affected.toString() );
        bound.setSet( 0, increment );
        bound.setString( 1, storeKey.toString() );
        session.execute( bound );
    }

    public void removeAffectedBy( StoreKey storeKey, StoreKey affected )
    {
        BoundStatement bound = preparedAffectedStoresReduction.bind();

        Set<String> reduction = new HashSet();
        reduction.add( affected.toString() );
        bound.setSet( 0, reduction );
        bound.setString( 1, storeKey.toString() );
        session.execute( bound );
    }

    public Boolean isAffectedEmpty()
    {
        BoundStatement bound = preparedAffectedStoreExistedQuery.bind();
        ResultSet result = session.execute( bound );
        return result.one() == null;
    }

    public void removeAffectedStore( StoreKey key )
    {
        DtxAffectedStore affectedStore = getAffectedStore( key );
        if ( affectedStore != null )
        {
            BoundStatement bound = preparedAffectedStoreDel.bind( key.toString() );
            session.execute( bound );
        }
    }
}
