/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.core.expire;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.aprox.action.AproxLifecycleException;
import org.commonjava.aprox.action.BootupAction;
import org.commonjava.aprox.action.ShutdownAction;
import org.commonjava.aprox.conf.AproxConfiguration;
import org.commonjava.aprox.content.DownloadManager;
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
import org.commonjava.maven.atlas.ident.util.SnapshotUtils;
import org.commonjava.maven.atlas.ident.version.part.SnapshotPart;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
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
    private DownloadManager fileManager;

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

            final long startMillis = TimeUnit.MILLISECONDS.convert( startSeconds, TimeUnit.SECONDS );

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

    public void cleanMetadata( final StoreKey key, final String path )
        throws AproxSchedulerException
    {
        if ( path.endsWith( "maven-metadata.xml" ) )
        {
            try
            {
                final Set<Group> groups = dataManager.getGroupsContaining( key );
                for ( final Group group : groups )
                {
                    //                    logger.info( "[CLEAN] Cleaning metadata path: {} in group: {}", path, group.getName() );

                    cancel( new StoreKeyMatcher( key, CONTENT_JOB_TYPE ), path );

                    final Transfer item = fileManager.getStorageReference( group, path );
                    if ( item.exists() )
                    {
                        try
                        {
                            item.delete();
                        }
                        catch ( final IOException e )
                        {
                            logger.error( "Failed to delete: {}. Error: {}",
                                          fileManager.getStorageReference( group, path ), e.getMessage() );
                        }
                    }
                }
            }
            catch ( final AproxDataException e )
            {
                logger.error( String.format( "Attempting to update groups for metadata change; Failed to retrieve groups containing store: {}. Error: {}",
                                             key, e.getMessage() ), e );
            }
        }
    }

    public void updateSnapshotVersions( final StoreKey key, final String path )
    {
        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        if ( pathInfo == null )
        {
            return;
        }

        final ArtifactStore store;
        try
        {
            store = dataManager.getArtifactStore( key );
        }
        catch ( final AproxDataException e )
        {
            logger.error( String.format( "Failed to update metadata after snapshot deletion. Reason: {}",
                                         e.getMessage() ), e );
            return;
        }

        if ( store == null )
        {
            logger.error( "Failed to update metadata after snapshot deletion in: {}. Reason: Cannot find corresponding ArtifactStore",
                          key );
            return;
        }

        final Transfer item = fileManager.getStorageReference( store, path );
        if ( item.getParent() == null || item.getParent()
                                             .getParent() == null )
        {
            return;
        }

        final Transfer metadata = fileManager.getStorageReference( store, item.getParent()
                                                                              .getParent()
                                                                              .getPath(), "maven-metadata.xml" );

        if ( metadata.exists() )
        {
            //            logger.info( "[UPDATE VERSIONS] Updating snapshot versions for path: {} in store: {}", path, key.getName() );
            Reader reader = null;
            Writer writer = null;
            try
            {
                reader = new InputStreamReader( metadata.openInputStream() );
                final Metadata md = new MetadataXpp3Reader().read( reader );

                final Versioning versioning = md.getVersioning();
                final List<String> versions = versioning.getVersions();

                final String version = pathInfo.getVersion();
                String replacement = null;

                final int idx = versions.indexOf( version );
                if ( idx > -1 )
                {
                    if ( idx > 0 )
                    {
                        replacement = versions.get( idx - 1 );
                    }

                    versions.remove( idx );
                }

                if ( version.equals( md.getVersion() ) )
                {
                    md.setVersion( replacement );
                }

                if ( version.equals( versioning.getLatest() ) )
                {
                    versioning.setLatest( replacement );
                }

                final SnapshotPart si = pathInfo.getSnapshotInfo();
                if ( si != null )
                {
                    final SnapshotPart siRepl = SnapshotUtils.extractSnapshotVersionPart( replacement );
                    final Snapshot snapshot = versioning.getSnapshot();

                    final String siTstamp = SnapshotUtils.generateSnapshotTimestamp( si.getTimestamp() );
                    if ( si.isRemoteSnapshot() && siTstamp.equals( snapshot.getTimestamp() )
                        && si.getBuildNumber() == snapshot.getBuildNumber() )
                    {
                        if ( siRepl != null )
                        {
                            if ( siRepl.isRemoteSnapshot() )
                            {
                                snapshot.setTimestamp( SnapshotUtils.generateSnapshotTimestamp( siRepl.getTimestamp() ) );
                                snapshot.setBuildNumber( siRepl.getBuildNumber() );
                            }
                            else
                            {
                                snapshot.setLocalCopy( true );
                            }
                        }
                        else
                        {
                            versioning.setSnapshot( null );
                        }
                    }
                }

                writer = new OutputStreamWriter( metadata.openOutputStream( TransferOperation.GENERATE, true ) );
                new MetadataXpp3Writer().write( writer, md );
            }
            catch ( final IOException e )
            {
                logger.error( "Failed to update metadata after snapshot deletion.\n  Snapshot: {}\n  Metadata: {}\n  Reason: {}",
                              e, item.getFullPath(), metadata, e.getMessage() );
            }
            catch ( final XmlPullParserException e )
            {
                logger.error( "Failed to update metadata after snapshot deletion.\n  Snapshot: {}\n  Metadata: {}\n  Reason: {}",
                              e, item.getFullPath(), metadata, e.getMessage() );
            }
            finally
            {
                closeQuietly( reader );
                closeQuietly( writer );
            }
        }
    }

    public Set<TriggerKey> cancelAllBefore( final GroupMatcher<TriggerKey> matcher, final long timeout )
        throws AproxSchedulerException
    {
        final Set<TriggerKey> canceled = new HashSet<>();
        try
        {
            final Set<TriggerKey> keys = scheduler.getTriggerKeys( matcher );
            final Date to = new Date( TimeUnit.MILLISECONDS.convert( timeout, TimeUnit.SECONDS ) );
            for ( final TriggerKey key : keys )
            {
                final Trigger trigger = scheduler.getTrigger( key );
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
                        scheduler.deleteJob( trigger.getJobKey() );
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
        return 20;
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
