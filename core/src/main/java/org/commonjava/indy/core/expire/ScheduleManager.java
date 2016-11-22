/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.core.expire;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.action.BootupAction;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.ShutdownAction;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.core.conf.IndySchedulerConfig;
import org.commonjava.indy.core.expire.cache.ScheduleCache;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.spi.pkg.ContentAdvisor;
import org.commonjava.indy.spi.pkg.ContentQuality;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheKeyMatcher;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

@ApplicationScoped
public class ScheduleManager
        implements BootupAction, ShutdownAction
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final String PAYLOAD = "payload";

    public static final String ANY = "__ANY__";

    public static final String CONTENT_JOB_TYPE = "CONTENT";

    public static final String JOB_TYPE = "JOB_TYPE";

    @Inject
    private StoreDataManager dataManager;

    @Inject
    private IndyConfiguration config;

    @Inject
    private IndyObjectMapper objectMapper;

    @Inject
    private IndySchedulerConfig schedulerConfig;

    @Inject
    private SpecialPathManager specialPathManager;

    // This cache uses string key which format is "storeKey:jobtype:jobname", and the "storeKey:jobtype" is acted
    // like a "group" to group a bunch of the keys has same storekey and jobtype. The "jobname" is usually using
    // a path.
    @Inject
    @ScheduleCache
    private CacheHandle<String, Map> scheduleCache;

    // As all write operation is executed with lock, HashMap is thread-safe for this purpose
    //TODO: If need to support cluster, this should also be replaced with a ISPN cache
    private Map<String, Long> scheduleSetTimeCache = new HashMap<>();

    @Inject
    @Any
    private Instance<ContentAdvisor> contentAdvisor;

    @Override
    public void init()
            throws IndyLifecycleException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.info( "Scheduler disabled. Skipping initialization" );
            return;
        }

        final Properties props = new Properties();
        final Map<String, String> configuration = schedulerConfig.getConfiguration();
        if ( configuration != null )
        {
            props.putAll( configuration );
        }

        final StringBuilder sb = new StringBuilder();
        for ( final String key : props.stringPropertyNames() )
        {
            if ( sb.length() > 0 )
            {
                sb.append( '\n' );
            }

            sb.append( key ).append( " = " ).append( props.getProperty( key ) );
        }

        logger.info( "Scheduler properties:\n\n{}\n\n", sb );

    }

    public synchronized void rescheduleSnapshotTimeouts( final HostedRepository deploy )
            throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return;
        }

        int timeout = -1;
        if ( deploy.isAllowSnapshots() && deploy.getSnapshotTimeoutSeconds() > 0 )
        {
            timeout = deploy.getSnapshotTimeoutSeconds();
        }

        if ( timeout > 0 )
        {
            final Set<String> canceled =
                    cancelAllBefore( new StoreKeyMatcher( deploy.getKey(), CONTENT_JOB_TYPE ), timeout );

            for ( final String key : canceled )
            {
                final String path = getName( key );
                final StoreKey sk = storeKeyFrom( getGroup( key ) );

                scheduleContentExpiration( sk, path, timeout );
            }
        }
    }

    public synchronized void rescheduleProxyTimeouts( final RemoteRepository repo )
            throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return;
        }

        int timeout = -1;
        if ( !repo.isPassthrough() && repo.getCacheTimeoutSeconds() > 0 )
        {
            timeout = repo.getCacheTimeoutSeconds();
        }
        else if ( repo.isPassthrough() )
        {
            timeout = config.getPassthroughTimeoutSeconds();
        }

        if ( timeout > 0 )
        {
            final Set<String> canceled =
                    cancelAllBefore( new StoreKeyMatcher( repo.getKey(), CONTENT_JOB_TYPE ), timeout );
            for ( final String key : canceled )
            {
                final String path = getName( key );
                final StoreKey sk = storeKeyFrom( getGroup( key ) );

                scheduleContentExpiration( sk, path, timeout );
            }
        }
    }

    public synchronized void setProxyTimeouts( final StoreKey key, final String path )
            throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return;
        }

        RemoteRepository repo = null;
        try
        {
            repo = (RemoteRepository) dataManager.getArtifactStore( key );
        }
        catch ( final IndyDataException e )
        {
            logger.error( String.format( "Failed to retrieve store for: %s. Reason: %s", key, e.getMessage() ), e );
        }

        if ( repo == null )
        {
            return;
        }

        int timeout = config.getPassthroughTimeoutSeconds();
        final ConcreteResource resource = new ConcreteResource( LocationUtils.toLocation( repo ), path );
        final SpecialPathInfo info = specialPathManager.getSpecialPathInfo( resource );
        if ( !repo.isPassthrough() )
        {
            if ( ( info != null && info.isMetadata() ) && repo.getMetadataTimeoutSeconds() > 0 )
            {
                logger.debug( "Using metadata timeout for: {}", resource );
                timeout = repo.getMetadataTimeoutSeconds();
            }
            else
            {
                if ( info == null )
                {
                    logger.debug( "No special path info for: {}", resource );
                }
                else
                {
                    logger.debug( "{} is a special path, but not metadata.", resource );
                }

                timeout = repo.getCacheTimeoutSeconds();
            }
        }

        if ( timeout > 0 )
        {
            //            logger.info( "[PROXY TIMEOUT SET] {}/{}; {}", repo.getKey(), path, new Date( System.currentTimeMillis()
            //                + timeout ) );
            cancel( new StoreKeyMatcher( key, CONTENT_JOB_TYPE ), path );

            scheduleContentExpiration( key, path, timeout );
        }
    }

    public synchronized void scheduleForStore( final StoreKey key, final String jobType, final String jobName,
                                               final Object payload, final int startSeconds, final int repeatSeconds )
            throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return;
        }

        final Map<String, String> dataMap = new HashMap<>( 2 );
        dataMap.put( JOB_TYPE, jobType );
        try
        {
            dataMap.put( PAYLOAD, objectMapper.writeValueAsString( payload ) );
        }
        catch ( final JsonProcessingException e )
        {
            throw new IndySchedulerException( "Failed to serialize JSON payload: " + payload, e );
        }

        final String cacheKey = cacheKey( key, jobType, jobName );

        // Here has some confusion: seems that only lifespan setting can not trigger the expire event, so the maxIdleTime
        // is also set here. Not sure why ISPN do it this way. See:
        // http://infinispan.org/docs/stable/faqs/faqs.html#eviction_and_expiration_questions
        scheduleCache.execute( cache -> cache.put( cacheKey, dataMap, startSeconds, TimeUnit.SECONDS, startSeconds,
                                                   TimeUnit.SECONDS ) );
        scheduleSetTimeCache.put( cacheKey, System.currentTimeMillis() );
    }

    public synchronized void scheduleContentExpiration( final StoreKey key, final String path,
                                                        final int timeoutSeconds )
            throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return;
        }

        logger.info( "Scheduling timeout for: {} in: {} in: {} seconds (at: {}).", path, key, timeoutSeconds,
                     new Date( System.currentTimeMillis() + ( timeoutSeconds * 1000 ) ) );

        scheduleForStore( key, CONTENT_JOB_TYPE, path, new ContentExpiration( key, path ), timeoutSeconds, -1 );
    }

    public synchronized void setSnapshotTimeouts( final StoreKey key, final String path )
            throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return;
        }

        HostedRepository deploy = null;
        try
        {
            final ArtifactStore store = dataManager.getArtifactStore( key );
            if ( store == null )
            {
                return;
            }

            if ( store instanceof HostedRepository )
            {
                deploy = (HostedRepository) store;
            }
            else if ( store instanceof Group )
            {
                final Group group = (Group) store;
                deploy = findDeployPoint( group );
            }
        }
        catch ( final IndyDataException e )
        {
            logger.error( String.format( "Failed to retrieve deploy point for: %s. Reason: %s", key, e.getMessage() ),
                          e );
        }

        if ( deploy == null )
        {
            return;
        }

        final ContentAdvisor advisor = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize( contentAdvisor.iterator(), Spliterator.ORDERED ), false )
                                                    .filter( Objects::nonNull )
                                                    .findFirst()
                                                    .orElse( null );
        final ContentQuality quality = advisor == null ? null : advisor.getContentQuality( path );
        if ( quality == null )
        {
            return;
        }

        if ( ContentQuality.SNAPSHOT == quality && deploy.getSnapshotTimeoutSeconds() > 0 )
        {
            final int timeout = deploy.getSnapshotTimeoutSeconds();

            //            //            logger.info( "[SNAPSHOT TIMEOUT SET] {}/{}; {}", deploy.getKey(), path, new Date( timeout ) );
            //            cancel( new StoreKeyMatcher( key, CONTENT_JOB_TYPE ), path );

            scheduleContentExpiration( key, path, timeout );
        }
    }

    private HostedRepository findDeployPoint( final Group group )
            throws IndyDataException
    {
        for ( final StoreKey key : group.getConstituents() )
        {
            if ( StoreType.hosted == key.getType() )
            {
                return dataManager.getHostedRepository( key.getName() );
            }
            else if ( StoreType.group == key.getType() )
            {
                final Group grp = dataManager.getGroup( key.getName() );
                final HostedRepository dp = findDeployPoint( grp );
                if ( dp != null )
                {
                    return dp;
                }
            }
        }

        return null;
    }

    public synchronized Set<String> cancelAllBefore( final CacheKeyMatcher<String> matcher, final long timeout )
            throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return Collections.emptySet();
        }

        final Set<String> canceled = new HashSet<>();

        final Date to = new Date( System.currentTimeMillis() + ( timeout * 1000 ) );
        matcher.matches( scheduleCache ).forEach( key -> {
            final Date nextFire = getNextExpireTime( key );
            if ( nextFire == null || !nextFire.after( to ) )
            {
                // former impl uses quartz and here use the unscheduleJob method, but ISPN does not have similar
                // op, so directly did a remove here.
                removeCache( key );
                canceled.add( key );
            }
        } );

        return canceled;
    }

    public synchronized Set<String> cancelAll( final CacheKeyMatcher<String> matcher )
            throws IndySchedulerException
    {
        return cancel( matcher, ANY );
    }

    public synchronized Set<String> cancel( final CacheKeyMatcher<String> matcher, final String name )
            throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return Collections.emptySet();
        }

        Set<String> canceled = new HashSet<>();
        final Set<String> keys = matcher.matches( scheduleCache );
        if ( keys != null && !keys.isEmpty() )
        {
            Set<String> unscheduled = null;
            if ( name == ANY )
            {
                for ( final String k : keys )
                {
                    removeCache( k );
                }
                unscheduled = keys;
            }
            else
            {
                for ( final String k : keys )
                {
                    if ( k.endsWith( name ) )
                    {
                        removeCache( k );
                        unscheduled = Collections.singleton( k );
                        break;
                    }
                }
            }

            if ( unscheduled != null )
            {
                canceled = unscheduled;
            }
        }

        return canceled;
    }

    public synchronized Expiration findSingleExpiration( final StoreKeyMatcher matcher )
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return null;
        }

        final Set<String> keys = matcher.matches( scheduleCache );
        if ( keys != null && !keys.isEmpty() )
        {
            String triggerKey = keys.iterator().next();
            return toExpiration( triggerKey );
        }

        return null;
    }

    public synchronized ExpirationSet findMatchingExpirations( final CacheKeyMatcher<String> matcher )
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return null;
        }

        final Set<String> keys = matcher.matches( scheduleCache );
        Set<Expiration> expirations = new HashSet<>( keys.size() );
        if ( keys != null && !keys.isEmpty() )
        {
            for ( String key : keys )
            {
                expirations.add( toExpiration( key ) );
            }
        }

        return new ExpirationSet( expirations );
    }

    private Expiration toExpiration( final String cacheKey )
    {
        return new Expiration( getGroup( cacheKey ), getName( cacheKey ), getNextExpireTime( cacheKey ) );
    }

    private Date getNextExpireTime( final String cacheKey )
    {

        return scheduleCache.execute( cache -> {
            final CacheEntry entry = cache.getAdvancedCache().getCacheEntry( cacheKey );
            if ( entry != null )
            {
                final Metadata metadata = entry.getMetadata();
                long expire = metadata.lifespan();
                final long startTimeInMillis = scheduleSetTimeCache.get( cacheKey );
                return calculateNextExpireTime( expire, startTimeInMillis );
            }
            return null;
        } );

    }

    static Date calculateNextExpireTime( final long expire, final long start )
    {
        if ( expire > 1 )
        {
            final long duration = System.currentTimeMillis() - start;
            if ( duration < expire )
            {
                final long nextTimeInMillis = expire - duration + System.currentTimeMillis();
                final LocalDateTime time =
                        Instant.ofEpochMilli( nextTimeInMillis ).atZone( ZoneId.systemDefault() ).toLocalDateTime();
                return Date.from( time.atZone( ZoneId.systemDefault() ).toInstant() );
            }
        }
        return null;
    }

    public synchronized String findFirstMatchingTrigger( final CacheKeyMatcher<String> matcher )
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return null;
        }

        final Set<String> keys = matcher.matches( scheduleCache );
        if ( keys != null && !keys.isEmpty() )
        {
            return keys.iterator().next();
        }

        return null;
    }

    public static String groupName( final StoreKey key, final String jobType )
    {
        return key.toString() + groupNameSuffix( jobType );
    }

    public static String groupNameSuffix( final String jobType )
    {
        return ":" + jobType;
    }

    public static StoreKey storeKeyFrom( final String group )
    {
        final String[] parts = group.split( ":" );
        if ( parts.length > 1 )
        {
            final StoreType type = StoreType.get( parts[0] );
            if ( type != null )
            {
                return new StoreKey( type, parts[1] );
            }
        }

        return null;
    }

    public synchronized boolean deleteJob( final String group, final String name )
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return false;
        }

        final String cacheKey = cacheKey( group, name );
        if ( scheduleCache.containsKey( cacheKey ) )
        {
            removeCache( cacheKey );
            return true;
        }

        return false;
    }

    @Override
    public String getId()
    {
        return "Indy Scheduler";
    }

    @Override
    public int getBootPriority()
    {
        return 80;
    }

    @Override
    public int getShutdownPriority()
    {
        return 95;
    }

    @Override
    public void stop()
            throws IndyLifecycleException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return;
        }

        scheduleCache.stop();
    }

    public static String getName( final String cacheKey )
    {
        if ( StringUtils.isNotBlank( cacheKey ) )
        {
            String[] keyParts = cacheKey.split( ":" );
            return keyParts[2];
        }
        return "";
    }

    public static String getGroup( final String cacheKey )
    {
        if ( StringUtils.isNotBlank( cacheKey ) )
        {
            String[] keyParts = cacheKey.split( ":" );
            return keyParts[0] + ":" + keyParts[1];
        }
        return "";
    }

    private String cacheKey( final String group, final String name )
    {
        return group + ":" + name;
    }

    private String cacheKey( final StoreKey key, final String jobType, final String name )
    {
        return cacheKey( groupName( key, jobType ), name );
    }

    private synchronized void removeCache( final String cacheKey )
    {
        if ( scheduleCache.containsKey( cacheKey ) )
        {
            scheduleCache.remove( cacheKey );
        }
        if ( scheduleSetTimeCache.containsKey( cacheKey ) )
        {
            scheduleSetTimeCache.remove( cacheKey );
        }
    }

}
