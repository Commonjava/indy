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
package org.commonjava.aprox.core.expire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.commonjava.aprox.action.AproxLifecycleException;
import org.commonjava.aprox.action.BootupAction;
import org.commonjava.aprox.action.ShutdownAction;
import org.commonjava.aprox.conf.AproxConfiguration;
import org.commonjava.aprox.core.conf.AproxSchedulerConfig;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.maven.atlas.ident.util.ArtifactPathInfo;
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

import com.fasterxml.jackson.core.JsonProcessingException;

@ApplicationScoped
public class ScheduleManager
    implements BootupAction, ShutdownAction
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String PAYLOAD = "payload";

    private static final String ANY = "__ANY__";

    static final String CONTENT_JOB_TYPE = "CONTENT";

    private static final String JOB_TYPE = "JOB_TYPE";

    @Inject
    @ExecutorConfig( daemon = true, priority = 7, named = "aprox-events" )
    private Executor executor;

    @Inject
    private StoreDataManager dataManager;

    @Inject
    private AproxConfiguration config;

    @Inject
    private AproxObjectMapper objectMapper;

    @Inject
    private AproxSchedulerConfig schedulerConfig;

    @Inject
    private Event<SchedulerEvent> eventDispatcher;

    private Scheduler scheduler;

    @Override
    public void init()
        throws AproxLifecycleException
    {
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
                         .addSchedulerListener( new AproxScheduleListener( scheduler, eventDispatcher ) );

                scheduler.getListenerManager()
                         .addTriggerListener( new AproxTriggerListener( eventDispatcher ) );
            }

            scheduler.start();
        }
        catch ( final SchedulerException e )
        {
            throw new AproxLifecycleException( "Failed to start scheduler", e );
        }
    }

    public static SchedulerEvent createEvent( final SchedulerEventType eventType, final JobDetail jobDetail )
    {
        final JobDataMap dataMap = jobDetail.getJobDataMap();
        final String type = dataMap.getString( JOB_TYPE );

        final String data = dataMap.getString( PAYLOAD );

        return new SchedulerEvent( eventType, type, data );
    }

    public void rescheduleSnapshotTimeouts( final HostedRepository deploy )
        throws AproxSchedulerException
    {
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

    public void rescheduleProxyTimeouts( final RemoteRepository repo )
        throws AproxSchedulerException
    {
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

    public void setProxyTimeouts( final StoreKey key, final String path )
        throws AproxSchedulerException
    {
        RemoteRepository repo = null;
        try
        {
            repo = (RemoteRepository) dataManager.getArtifactStore( key );
        }
        catch ( final AproxDataException e )
        {
            logger.error( String.format( "Failed to retrieve store for: %s. Reason: %s", key, e.getMessage() ), e );
        }

        if ( repo == null )
        {
            return;
        }

        int timeout = config.getPassthroughTimeoutSeconds();
        if ( !repo.isPassthrough() )
        {
            timeout = repo.getCacheTimeoutSeconds();
        }

        if ( timeout > 0 )
        {
            //            logger.info( "[PROXY TIMEOUT SET] {}/{}; {}", repo.getKey(), path, new Date( System.currentTimeMillis()
            //                + timeout ) );
            cancel( new StoreKeyMatcher( key, CONTENT_JOB_TYPE ), path );

            scheduleContentExpiration( key, path, timeout );
        }
    }

    public void scheduleForStore( final StoreKey key, final String jobType, final String jobName, final Object payload,
                                  final int startSeconds, final int repeatSeconds )
        throws AproxSchedulerException
    {
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put( JOB_TYPE, jobType );
        try
        {
            dataMap.put( PAYLOAD, objectMapper.writeValueAsString( payload ) );
        }
        catch ( final JsonProcessingException e )
        {
            throw new AproxSchedulerException( "Failed to serialize JSON payload: " + payload, e );
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
            scheduler.scheduleJob( detail, trigger );
        }
        catch ( final SchedulerException e )
        {
            throw new AproxSchedulerException( "Failed to schedule content-expiration job.", e );
        }
    }

    public void scheduleContentExpiration( final StoreKey key, final String path, final int timeoutSeconds )
        throws AproxSchedulerException
    {
        logger.info( "Scheduling timeout for: {} in: {} in: {} seconds.", path, key, timeoutSeconds );
        scheduleForStore( key, CONTENT_JOB_TYPE, path, new ContentExpiration( key, path ), timeoutSeconds, -1 );
    }

    public void setSnapshotTimeouts( final StoreKey key, final String path )
        throws AproxSchedulerException
    {
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
        catch ( final AproxDataException e )
        {
            logger.error( String.format( "Failed to retrieve deploy point for: %s. Reason: %s", key, e.getMessage() ),
                          e );
        }

        if ( deploy == null )
        {
            return;
        }

        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        if ( pathInfo == null )
        {
            return;
        }

        if ( pathInfo.isSnapshot() && deploy.getSnapshotTimeoutSeconds() > 0 )
        {
            final int timeout = deploy.getSnapshotTimeoutSeconds();

            //            logger.info( "[SNAPSHOT TIMEOUT SET] {}/{}; {}", deploy.getKey(), path, new Date( timeout ) );
            cancel( new StoreKeyMatcher( key, CONTENT_JOB_TYPE ), path );

            scheduleContentExpiration( key, path, timeout );
        }
    }

    private HostedRepository findDeployPoint( final Group group )
        throws AproxDataException
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

    public Set<TriggerKey> cancelAllBefore( final GroupMatcher<TriggerKey> matcher, final long timeout )
        throws AproxSchedulerException
    {
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
                if ( !nextFire.after( to ) )
                {
                    scheduler.unscheduleJob( key );
                    canceled.add( key );
                }
            }
        }
        catch ( final SchedulerException e )
        {
            throw new AproxSchedulerException( "Failed to cancel jobs that timeout after " + timeout + " seconds.", e );
        }

        return canceled;
    }

    public Set<TriggerKey> cancelAll( final GroupMatcher<TriggerKey> matcher )
        throws AproxSchedulerException
    {
        return cancel( matcher, ANY );
    }

    public Set<TriggerKey> cancel( final GroupMatcher<TriggerKey> matcher, final String name )
        throws AproxSchedulerException
    {
        Set<TriggerKey> canceled = new HashSet<>();
        try
        {
            final Set<TriggerKey> keys = scheduler.getTriggerKeys( matcher );
            if ( keys != null && !keys.isEmpty() )
            {
                Set<TriggerKey> unscheduled = null;
                if ( name == ANY )
                {
                    scheduler.unscheduleJobs( new ArrayList<TriggerKey>( keys ) );
                    unscheduled = keys;
                }
                else
                {
                    for ( final TriggerKey key : keys )
                    {
                        if ( key.getName()
                                .equals( name ) )
                        {
                            scheduler.unscheduleJob( key );
                            unscheduled = Collections.singleton( key );
                            break;
                        }
                    }
                }

                if ( unscheduled != null )
                {
                    for ( final TriggerKey tk : unscheduled )
                    {
                        final Trigger trigger = scheduler.getTrigger( tk );
                        if ( trigger != null )
                        {
                            scheduler.deleteJob( trigger.getJobKey() );
                        }
                    }
                }

                canceled = unscheduled;
            }
        }
        catch ( final SchedulerException e )
        {
            throw new AproxSchedulerException( "Failed to cancel all triggers matching: " + matcher, e );
        }

        return canceled;
    }

    public TriggerKey findFirstMatchingTrigger( final GroupMatcher<TriggerKey> matcher )
        throws AproxSchedulerException
    {
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
            throw new AproxSchedulerException( "Failed to find all triggers matching: " + matcher, e );
        }

        return null;
    }

    public String groupName( final StoreKey key, final String jobType )
    {
        return key.toString() + ":" + jobType;
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

    public void deleteJobs( final Set<TriggerKey> keys )
        throws AproxSchedulerException
    {
        for ( final TriggerKey key : keys )
        {
            try
            {
                final JobKey jk = new JobKey( key.getName(), key.getGroup() );
                scheduler.deleteJob( jk );
            }
            catch ( final SchedulerException e )
            {
                throw new AproxSchedulerException( "Failed to delete job corresponding to: %s", e, key, e.getMessage() );
            }
        }
    }

    public void deleteJob( final String group, final String name )
        throws AproxSchedulerException
    {
        final JobKey jk = new JobKey( name, group );
        try
        {
            scheduler.deleteJob( jk );
        }
        catch ( final SchedulerException e )
        {
            throw new AproxSchedulerException( "Failed to delete job: %s/%s", e, group, name );
        }
    }

    @Override
    public String getId()
    {
        return "AProx Scheduler";
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
        throws AproxLifecycleException
    {
        if ( scheduler != null )
        {
            try
            {
                scheduler.shutdown();
            }
            catch ( final SchedulerException e )
            {
                throw new AproxLifecycleException( "Failed to shutdown scheduler: %s", e, e.getMessage() );
            }
        }
    }

}
