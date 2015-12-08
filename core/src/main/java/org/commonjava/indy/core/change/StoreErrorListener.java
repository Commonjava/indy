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
package org.commonjava.indy.core.change;

import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.change.event.IndyStoreErrorEvent;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.core.expire.IndySchedulerException;
import org.commonjava.indy.core.expire.ScheduleManager;
import org.commonjava.indy.core.expire.SchedulerEvent;
import org.commonjava.indy.core.expire.SchedulerEventType;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;

public class StoreErrorListener
{

    private static final String DISABLE_TIMEOUT = "Disable-Timeout";

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private ScheduleManager scheduleManager;

    @Inject
    private IndyObjectMapper objectMapper;

    @Inject
    private IndyConfiguration config;

    public void onStoreError( @Observes IndyStoreErrorEvent evt )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        StoreKey key = evt.getStoreKey();
        Throwable error = evt.getError();
        try
        {
            ArtifactStore store = storeDataManager.getArtifactStore( key );
            store.setDisabled( true );

            storeDataManager.storeArtifactStore( store, new ChangeSummary( ChangeSummary.SYSTEM_USER, String.format(
                    "Disabling %s due to error: %s\n\nStack Trace:\n  %s", key, error,
                    StringUtils.join( error.getStackTrace(), "\n  " ) ) ), new EventMetadata() );

            logger.warn( "{} has been disabled due to store-level error: {}\n Will re-enable in {} seconds.", key,
                         error, config.getStoreDisableTimeoutSeconds() );

            scheduleManager.scheduleForStore( key, DISABLE_TIMEOUT, disableJobName( key ), key,
                                              config.getStoreDisableTimeoutSeconds(), 99999 );
        }
        catch ( IndyDataException e )
        {
            logger.error( String.format( "Failed to disable %s on error: %s", key, error ), e);
        }
        catch ( IndySchedulerException e )
        {
            logger.error( String.format( "Failed to schedule re-enablement of %s for retry.", key ), e );
        }
    }

    private String disableJobName( StoreKey key )
    {
        return String.format( "%s-%s", key, DISABLE_TIMEOUT );
    }

    public void onDisableTimeout( @Observes SchedulerEvent evt )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "Checking for store-reenable event in: {} (trigger? {} Disable-Timeout? {})", evt,
                      evt.getEventType() == SchedulerEventType.TRIGGER, DISABLE_TIMEOUT.equals( evt.getJobType() ) );

        if ( evt.getEventType() == SchedulerEventType.TRIGGER && DISABLE_TIMEOUT.equals( evt.getJobType() ) )
        {
            String keystr = evt.getPayload();
            StoreKey key = null;
            try
            {
                key = objectMapper.readValue( keystr, StoreKey.class );
            }
            catch ( IOException e )
            {
                logger.warn( "Failed to read StoreKey from JSON string: '{}' in event payload.", keystr );
            }

            logger.debug( "Read key: {} from JSON string: '{}' in event payload.", key, keystr );

            if ( key != null )
            {
                try
                {
                    ArtifactStore store = storeDataManager.getArtifactStore( key );
                    store.setDisabled( false );

                    storeDataManager.storeArtifactStore( store, new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                                   "Re-enabling " + key ),
                                                         new EventMetadata() );

                    logger.warn( "{} has been re-enabled for use.", key );

                    scheduleManager.deleteJob( scheduleManager.groupName( key, DISABLE_TIMEOUT ), disableJobName( key ) );
                }
                catch ( IndyDataException e )
                {
                    logger.error( String.format( "Failed to re-enable %s", key ), e);
                }
                catch ( IndySchedulerException e )
                {
                    logger.error( String.format( "Failed to delete re-enablement job for %s.", key ), e );
                }
            }
        }
    }
}
