/**
 * Copyright (C) 2011-2017 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.model.galley.RepositoryLocation;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.commonjava.maven.galley.util.PathUtils.normalize;

@ApplicationScoped
@Default
public class DBNotFoundCache
                implements NotFoundCache
{

    private static final String TIMEOUT_FORMAT = "yyyy-MM-dd hh:mm:ss z";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    protected IndyConfiguration config;

    private static final String DB_DRIVER = "org.h2.Driver";

    private static final String DB_USER = "";

    private static final String DB_PASSWORD = "";

    private final ScheduledExecutorService evictionService = Executors.newScheduledThreadPool( 1 );

    protected DBNotFoundCache()
    {
    }

    public DBNotFoundCache( final IndyConfiguration config ) throws Exception
    {
        this.config = config;
        this.init();
    }

    // limit the max result set for endpoint getAllMissing to avoid OOM
    private final int MAX_GET_MISSING_RESULT_SIZE = 100000;

    private static String DB_CONNECTION;

    /* @formatter:off */
    private final String CREATE_TABLE = "CREATE TABLE nfc ("
                    + "id BIGINT auto_increment primary key, "
                    + "resource VARCHAR(128), "
                    + "location VARCHAR(256), "
                    + "path VARCHAR(512), "
                    + "timeout BIGINT"
                    + ")";
    /* @formatter:on */

    private final String DELETE_ALL = "DELETE FROM nfc";

    private final String DELETE_BY_TIMEOUT = "DELETE FROM nfc WHERE timeout<?";

    private final String DELETE_BY_RES = "DELETE FROM nfc WHERE resource=?";

    private final String DELETE_BY_LOCATION = "DELETE FROM nfc WHERE location=?";

    private final String SELECT_BY_RES = "SELECT * FROM nfc WHERE resource=?";

    private final String SELECT_BY_LOCATION = "SELECT * FROM nfc WHERE location=? LIMIT " + MAX_GET_MISSING_RESULT_SIZE;

    private final String SELECT = "SELECT * FROM nfc LIMIT " + MAX_GET_MISSING_RESULT_SIZE;

    private final String INSERT = "INSERT INTO nfc (resource, location, path, timeout) values (?,?,?,?)";

    @PostConstruct
    private void init() throws Exception
    {
        boolean createTable = false;
        String dir = config.getNotFoundCacheDataDir();
        File f = new File( dir );
        if ( !f.isDirectory() )
        {
            boolean success = f.mkdirs();
            if ( !success )
            {
                throw new Exception( "Create NFC data dir failed, dir: " + dir );
            }
            createTable = true;
        }
        DB_CONNECTION = "jdbc:h2:" + f.getAbsolutePath() + "/nfc.h2";

        if ( createTable )
        {
            Connection connection = getDBConnection();
            try
            {
                Statement stmt = connection.createStatement();
                stmt.execute( CREATE_TABLE );
                stmt.close();
            }
            finally
            {
                connection.close();
            }
        }

        evictionService.scheduleAtFixedRate( () -> clearAllExpiredMissing(), 30, 30, TimeUnit.MINUTES );
    }

    private Connection getDBConnection()
    {
        try
        {
            Class.forName( DB_DRIVER );
        }
        catch ( ClassNotFoundException e )
        {
            logger.error( "Can not initiate H2 driver", e );
            return null;
        }

        Connection dbConnection = null;
        try
        {
            dbConnection = DriverManager.getConnection( DB_CONNECTION, DB_USER, DB_PASSWORD );
        }
        catch ( SQLException e )
        {
            logger.error( "Can not create H2 connection", e );
        }
        return dbConnection;
    }

    private long getTimeout( ConcreteResource resource )
    {
        long timeout = Long.MAX_VALUE;
        if ( config.getNotFoundCacheTimeoutSeconds() > 0 )
        {
            timeout = System.currentTimeMillis() + config.getNotFoundCacheTimeoutSeconds() * 1000;
        }

        final Location loc = resource.getLocation();
        final Integer to = loc.getAttribute( RepositoryLocation.ATTR_NFC_TIMEOUT_SECONDS, Integer.class );
        if ( to != null && to > 0 )
        {
            timeout = System.currentTimeMillis() + ( to * 1000 );
        }
        return timeout;
    }

    @Override
    public void addMissing( final ConcreteResource resource )
    {
        final long timeout = getTimeout( resource );

        if ( logger.isDebugEnabled() )
        {
            logger.debug( "[NFC] '{}' will not be checked again until: {}",
                          normalize( resource.getLocationUri(), resource.getPath() ),
                          new SimpleDateFormat( TIMEOUT_FORMAT ).format( new Date( timeout ) ) );
        }
        putToCache( resource, timeout );
    }

    private String getStoreKey( Location location )
    {
        return ( (KeyedLocation) location ).getKey().toString(); // shouldn't fail the cast
    }

    // MD5 hash length 32 hex digits
    private String getResourceKey( ConcreteResource resource )
    {
        return DigestUtils.md5Hex( normalize( getStoreKey( resource.getLocation() ), resource.getPath() ) );
    }

    private void putToCache( ConcreteResource resource, long timeout )
    {
        String key = getResourceKey( resource );
        Connection connection = getDBConnection();
        try
        {
            PreparedStatement stmt = connection.prepareStatement( INSERT );
            stmt.setString( 1, key );
            stmt.setString( 2, getStoreKey( resource.getLocation() ) );
            stmt.setString( 3, resource.getPath() );
            stmt.setLong( 4, timeout );
            stmt.executeUpdate();
            stmt.close();
        }
        catch ( SQLException e )
        {
            logger.warn( "[NFC] clearByLocation failed", e );
        }
        finally
        {
            closeQuietly( connection );
        }
    }

    @Override
    public boolean isMissing( final ConcreteResource resource )
    {
        final Long timeout = getFromCache( resource );
        boolean result = false;
        if ( timeout != null && System.currentTimeMillis() < timeout )
        {
            result = true;
        }

        logger.debug( "NFC check: {} result is: {}", resource, result );
        return result;
    }

    private Long getFromCache( ConcreteResource resource )
    {
        Long timeout = null;
        String key = getResourceKey( resource );
        Connection connection = getDBConnection();
        try
        {
            PreparedStatement stmt = connection.prepareStatement( SELECT_BY_RES );
            stmt.setString( 1, key );
            ResultSet rs = stmt.executeQuery();
            while ( rs.next() )
            {
                timeout = rs.getLong( "timeout" );
            }
            stmt.close();
        }
        catch ( SQLException e )
        {
            logger.warn( "[NFC] getFromCache failed", e );
        }
        finally
        {
            closeQuietly( connection );
        }
        return timeout;
    }

    @Override
    public void clearMissing( final Location location )
    {
        clearByLocation( location );
    }

    private void clearByLocation( Location location )
    {
        Connection connection = getDBConnection();
        try
        {
            PreparedStatement stmt = connection.prepareStatement( DELETE_BY_LOCATION );
            stmt.setString( 1, getStoreKey( location ) );
            stmt.executeUpdate();
            stmt.close();
        }
        catch ( SQLException e )
        {
            logger.warn( "[NFC] clearByLocation failed", e );
        }
        finally
        {
            closeQuietly( connection );
        }
    }

    @Override
    public void clearMissing( final ConcreteResource resource )
    {
        clearByResource( resource );
    }

    private void clearByResource( ConcreteResource resource )
    {
        String key = getResourceKey( resource );
        Connection connection = getDBConnection();
        try
        {
            PreparedStatement stmt = connection.prepareStatement( DELETE_BY_RES );
            stmt.setString( 1, key );
            stmt.executeUpdate();
            stmt.close();
        }
        catch ( SQLException e )
        {
            logger.warn( "[NFC] clearByResource failed", e );
        }
        finally
        {
            closeQuietly( connection );
        }
    }

    @Override
    public void clearAllMissing()
    {
        Connection connection = getDBConnection();
        try
        {
            Statement stmt = connection.createStatement();
            stmt.execute( DELETE_ALL );
            stmt.close();
        }
        catch ( SQLException e )
        {
            logger.warn( "[NFC] clearAllMissing failed", e );
        }
        finally
        {
            closeQuietly( connection );
        }
    }

    private void closeQuietly( Connection connection )
    {
        try
        {
            connection.close();
        }
        catch ( SQLException e )
        {
            logger.warn( "[NFC] Close connection failed", e );
        }
    }

    @Override
    public Map<Location, Set<String>> getAllMissing()
    {
        clearAllExpiredMissing();

        final Map<Location, Set<String>> result = new HashMap<>();
        Connection connection = getDBConnection();
        try
        {
            int count = 0;
            PreparedStatement stmt = connection.prepareStatement( SELECT );
            ResultSet rs = stmt.executeQuery();
            while ( rs.next() )
            {
                count++;
                if ( count <= MAX_GET_MISSING_RESULT_SIZE )
                {
                    String location = rs.getString( "location" );
                    Location loc = getLocation( location );
                    Set<String> paths = result.computeIfAbsent( loc, k -> new HashSet<>() );
                    paths.add( rs.getString( "path" ) );
                }
                else
                {
                    logger.info( "NfcCache size is too large, only return {} records", count );
                    break;
                }
            }
            stmt.close();
        }
        catch ( SQLException e )
        {
            logger.warn( "[NFC] getFromCache failed", e );
        }
        finally
        {
            closeQuietly( connection );
        }

        return result;
    }

    private Location getLocation( String location )
    {
        KeyedLocation keyedLoc = new DummyKeyedLocation( StoreKey.fromString( location ) );
        return keyedLoc;
    }

    @Override
    public Set<String> getMissing( final Location location )
    {
        clearAllExpiredMissing();

        final Set<String> paths = new HashSet<>();

        Connection connection = getDBConnection();
        try
        {
            int count = 0;
            PreparedStatement stmt = connection.prepareStatement( SELECT_BY_LOCATION );
            stmt.setString( 1, getStoreKey( location ) );
            ResultSet rs = stmt.executeQuery();
            while ( rs.next() )
            {
                count++;
                if ( count <= MAX_GET_MISSING_RESULT_SIZE )
                {
                    paths.add( rs.getString( "path" ) );
                }
                else
                {
                    logger.info( "NfcCache size for {} is too large, only return {} records", location, count );
                    break;
                }
            }
            stmt.close();
        }
        catch ( SQLException e )
        {
            logger.warn( "[NFC] getFromCache failed", e );
        }
        finally
        {
            closeQuietly( connection );
        }

        return paths;
    }

    private synchronized void clearAllExpiredMissing()
    {
        Connection connection = getDBConnection();
        try
        {
            PreparedStatement stmt = connection.prepareStatement( DELETE_BY_TIMEOUT );
            stmt.setLong( 1, System.currentTimeMillis() );
            stmt.executeUpdate();
            stmt.close();
        }
        catch ( SQLException e )
        {
            logger.warn( "[NFC] clearAllExpiredMissing failed", e );
        }
        finally
        {
            closeQuietly( connection );
        }
    }

    // NfcController only use storeKey
    private class DummyKeyedLocation
                    implements KeyedLocation
    {

        private final StoreKey storeKey;

        public DummyKeyedLocation( StoreKey key )
        {
            this.storeKey = key;
        }

        @Override
        public StoreKey getKey()
        {
            return storeKey;
        }

        @Override
        public boolean allowsDownloading()
        {
            return false;
        }

        @Override
        public boolean allowsPublishing()
        {
            return false;
        }

        @Override
        public boolean allowsStoring()
        {
            return false;
        }

        @Override
        public boolean allowsSnapshots()
        {
            return false;
        }

        @Override
        public boolean allowsReleases()
        {
            return false;
        }

        @Override
        public boolean allowsDeletion()
        {
            return false;
        }

        @Override
        public String getUri()
        {
            return null;
        }

        @Override
        public String getName()
        {
            return null;
        }

        @Override
        public Map<String, Object> getAttributes()
        {
            return null;
        }

        @Override
        public <T> T getAttribute( String key, Class<T> type )
        {
            return null;
        }

        @Override
        public <T> T getAttribute( String key, Class<T> type, T defaultValue )
        {
            return null;
        }

        @Override
        public Object removeAttribute( String key )
        {
            return null;
        }

        @Override
        public Object setAttribute( String key, Object value )
        {
            return null;
        }
    }
}
