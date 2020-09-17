package org.commonjava.indy.cassandra.data;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.commonjava.indy.cassandra.data.config.CassandraStoreConfig;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.cassandra.CassandraClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class CassandraStoreQuery
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    CassandraClient client;

    @Inject
    CassandraStoreConfig storeConfig;

    private Mapper<DtxArtifactStore> storeMapper;

    private Session session;

    private PreparedStatement preparedSingleArtifactStoreQuery;

    private PreparedStatement preparedArtifactStoresQuery;

    public CassandraStoreQuery() {}

    public CassandraStoreQuery( CassandraClient client, CassandraStoreConfig config )
    {
        this.client = client;
        this.storeConfig = config;
        init();
    }

    @PostConstruct
    public void init()
    {

        String keySpace = storeConfig.getKeyspace();

        session = client.getSession( keySpace );

        session.execute( CassandraStoreUtil.getSchemaCreateKeyspace( keySpace, storeConfig ) );
        session.execute( CassandraStoreUtil.getSchemaCreateTableStore( keySpace ) );

        MappingManager manager = new MappingManager( session );

        storeMapper = manager.mapper( DtxArtifactStore.class, keySpace );

        preparedSingleArtifactStoreQuery = session.prepare( "select * from " + keySpace + ".artifactstore where packagetype=? and storetype=? and name=?" );

        preparedArtifactStoresQuery = session.prepare( "select * from " + keySpace + ".artifactstore" );
    }

    public DtxArtifactStore getArtifactStore( String packageType, StoreType type, String name )
    {
        BoundStatement bound = preparedSingleArtifactStoreQuery.bind( packageType, type.name(), name );
        ResultSet result = session.execute( bound );
        return toDtxArtifactStore( result.one() );
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
}
