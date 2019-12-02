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
package org.commonjava.indy.core.inject;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.subsys.cassandra.CassandraClient;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.commonjava.indy.conf.DefaultIndyConfiguration.DEFAULT_NOT_FOUND_CACHE_TIMEOUT_SECONDS;

@ApplicationScoped
@Default
public class CassandraNotFoundCache
                extends AbstractNotFoundCache
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String TIMEOUT_FORMAT = "yyyy-MM-dd HH:mm:ss z";

    private PreparedStatement preparedInsert;

    private PreparedStatement preparedExistQuery;

    private PreparedStatement preparedDelete;

    private PreparedStatement preparedDeleteByStore;

    private PreparedStatement preparedCountByStore;

    private PreparedStatement preparedQueryByStore;

    public static String getSchemaCreateKeyspace( String keyspace )
    {
        return "CREATE KEYSPACE IF NOT EXISTS " + keyspace
                        + " WITH REPLICATION = {'class':'SimpleStrategy', 'replication_factor':1};";
    }

    // @formatter:off
    public static String getSchemaCreateTable( String keyspace )
    {
        return "CREATE TABLE IF NOT EXISTS " + keyspace + ".nfc ("
                        + "storekey varchar,"
                        + "path varchar,"
                        + "creation timestamp,"
                        + "expiration timestamp,"
                        + "PRIMARY KEY (storekey, path)"
                        + ");";
    }
    // @formatter:on

    @Inject
    private CassandraClient cassandraClient;

    private int maxResultSetSize; // limit the max size for REST endpoint getMissing to avoid OOM

    @Inject
    protected IndyConfiguration config;

    private String keyspace;

    private Session session;

    protected CassandraNotFoundCache()
    {
    }

    public CassandraNotFoundCache( final IndyConfiguration config )
    {
        this.config = config;
    }

    @PostConstruct
    public void start()
    {
        keyspace = config.getCacheKeyspace();
        maxResultSetSize = config.getNfcMaxResultSetSize();

        session = cassandraClient.getSession();

        session.execute( getSchemaCreateKeyspace( keyspace ) );
        session.execute( getSchemaCreateTable( keyspace ) );

        preparedExistQuery =
                        session.prepare( "SELECT count(*) FROM " + keyspace + ".nfc WHERE storekey=? and path=?;" );

        preparedCountByStore = session.prepare( "SELECT count(*) FROM " + keyspace + ".nfc WHERE storekey=?;" );

        preparedQueryByStore = session.prepare( "SELECT * FROM " + keyspace + ".nfc WHERE storekey=?;" );

        preparedDeleteByStore = session.prepare( "DELETE FROM " + keyspace + ".nfc WHERE storekey=?;" );

        preparedDelete = session.prepare( "DELETE FROM " + keyspace + ".nfc WHERE storekey=? AND path=?;" );

        preparedInsert = session.prepare( "INSERT INTO " + keyspace
                                                          + ".nfc (storekey,path,creation,expiration) VALUES (?,?,?,?) USING TTL ?;" ); // ttl in seconds
    }

    @Override
    protected IndyConfiguration getIndyConfiguration()
    {
        return config;
    }

    @Override
    @Measure
    public void addMissing( final ConcreteResource resource )
    {
        KeyedLocation location = (KeyedLocation) resource.getLocation();
        StoreKey key = location.getKey();

        int timeoutInSeconds = DEFAULT_NOT_FOUND_CACHE_TIMEOUT_SECONDS;
        int t = getTimeoutInSeconds( resource );
        if ( t > 0 )
        {
            timeoutInSeconds = t;
        }
        Date curDate = new Date();
        Date timeoutDate = new Date( curDate.getTime() + ( timeoutInSeconds * 1000 ) );
        logger.debug( "[NFC] {} will not be checked again until {}", resource,
                      new SimpleDateFormat( TIMEOUT_FORMAT ).format( timeoutDate ) );

        BoundStatement bound = preparedInsert.bind( key.toString(), resource.getPath(), curDate, timeoutDate,
                                                    timeoutInSeconds );
        session.execute( bound );
    }

    @Override
    @Measure
    public boolean isMissing( final ConcreteResource resource )
    {
        StoreKey key = getResourceKey( resource );
        BoundStatement bound = preparedExistQuery.bind( key.toString(), resource.getPath() );
        ResultSet result = session.execute( bound );
        long count = result.one().get( 0, Long.class );
        boolean missing = count > 0;
        logger.trace( "NFC check: {}, missing: {}", resource, missing );
        return missing;
    }

    @Override
    @Measure
    public void clearMissing( final Location location )
    {
        StoreKey key = ( (KeyedLocation) location ).getKey();
        BoundStatement bound = preparedDeleteByStore.bind( key.toString() );
        session.execute( bound );
    }

    @Override
    @Measure
    public void clearMissing( final ConcreteResource resource )
    {
        StoreKey key = getResourceKey( resource );
        BoundStatement bound = preparedDelete.bind( key.toString(), resource.getPath() );
        session.execute( bound );
    }

    @Override
    @Measure
    public void clearAllMissing()
    {
        session.execute( "TRUNCATE " + keyspace + ".nfc;" );
    }

    @Override
    @Measure
    public Map<Location, Set<String>> getAllMissing()
    {
        return Collections.emptyMap(); // not support
    }

    @Override
    @Measure
    public Set<String> getMissing( final Location location )
    {
        logger.debug( "[NFC] getMissing for {}", location );
        StoreKey key = ( (KeyedLocation) location ).getKey();
        BoundStatement bound = preparedQueryByStore.bind( key.toString() );
        ResultSet result = session.execute( bound );
        int count = 0;
        Set<String> matches = new HashSet<>();
        for ( Row row : result )
        {
            if ( maxResultSetSize > 0 && count >= maxResultSetSize )
            {
                logger.debug( "[NFC] getMissing reach maxResultSetSize: {}", maxResultSetSize );
                break;
            }
            count++;
            matches.add( row.get( "path", String.class ) );
        }
        logger.debug( "[NFC] getMissing complete, count: {}", matches.size() );
        return matches;
    }

    @Override
    @Measure
    public Map<Location, Set<String>> getAllMissing( int pageIndex, int pageSize )
    {
        return getAllMissing();
    }

    /**
     * Get missing entries via pagination.
     * @param location
     * @param pageIndex starts from 0
     * @param pageSize how many entries in each page
     * @return
     */
    @Override
    @Measure
    public Set<String> getMissing( Location location, int pageIndex, int pageSize )
    {
        return getMissing( location );
    }

    @Override
    @Measure
    public long getSize( StoreKey storeKey )
    {
        BoundStatement bound = preparedCountByStore.bind( storeKey.toString() );
        ResultSet result = session.execute( bound );
        return result.one().get( 0, Long.class );
    }

    @Override
    @Measure
    public long getSize()
    {
        return 0; // not support
    }

    private StoreKey getResourceKey( ConcreteResource resource )
    {
        KeyedLocation location = (KeyedLocation) resource.getLocation();
        return location.getKey();
    }
}
