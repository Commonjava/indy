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
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.action.BootupAction;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.ShutdownAction;
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
import org.commonjava.indy.spi.pkg.ContentAdvisor;
import org.commonjava.indy.spi.pkg.ContentQuality;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.Executor;
import java.util.stream.StreamSupport;

@ApplicationScoped
public class ScheduleManager
    implements BootupAction, ShutdownAction
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final String PAYLOAD = "payload";

    private static final String ANY = "__ANY__";

    public static final String CONTENT_JOB_TYPE = "CONTENT";

    public static final String STORE_JOB_TYPE = "STORE";

    public static final String JOB_TYPE = "JOB_TYPE";

    @Inject
    @WeftManaged
    @ExecutorConfig( daemon = true, priority = 7, named = "indy-events" )
    private Executor executor;

    @Inject
    private StoreDataManager dataManager;

    @Inject
    private IndyConfiguration config;

    @Inject
    private IndyObjectMapper objectMapper;

    @Inject
    private IndySchedulerConfig schedulerConfig;

    @Inject
    private Event<SchedulerEvent> eventDispatcher;

    @Inject
    private SpecialPathManager specialPathManager;

    private Scheduler scheduler;

    @Inject
    @Any
    private Instance<ContentAdvisor> contentAdvisor;

    @Override
    public void init()
        throws IndyLifecycleException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.info( "Scheduler disabled. Skipping Quartz initialization" );
            return;
        }

        try
        {
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

                sb.append( key )
                  .append( " = " )
                  .append( props.getProperty( key ) );
            }

            logger.info( "Scheduler properties:\n\n{}\n\n", sb );

            final StdSchedulerFactory fac = new StdSchedulerFactory( props );
            scheduler = fac.getScheduler();

            if ( eventDispatcher != null )
            {
                scheduler.getListenerManager()
                         .addSchedulerListener( new IndyScheduleListener( scheduler, eventDispatcher ) );

                scheduler.getListenerManager()
                         .addTriggerListener( new IndyTriggerListener( eventDispatcher ) );

                scheduler.getListenerManager().addJobListener( new IndyJobListener() );
            }

            scheduler.start();
        }
        catch ( final SchedulerException e )
        {
            throw new IndyLifecycleException( "Failed to start scheduler", e );
        }
    }

    public static SchedulerEvent createEvent( final SchedulerEventType eventType, final JobDetail jobDetail )
    {
        final JobDataMap dataMap = jobDetail.getJobDataMap();
        final String type = dataMap.getString( JOB_TYPE );

        final String data = dataMap.getString( PAYLOAD );

        return new SchedulerEvent( eventType, type, data );
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
            final Set<TriggerKey> canceled =
                    cancelAllBefore( new StoreKeyMatcher( deploy.getKey(), CONTENT_JOB_TYPE ), timeout );

            for ( final TriggerKey key : canceled )
            {
                final String path = key.getName();
                final StoreKey sk = storeKeyFrom( key.getGroup() );

                scheduleContentExpiration( sk, path, timeout );
            }
        }
    }

    public synchronized void rescheduleRepoTimeouts( final HostedRepository deploy )
            throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return;
        }

        if ( deploy == null )
        {
            return;
        }

        Integer timeout = deploy.getRepoTimeoutSeconds();
        if ( timeout == null || timeout <= 0 )
        {
            cancel( new StoreKeyMatcher( deploy.getKey(), STORE_JOB_TYPE ), STORE_JOB_TYPE );
            return;
        }

        final Set<TriggerKey> canceled =
                cancelAllBefore( new StoreKeyMatcher( deploy.getKey(), STORE_JOB_TYPE ), timeout );

        for ( final TriggerKey key : canceled )
        {
            final StoreKey sk = storeKeyFrom( key.getGroup() );
            scheduleStoreExpiration( sk, timeout );
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
            final Set<TriggerKey> canceled =
                cancelAllBefore( new StoreKeyMatcher( repo.getKey(), CONTENT_JOB_TYPE ), timeout );
            for ( final TriggerKey key : canceled )
            {
                final String path = key.getName();
                final StoreKey sk = storeKeyFrom( key.getGroup() );

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
            if ( (info != null && info.isMetadata()) && repo.getMetadataTimeoutSeconds() > 0 )
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

    public synchronized void scheduleForStore( final StoreKey key, final String jobType, final String jobName, final Object payload,
                                  final int startSeconds, final int repeatSeconds )
        throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return;
        }

        final JobDataMap dataMap = new JobDataMap();
        dataMap.put( JOB_TYPE, jobType );
        try
        {
            dataMap.put( PAYLOAD, objectMapper.writeValueAsString( payload ) );
        }
        catch ( final JsonProcessingException e )
        {
            throw new IndySchedulerException( "Failed to serialize JSON payload: " + payload, e );
        }

        final JobKey jk = new JobKey( jobName, groupName( key, jobType ) );
        try
        {
            JobDetail detail = scheduler.getJobDetail( jk );
            if ( detail == null )
            {
                detail = JobBuilder.newJob( ExpirationJob.class )
                                   .withIdentity( jk )
                                   .storeDurably()
                                   .requestRecovery()
                                   .setJobData( dataMap )
                                   .build();
            }

            final long startMillis = System.currentTimeMillis() + ( startSeconds * 1000 );

            final TriggerBuilder<Trigger> tb = TriggerBuilder.newTrigger()
                                                             .withIdentity( jk.getName(), jk.getGroup() )
                                                             .forJob( detail )
                                                             .startAt( new Date( startMillis ) );

            if ( repeatSeconds > -1 )
            {
                tb.withSchedule( SimpleScheduleBuilder.repeatSecondlyForever( repeatSeconds ) );
            }

            final Trigger trigger = tb.build();
            scheduler.scheduleJob( detail, Collections.singleton( trigger ), true );
        }
        catch ( final SchedulerException e )
        {
            throw new IndySchedulerException( "Failed to schedule content-expiration job.", e );
        }
    }

    public synchronized void scheduleContentExpiration( final StoreKey key, final String path, final int timeoutSeconds )
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

    public synchronized void scheduleStoreExpiration( final StoreKey key, final int timeoutSeconds )
            throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return;
        }

        logger.info( "Scheduling timeout for: {} in: {} seconds (at: {}).", key, timeoutSeconds,
                     new Date( System.currentTimeMillis() + ( timeoutSeconds * 1000 ) ) );

        scheduleForStore( key, STORE_JOB_TYPE, STORE_JOB_TYPE, key, timeoutSeconds, -1 );
    }

    public synchronized void setSnapshotTimeouts( final StoreKey key, final String path )
        throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return;
        }

        HostedRepository deploy = getHostedRepository( key );

        if ( deploy == null )
        {
            return;
        }

        //        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
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

        if ( ContentQuality.SNAPSHOT == quality  && deploy.getSnapshotTimeoutSeconds() > 0 )
        {
            final int timeout = deploy.getSnapshotTimeoutSeconds();

            //            logger.info( "[SNAPSHOT TIMEOUT SET] {}/{}; {}", deploy.getKey(), path, new Date( timeout ) );
            cancel( new StoreKeyMatcher( key, CONTENT_JOB_TYPE ), path );

            scheduleContentExpiration( key, path, timeout );
        }
    }

    public synchronized void setRepoTimeouts( final StoreKey key )
            throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return;
        }

        HostedRepository deploy = getHostedRepository( key );

        if ( deploy == null )
        {
            return;
        }

        Integer timeout = deploy.getRepoTimeoutSeconds();

        cancel( new StoreKeyMatcher( key, STORE_JOB_TYPE ), STORE_JOB_TYPE );

        if ( timeout != null && timeout > 0 )
        {
            scheduleStoreExpiration( key, timeout );
        }
    }

    private synchronized HostedRepository getHostedRepository( final StoreKey key )
            throws IndySchedulerException
    {
        HostedRepository deploy = null;
        try
        {
            final ArtifactStore store = dataManager.getArtifactStore( key );
            if ( store == null )
            {
                return null;
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

        return deploy;
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

    public synchronized Set<TriggerKey> cancelAllBefore( final GroupMatcher<TriggerKey> matcher, final long timeout )
        throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return Collections.emptySet();
        }

        final Set<TriggerKey> canceled = new HashSet<>();
        try
        {
            final Set<TriggerKey> keys = scheduler.getTriggerKeys( matcher );
            final Date to = new Date( System.currentTimeMillis() + ( timeout * 1000 ) );
            for ( final TriggerKey key : keys )
            {
                final Trigger trigger = scheduler.getTrigger( key );
                if ( trigger == null )
                {
                    continue;
                }

                final Date nextFire = trigger.getFireTimeAfter( new Date() );
                if ( nextFire == null || !nextFire.after( to ) )
                {
                    scheduler.unscheduleJob( key );
                    canceled.add( key );
                }
            }
        }
        catch ( final SchedulerException e )
        {
            throw new IndySchedulerException( "Failed to cancel jobs that timeout after " + timeout + " seconds.", e );
        }

        return canceled;
    }

    public synchronized Set<TriggerKey> cancelAll( final GroupMatcher<TriggerKey> matcher )
        throws IndySchedulerException
    {
        return cancel( matcher, ANY );
    }

    public synchronized Set<TriggerKey> cancel( final GroupMatcher<TriggerKey> matcher, final String name )
        throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return Collections.emptySet();
        }

        Set<TriggerKey> canceled = new HashSet<>();
        try
        {
            final Set<TriggerKey> keys = scheduler.getTriggerKeys( matcher );
            if ( keys != null && !keys.isEmpty() )
            {
                Set<TriggerKey> unscheduled = null;
                if ( name == ANY )
                {
                    for ( final TriggerKey tk : keys )
                    {
                        final Trigger trigger = scheduler.getTrigger( tk );
                        if ( trigger != null )
                        {
                            scheduler.deleteJob( trigger.getJobKey() );
                        }
                    }
                    unscheduled = keys;
                }
                else
                {
                    for ( final TriggerKey key : keys )
                    {
                        if ( key.getName()
                                .equals( name ) )
                        {
                            final Trigger trigger = scheduler.getTrigger( key );
                            if ( trigger != null )
                            {
                                scheduler.deleteJob( trigger.getJobKey() );
                            }
                            unscheduled = Collections.singleton( key );
                            break;
                        }
                    }
                }

                if ( unscheduled != null )
                {
                    canceled = unscheduled;
                }
            }
        }
        catch ( final SchedulerException e )
        {
            throw new IndySchedulerException( "Failed to cancel all triggers matching: " + matcher, e );
        }

        return canceled;
    }

    public synchronized Expiration findSingleExpiration( final GroupMatcher<TriggerKey> matcher )
            throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return null;
        }

        try
        {
            final Set<TriggerKey> keys = scheduler.getTriggerKeys( matcher );
            if ( keys != null && !keys.isEmpty() )
            {
                TriggerKey triggerKey = keys.iterator().next();
                return toExpiration( triggerKey );
            }
        }
        catch ( final SchedulerException e )
        {
            throw new IndySchedulerException( "Failed to find trigger matching: " + matcher, e );
        }

        return null;
    }

    public synchronized ExpirationSet findMatchingExpirations( final GroupMatcher<TriggerKey> matcher )
            throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return null;
        }

        try
        {
            final Set<TriggerKey> keys = scheduler.getTriggerKeys( matcher );
            Set<Expiration> expirations = new HashSet<>( keys.size() );
            if ( keys != null && !keys.isEmpty() )
            {
                for ( TriggerKey key : keys )
                {
                    expirations.add( toExpiration( key ) );
                }
            }

            return new ExpirationSet( expirations );
        }
        catch ( final SchedulerException e )
        {
            throw new IndySchedulerException( "Failed to find trigger matching: " + matcher, e );
        }
    }

    private Expiration toExpiration( final TriggerKey key )
            throws SchedulerException
    {
        Trigger trigger = scheduler.getTrigger( key );

        return new Expiration( trigger.getJobKey().getGroup(), trigger.getJobKey().getName(),
                               trigger.getNextFireTime() );
    }

    public synchronized TriggerKey findFirstMatchingTrigger( final GroupMatcher<TriggerKey> matcher )
        throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return null;
        }

        try
        {
            final Set<TriggerKey> keys = scheduler.getTriggerKeys( matcher );
            if ( keys != null && !keys.isEmpty() )
            {
                return keys.iterator()
                           .next();
            }
        }
        catch ( final SchedulerException e )
        {
            throw new IndySchedulerException( "Failed to find all triggers matching: " + matcher, e );
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

    public synchronized boolean deleteJobs( final Set<TriggerKey> keys )
        throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return false;
        }

        for ( final TriggerKey key : keys )
        {
            try
            {
                final JobKey jk = new JobKey( key.getName(), key.getGroup() );
                return scheduler.deleteJob( jk );
            }
            catch ( final SchedulerException e )
            {
                throw new IndySchedulerException( "Failed to delete job corresponding to: %s", e, key, e.getMessage() );
            }
        }

        return false;
    }

    public synchronized boolean deleteJob( final String group, final String name )
        throws IndySchedulerException
    {
        if ( !schedulerConfig.isEnabled() )
        {
            logger.debug( "Scheduler disabled." );
            return false;
        }

        final JobKey jk = new JobKey( name, group );
        try
        {
            return scheduler.deleteJob( jk );
        }
        catch ( final SchedulerException e )
        {
            throw new IndySchedulerException( "Failed to delete job: %s/%s", e, group, name );
        }
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

        if ( scheduler != null )
        {
            try
            {
                scheduler.shutdown();
            }
            catch ( final SchedulerException e )
            {
                throw new IndyLifecycleException( "Failed to shutdown scheduler: %s", e, e.getMessage() );
            }
        }
    }

}
