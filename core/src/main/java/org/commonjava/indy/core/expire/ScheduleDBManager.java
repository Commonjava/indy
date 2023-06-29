/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.ShutdownAction;
import org.commonjava.indy.cluster.LocalIndyNodeProvider;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.core.conf.IndySchedulerConfig;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.commonjava.indy.schedule.ScheduleDB;
import org.commonjava.indy.schedule.datastax.JobType;
import org.commonjava.indy.schedule.datastax.model.DtxSchedule;
import org.commonjava.indy.spi.pkg.ContentAdvisor;
import org.commonjava.indy.spi.pkg.ContentQuality;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import static org.commonjava.indy.core.change.StoreEnablementManager.DISABLE_TIMEOUT;
import static org.commonjava.indy.core.change.StoreEnablementManager.TIMEOUT_USE_DEFAULT;

/**
 * A ScheduleDBManager is used to do the schedule time out jobs for the {@link ArtifactStore} to do some time-related jobs, like
 * removing useless artifacts. It used Cassandra to store the schedule info and regularly to check the status to implement this
 * type of function.
 */
@SuppressWarnings( "RedundantThrows" )
@ApplicationScoped
@ClusterScheduleManager
public class ScheduleDBManager
        implements ScheduleManager, ShutdownAction
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final String ANY = "__ANY__";

    @Inject
    private StoreDataManager dataManager;

    @Inject
    private IndyConfiguration config;

    @Inject
    private IndyObjectMapper objectMapper;

    @Inject
    private SpecialPathManager specialPathManager;

    @Inject
    @Any
    private Instance<ContentAdvisor> contentAdvisor;

    @Inject
    private Event<SchedulerEvent> eventDispatcher;

    @Inject
    private LocalIndyNodeProvider nodeHolder;

    @Inject
    private ScheduleDB scheduleDB;

    @Inject
    private IndySchedulerConfig schedulerConfig;

    @PostConstruct
    public void init()
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.info( "Scheduler disabled. Skipping initialization" );
            return;
        }
    }

    public void rescheduleSnapshotTimeouts( final HostedRepository deploy )
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

            final Collection<DtxSchedule> schedules = scheduleDB.querySchedules( deploy.getKey().toString(), JobType.CONTENT.getJobType(), Boolean.FALSE );

            final Set<DtxSchedule> rescheduled = rescheduleAllBefore( schedules, timeout );

            for ( DtxSchedule schedule : rescheduled )
            {
                scheduleContentExpiration( StoreKey.fromString( schedule.getStoreKey() ), schedule.getJobName(), timeout );
            }
        }
    }

    public void rescheduleProxyTimeouts( final RemoteRepository repo )
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

            final Collection<DtxSchedule> schedules =
                            scheduleDB.querySchedules( repo.getKey().toString(), JobType.CONTENT.getJobType(),
                                                       Boolean.FALSE );

            final Set<DtxSchedule> rescheduled = rescheduleAllBefore( schedules, timeout );

            for ( DtxSchedule schedule : rescheduled )
            {
                scheduleContentExpiration( StoreKey.fromString( schedule.getStoreKey() ), schedule.getJobName(), timeout );
            }

        }
    }

    public void setProxyTimeouts( final StoreKey key, final String path )
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
        final SpecialPathInfo info = specialPathManager.getSpecialPathInfo( resource, key.getPackageType() );
        if ( !repo.isPassthrough() )
        {
            if ( ( info != null && info.isMetadata() ) && repo.getMetadataTimeoutSeconds() >= 0 )
            {
                if ( repo.getMetadataTimeoutSeconds() == 0 )
                {
                    logger.debug( "Using default metadata timeout for: {}", resource );
                    timeout = config.getRemoteMetadataTimeoutSeconds();
                }
                else
                {
                    logger.debug( "Using metadata timeout for: {}", resource );
                    timeout = repo.getMetadataTimeoutSeconds();
                }
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

            scheduleContentExpiration( key, path, timeout );
        }
    }

    public void scheduleForStore( final StoreKey key, final String jobType, final String jobName,
                                               final Object payload, final int startSeconds )
            throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return;
        }

        String payloadStr;
        try
        {
            payloadStr = objectMapper.writeValueAsString( payload ) ;
        }
        catch ( final JsonProcessingException e )
        {
            throw new IndySchedulerException( "Failed to serialize JSON payload: " + payload, e );
        }

        scheduleDB.createSchedule( key.toString(), jobType, jobName, payloadStr, Long.valueOf( startSeconds ) );
        logger.debug( "Scheduled for the key {} with timeout: {} seconds", key, startSeconds );
    }

    public void scheduleContentExpiration( final StoreKey key, final String path,
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

        scheduleForStore( key, JobType.CONTENT.getJobType(), path, new ContentExpiration( key, path ), timeoutSeconds );
    }

    public void setSnapshotTimeouts( final StoreKey key, final String path )
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

    public void rescheduleDisableTimeout( final StoreKey key )
            throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return;
        }

        ArtifactStore store = null;
        try
        {
            store = dataManager.getArtifactStore( key );
        }
        catch ( final IndyDataException e )
        {
            logger.error( String.format( "Failed to retrieve store for: %s. Reason: %s", key, e.getMessage() ), e );
        }

        if ( store == null )
        {
            return;
        }

        int timeout = store.getDisableTimeout();
        if ( timeout == TIMEOUT_USE_DEFAULT )
        {
            // case TIMEOUT_USE_DEFAULT: will use default timeout configuration
            timeout = config.getStoreDisableTimeoutSeconds();
        }

        if ( timeout > TIMEOUT_USE_DEFAULT && store.isDisabled() )
        {
            final StoreKey sk = store.getKey();
            logger.debug( "Set/Reschedule disable timeout for store:{}", sk );
            scheduleForStore( sk, DISABLE_TIMEOUT, sk.toString() + "#" + DISABLE_TIMEOUT, sk, timeout );
        }
    }

    private HostedRepository findDeployPoint( final Group group )
            throws IndyDataException
    {
        for ( final StoreKey key : group.getConstituents() )
        {
            if ( StoreType.hosted == key.getType() )
            {
                return (HostedRepository) dataManager.getArtifactStore( key );
            }
            else if ( StoreType.group == key.getType() )
            {
                final Group grp = (Group) dataManager.getArtifactStore( key );
                final HostedRepository dp = findDeployPoint( grp );
                if ( dp != null )
                {
                    return dp;
                }
            }
        }

        return null;
    }

    public Set<DtxSchedule> rescheduleAllBefore( final Collection<DtxSchedule> schedules, final long timeout )
    {

        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return Collections.emptySet();
        }

        final Set<DtxSchedule> rescheduled = new HashSet<>();

        final Date to = new Date( System.currentTimeMillis() + ( timeout * 1000 ) );
        schedules.forEach( schedule -> {
            final Date nextFire = getNextExpireTime( schedule );
            if ( nextFire == null || !nextFire.after( to ) )
            {
                rescheduled.add( schedule );
            }
        } );

        return rescheduled;

    }

    public Expiration findSingleExpiration( final StoreKey key, final String jobType )
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return null;
        }

        final Collection<DtxSchedule> schedules =
                        scheduleDB.querySchedules( key.toString(), jobType,
                                                   Boolean.FALSE );

        if ( schedules != null && !schedules.isEmpty() )
        {
            DtxSchedule schedule = schedules.iterator().next();
            return toExpiration( schedule );

        }

        return null;
    }

    @Override
    public ExpirationSet findMatchingExpirations( String jobType )
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return null;
        }

        final Collection<DtxSchedule> schedules = scheduleDB.querySchedulesByJobType( jobType );
        Set<Expiration> expirations = new HashSet<>( schedules.size() );
        if ( !schedules.isEmpty() )
        {
            for ( DtxSchedule schedule : schedules )
            {
                expirations.add( toExpiration( schedule ) );
            }
        }

        return new ExpirationSet( expirations );
    }

    public ExpirationSet findMatchingExpirations( final StoreKey key, final String jobType )
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return null;
        }

        final Collection<DtxSchedule> schedules = scheduleDB.querySchedules( key.toString(), jobType, Boolean.FALSE );
        Set<Expiration> expirations = new HashSet<>( schedules.size() );
        if ( !schedules.isEmpty() )
        {
            for ( DtxSchedule schedule : schedules )
            {
                expirations.add( toExpiration( schedule ) );
            }
        }

        return new ExpirationSet( expirations );
    }

    private Expiration toExpiration( final DtxSchedule schedule )
    {
        return new Expiration( ScheduleDBManager.groupName( StoreKey.fromString( schedule.getStoreKey() ),
                                                            schedule.getJobType() ), schedule.getJobName(),
                               getNextExpireTime( schedule ) );
    }

    private Date getNextExpireTime( final DtxSchedule dtxSchedule )
    {

        if ( !dtxSchedule.getExpired() )
        {
            Long lifespan = dtxSchedule.getLifespan();
            final long startTimeInMillis = dtxSchedule.getScheduleTime().getTime();
            return calculateNextExpireTime( lifespan.longValue(), startTimeInMillis );
        }

        return null;
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

    public static String groupName( final StoreKey key, final String jobType )
    {
        return key.toString() + groupNameSuffix( jobType );
    }

    public static String groupNameSuffix( final String jobType )
    {
        return "#" + jobType;
    }

    public static StoreKey storeKeyFrom( final String group )
    {
        final String[] parts = group.split( "#" );
        if ( parts.length > 1 )
        {
            final Logger logger = LoggerFactory.getLogger( ScheduleDBManager.class );
            StoreKey storeKey = null;
            try
            {
                storeKey = StoreKey.fromString( parts[0] );
            }
            catch ( IllegalArgumentException e )
            {
                logger.warn( "Not a store key for string: {}", parts[0] );
            }

            //TODO this part of code may be obsolete, will need further check then remove
            if ( storeKey == null )
            {
                logger.info( "Not a store key for string: {}, will parse as store type", parts[0] );
                final StoreType type = StoreType.get( parts[0] );
                if ( type != null )
                {
                    storeKey = new StoreKey( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, type, parts[1] );
                }
            }
            return storeKey;
        }

        return null;
    }

    @Override
    public String getId()
    {
        return "Indy ScheduleDB";
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

    }

}
