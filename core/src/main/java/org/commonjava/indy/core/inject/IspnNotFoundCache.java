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

import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.model.galley.RepositoryLocation;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.infinispan.Cache;
import org.infinispan.query.Search;
import org.infinispan.query.dsl.Expression;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.commonjava.indy.model.core.StoreKey.fromString;

@ApplicationScoped
@Default
public class IspnNotFoundCache
                extends AbstractNotFoundCache
{

    private static final String TIMEOUT_FORMAT = "yyyy-MM-dd HH:mm:ss z";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    @NfcCache
    private CacheHandle<String, NfcConcreteResourceWrapper> nfcCache;

    private QueryFactory queryFactory;

    private int maxResultSetSize;

    // limit the max size for REST endpoint getMissing to avoid OOM

    @Inject
    protected IndyConfiguration config;

    protected IspnNotFoundCache()
    {
    }

    public IspnNotFoundCache( final IndyConfiguration config )
    {
        this.config = config;
    }

    @PostConstruct
    private void start()
    {
        queryFactory = Search.getQueryFactory( nfcCache.getCache() ); // Obtain a query factory for the cache
        maxResultSetSize = config.getNfcMaxResultSetSize();
    }

    private int getTimeoutInSeconds( ConcreteResource resource )
    {
        int timeoutInSeconds = config.getNotFoundCacheTimeoutSeconds();
        Location loc = resource.getLocation();
        Integer to = loc.getAttribute( RepositoryLocation.ATTR_NFC_TIMEOUT_SECONDS, Integer.class );
        if ( to != null && to > 0 )
        {
            timeoutInSeconds = to;
        }
        return timeoutInSeconds;
    }

    @Override
    public void addMissing( final ConcreteResource resource )
    {
        final int timeoutInSeconds = getTimeoutInSeconds( resource );
        long timeout = Long.MAX_VALUE;
        if ( timeoutInSeconds > 0 )
        {
            timeout = System.currentTimeMillis() + ( timeoutInSeconds * 1000 );
        }
        logger.debug( "[NFC] '{}' will not be checked again until {}", resource,
                      new SimpleDateFormat( TIMEOUT_FORMAT ).format( new Date( timeout ) ) );

        final String key = getResourceKey( resource );
        final long f_timeout = timeout;
        nfcCache.execute( cache -> cache.put( key, new NfcConcreteResourceWrapper( resource, f_timeout ), timeoutInSeconds,
                                              TimeUnit.SECONDS ) );
    }

    @Override
    public boolean isMissing( final ConcreteResource resource )
    {
        String key = getResourceKey( resource );
        NfcConcreteResourceWrapper obj = nfcCache.get( key );
        boolean missing = ( obj != null && obj.getTimeout() > System.currentTimeMillis() );
        if ( obj != null && missing )
        {
            nfcCache.remove( key );
        }
        logger.debug( "NFC check: {} result is: {}", resource, missing );
        return missing;
    }

    @Override
    public void clearMissing( final Location location )
    {
        Cache<String, NfcConcreteResourceWrapper> cache = nfcCache.getCache();
        cache.entrySet().stream().filter( e ->
                                          {
                                              StoreKey storeKey = fromString( e.getValue().getLocation() );
                                              return storeKey.equals( ( (KeyedLocation) location ).getKey() );
                                          } )
             .forEach( e -> cache.remove( e.getKey() ) );
    }

    @Override
    public void clearMissing( final ConcreteResource resource )
    {
        String key = getResourceKey( resource );
        nfcCache.execute( cache -> cache.remove( key ) );
    }

    @Override
    public void clearAllMissing()
    {
        nfcCache.getCache().clear();
    }

    @Override
    public Map<Location, Set<String>> getAllMissing()
    {
        logger.debug( "[NFC] getAllMissing start" );
        Map<Location, Set<String>> result = new HashMap<>();

        Query query = queryFactory.from( NfcConcreteResourceWrapper.class )
                                  .maxResults( maxResultSetSize )
                                  .build();

        List<NfcConcreteResourceWrapper> all = query.list();

        for ( NfcConcreteResourceWrapper entry : all )
        {
            String loc = entry.getLocation();
            StoreKey storeKey = fromString( loc );
            Set<String> paths = result.computeIfAbsent( new NfcKeyedLocation( storeKey ), k -> new HashSet<>() );
            paths.add( entry.getPath() );
        }
        logger.debug( "[NFC] getAllMissing complete, size: {}", all.size() );
        return result;
    }

    @Override
    public Set<String> getMissing( final Location location )
    {
        logger.debug( "[NFC] getMissing for {} start", location );
        Set<String> paths = new HashSet<>();

        Query query = queryFactory.from( NfcConcreteResourceWrapper.class )
                                  .maxResults( maxResultSetSize )
                                  .having( "location" )
                                  .eq( ( (KeyedLocation) location ).getKey().toString() )
                                  .toBuilder()
                                  .build();

        List<NfcConcreteResourceWrapper> matches = query.list();
        matches.forEach( resource -> paths.add( resource.getPath() ));

        logger.debug( "[NFC] getMissing complete, count: {}", matches.size() );
        return paths;
    }

    @Override
    public Map<Location, Set<String>> getAllMissing( int pageIndex, int pageSize )
    {
        logger.debug( "[NFC] getAllMissing start, pageIndex: {}, pageSize: {}", pageIndex, pageSize );
        Map<Location, Set<String>> result = new HashMap<>();

        pageSize = getProperPageSize(pageSize);
        long offset = pageIndex * pageSize;

        Query query = queryFactory.from( NfcConcreteResourceWrapper.class )
                                  .startOffset( offset )
                                  .maxResults( pageSize )
                                  .orderBy( "location" )
                                  .orderBy( "path" )
                                  .build();

        List<NfcConcreteResourceWrapper> all = query.list();

        for ( NfcConcreteResourceWrapper entry : all )
        {
            String loc = entry.getLocation();
            StoreKey storeKey = fromString( loc );
            Set<String> paths = result.computeIfAbsent( new NfcKeyedLocation( storeKey ), k -> new HashSet<>() );
            paths.add( entry.getPath() );
        }
        logger.debug( "[NFC] getAllMissing complete, size: {}", all.size() );
        return result;
    }

    /**
     * Get missing entries via pagination.
     * @param location
     * @param pageIndex starts from 0
     * @param pageSize how many entries in each page
     * @return
     */
    @Override
    public Set<String> getMissing( Location location, int pageIndex, int pageSize )
    {
        logger.debug( "[NFC] getMissing for {} start, pageIndex: {}, pageSize: {}", location, pageIndex, pageSize );
        Set<String> paths = new HashSet<>();

        pageSize = getProperPageSize(pageSize);
        long offset = pageIndex * pageSize;

        Query query = queryFactory.from( NfcConcreteResourceWrapper.class )
                                  .startOffset( offset )
                                  .maxResults( pageSize )
                                  .orderBy( "path" )
                                  .having( "location" )
                                  .eq( ( (KeyedLocation) location ).getKey().toString() )
                                  .toBuilder()
                                  .build();

        List<NfcConcreteResourceWrapper> matches = query.list();
        matches.forEach( resource -> paths.add( resource.getPath() ) );

        logger.debug( "[NFC] getMissing complete, count: {}", matches.size() );
        return paths;
    }

    @Override
    public long getSize( StoreKey storeKey )
    {
        Query query = queryFactory.from( NfcConcreteResourceWrapper.class )
                                  .select( Expression.count( "path" ) )
                                  .having( "location" )
                                  .eq( storeKey.toString() )
                                  .toBuilder()
                                  .build();

        List<Object> result = query.list();
        Object[] count = (Object[]) result.get( 0 );
        return (Long) count[0];
    }

    @Override
    public long getSize()
    {
        return nfcCache.getCache().size();
    }

    private int getProperPageSize( int pageSize )
    {
        if ( pageSize <= 0 || pageSize > maxResultSetSize )
        {
            logger.debug( "[NFC] Invalid pageSize {}, use default {}", pageSize, maxResultSetSize );
            pageSize = maxResultSetSize;
        }
        return pageSize;
    }

    private String getResourceKey( ConcreteResource resource )
    {
        KeyedLocation location = (KeyedLocation) resource.getLocation();
        StoreKey key = location.getKey();
        return md5Hex( key.toString() + ":" + resource.getPath() );
    }
}
