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
package org.commonjava.aprox.core.change;

import org.apache.commons.lang.StringUtils;
import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.change.event.AproxStoreErrorEvent;
import org.commonjava.aprox.conf.AproxConfiguration;
import org.commonjava.aprox.core.expire.AproxSchedulerException;
import org.commonjava.aprox.core.expire.ScheduleManager;
import org.commonjava.aprox.core.expire.SchedulerEvent;
import org.commonjava.aprox.core.expire.SchedulerEventType;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
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
    private AproxObjectMapper objectMapper;

    @Inject
    private AproxConfiguration config;

    public void onStoreError( @Observes AproxStoreErrorEvent evt )
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
        catch ( AproxDataException e )
        {
            logger.error( String.format( "Failed to disable %s on error: %s", key, error ), e);
        }
        catch ( AproxSchedulerException e )
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
                catch ( AproxDataException e )
                {
                    logger.error( String.format( "Failed to re-enable %s", key ), e);
                }
                catch ( AproxSchedulerException e )
                {
                    logger.error( String.format( "Failed to delete re-enablement job for %s.", key ), e );
                }
            }
        }
    }
}
