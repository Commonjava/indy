/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.subsys.kafka.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.streams.kstream.KStream;
import org.commonjava.event.store.AbstractStoreUpdateEvent;
import org.commonjava.event.store.EventStoreKey;
import org.commonjava.event.store.StoreEnablementEvent;
import org.commonjava.event.store.StorePostUpdateEvent;
import org.commonjava.event.store.StorePreUpdateEvent;
import org.commonjava.indy.change.event.ArtifactStoreUpdateType;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.data.StoreEventDispatcher;
import org.commonjava.indy.db.service.ServiceStoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.PathStyle;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.kafka.event.DefualtIndyStoreEvent;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.commonjava.indy.subsys.kafka.event.TopicType.STORE_EVENT;

@ApplicationScoped
public class RepoServiceEventHandler
        implements ServiceEventHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreEventDispatcher dispatcher;

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private ObjectMapper mapper;

    @Override
    public void dispatchEvent( KStream<String, String> streams, String topic )
    {
        if ( !topic.equals( STORE_EVENT.getName() ) )
        {
            return;
        }
        logger.trace( "Using {} as the event dispatcher", dispatcher.getClass().getName() );
        streams.foreach( ( key, value ) -> {
            try
            {
                final DefualtIndyStoreEvent storeEvent = mapper.readValue( value, DefualtIndyStoreEvent.class );
                logger.info( "Start the consumer streaming for event type {}", storeEvent.getEventType().name() );
                final Map<EventStoreKey, ArtifactStore> storeMap = getStoreMap( storeEvent );
                final ArtifactStore[] stores = storeMap.values().toArray( new ArtifactStore[0] );
                final EventMetadata eventMetadata = convertEventMetadata( storeEvent );
                Map<ArtifactStore, ArtifactStore> changeMap;
                List<String> changeMapStr;
                switch ( storeEvent.getEventType() )
                {
                    case PreDelete:
                        logger.info( "Firing store pre-delete event for: {} ", Arrays.stream( stores )
                                                                                  .map( ArtifactStore::getKey )
                                                                                  .collect( Collectors.toList() ) );
                        dispatcher.deleting( eventMetadata, stores );
                        break;
                    case PostDelete:
                        logger.info( "Firing store post-delete event for: {} ", Arrays.stream( stores )
                                                                                     .map( ArtifactStore::getKey )
                                                                                     .collect( Collectors.toList() ) );
                        dispatcher.deleted( eventMetadata, stores );
                        break;
                    case PreUpdate:
                        StorePreUpdateEvent preUpdateEvent = mapper.readValue( value, StorePreUpdateEvent.class );
                        changeMap = getChangeMap( preUpdateEvent, storeMap );
                        changeMapStr = changeMap.entrySet()
                                                .stream()
                                                .map( e -> String.format( "%s -> %s", e.getKey().getKey(),
                                                                          e.getValue().getKey() ) )
                                                .collect( Collectors.toList() );
                        logger.info( "Firing store pre-update event for: {} ", changeMapStr );
                        dispatcher.updating( ArtifactStoreUpdateType.valueOf( preUpdateEvent.getUpdateType().name() ),
                                             eventMetadata, changeMap );
                        break;
                    case PostUpdate:
                        StorePostUpdateEvent postUpdateEvent = mapper.readValue( value, StorePostUpdateEvent.class );
                        changeMap = getChangeMap( postUpdateEvent, storeMap );
                        changeMapStr = changeMap.entrySet()
                                                .stream()
                                                .map( e -> String.format( "%s -> %s", e.getKey().getKey(),
                                                                          e.getValue().getKey() ) )
                                                .collect( Collectors.toList() );
                        logger.info( "Firing store post-update event for: {}", changeMapStr );
                        dispatcher.updated( ArtifactStoreUpdateType.valueOf( postUpdateEvent.getUpdateType().name() ),
                                             eventMetadata, changeMap );
                        break;
                    case Enablement:
                        StoreEnablementEvent enablementEvent = mapper.readValue( value, StoreEnablementEvent.class );
                        boolean disabling = enablementEvent.isDisabling();
                        boolean preprocessing = enablementEvent.isPreprocessing();
                        List<StoreKey> storesKeys =
                                Arrays.stream( stores ).map( ArtifactStore::getKey ).collect( Collectors.toList() );
                        if ( !disabling && preprocessing )
                        {
                            logger.info( "Firing store enabling event for: {}", storesKeys );
                            dispatcher.enabling( eventMetadata, stores );
                        }
                        if ( !disabling && !preprocessing )
                        {
                            logger.info( "Firing store enabled event for: {}", storesKeys );
                            dispatcher.enabled( eventMetadata, stores );
                        }
                        if ( disabling && preprocessing )
                        {
                            logger.info( "Firing store disabling event for: {}", storesKeys );
                            dispatcher.disabling( eventMetadata, stores );
                        }
                        if ( disabling && !preprocessing )
                        {
                            logger.info( "Firing store disabled event for: {}", storesKeys );
                            dispatcher.disabled( eventMetadata, stores );
                        }
                        break;
                }
                logger.info( "Finish the consumer event dispatcher for event type {}",
                              storeEvent.getEventType().name() );
            }
            catch ( JsonProcessingException e )
            {
                logger.error( String.format( "Failed to parse and read value from event message on topic %s, "
                                                     + "it might not be the standard service event format: %s.", topic,
                                             e.getMessage() ), e );
            }
            catch ( IndyDataException e )
            {
                logger.error(
                        String.format( "Error occurred during retrieving through data manager on topic %s: %s.", topic,
                                       e.getMessage() ), e );
            }
            catch ( Exception e )
            {
                logger.error(
                        String.format( "Error occurred during consuming the streaming messages on topic %s: %s.", topic,
                                       e.getMessage() ), e );
            }
        } );
    }

    private Map<EventStoreKey, ArtifactStore> getStoreMap( DefualtIndyStoreEvent storeEvent )
            throws IndyDataException
    {
        final Map<EventStoreKey, ArtifactStore> storeMap = new HashMap<>();
        for ( EventStoreKey eventStoreKey : storeEvent.getKeys() )
        {
            if ( storeDataManager instanceof ServiceStoreDataManager )
            {
                logger.info( "Get store through store data manager {} force query.",
                             storeDataManager.getClass().getName() );
                StoreKey storeKey = convertToStoreKey( eventStoreKey );
                ArtifactStore store = ( (ServiceStoreDataManager) storeDataManager ).getArtifactStore( storeKey, true );
                if ( store == null )
                {
                    logger.error( "Failed to fetch store {} through data manager.", storeKey );
                    continue;
                }
                storeMap.put( eventStoreKey, store );
            }
            else
            {
                logger.warn( "No valid force query called from data manager: {}.",
                             storeDataManager.getClass().getName() );
            }
        }
        return storeMap;
    }

    private StoreKey convertToStoreKey( EventStoreKey eventStoreKey )
    {
        return new StoreKey( eventStoreKey.getPackageType(), StoreType.valueOf( eventStoreKey.getStoreType() ),
                             eventStoreKey.getStoreName() );
    }

    public EventMetadata convertEventMetadata( DefualtIndyStoreEvent storeEvent )
    {
        org.commonjava.event.common.EventMetadata metadata = storeEvent.getEventMetadata();
        final EventMetadata eventMetadata = new EventMetadata( metadata.getPackageType() );
        for ( Object metaKey : metadata.getMetadata().keySet() )
        {
            eventMetadata.set( metaKey, metadata.get( metaKey ) );
        }
        return eventMetadata;
    }

    private Map<ArtifactStore, ArtifactStore> getChangeMap( AbstractStoreUpdateEvent updateEvent,
                                                            Map<EventStoreKey, ArtifactStore> storeMap )
    {
        Map<ArtifactStore, ArtifactStore> changeMap = new HashMap<>();
        if ( storeMap == null || storeMap.size() == 0 )
        {
            return changeMap;
        }
        for ( EventStoreKey storeKey : updateEvent.getChangeMap().keySet() )
        {
            ArtifactStore newStore = storeMap.get( storeKey );
            if ( newStore == null )
            {
                logger.warn( "Don't find actual store {} according to the event changes.", storeKey.toString() );
                continue;
            }
            Map<String, List<Object>> changes = updateEvent.getChangeMap().get( storeKey );
            ArtifactStore originalStore = newStore.copyOf();
            for ( String metaName : changes.keySet() )
            {
                List<Object> values = changes.get( metaName );
                Object originalValue = values.get( 1 );
                revertOriginalRepo( metaName, newStore, originalStore, originalValue );
            }
            changeMap.put( newStore, originalStore );
        }
        return changeMap;
    }

    @SuppressWarnings( {"unchecked", "rawtypes"} )
    private void revertOriginalRepo( String metaName, ArtifactStore newStore, ArtifactStore originalStore,
                                     Object originalValue )
    {
        switch ( metaName )
        {
            case "description":
                originalStore.setDescription( (String) originalValue );
                break;
            case "path_style":
                originalStore.setPathStyle( PathStyle.valueOf( (String) originalValue ) );
                break;
            case "disable_timeout":
                originalStore.setDisableTimeout( (Integer) originalValue );
                break;
            case "path_mask_patterns":
                List<String> list = objToList( originalValue, String.class );
                originalStore.setPathMaskPatterns( new HashSet( list ) );
                break;
            case "authoritative_index":
                originalStore.setAuthoritativeIndex( (Boolean) originalValue );
                break;
            case "metadata":
                originalStore.setMetadata( (Map) originalValue );
                break;
        }
        switch ( newStore.getType() )
        {
            case remote:
                revertOriginalRemoteRepo( metaName, originalValue, (RemoteRepository) originalStore );
                break;
            case hosted:
                revertOriginalHostedRepo( metaName, originalValue, (HostedRepository) originalStore );
                break;
            case group:
                revertOriginalGroupRepo( metaName, originalValue, (Group) originalStore );
                break;
        }
    }

    private void revertOriginalRemoteRepo( String metaName, Object originalValue, RemoteRepository originalStore )
    {
        switch ( metaName )
        {
            case "allow_releases":
                originalStore.setAllowReleases( (Boolean) originalValue );
                break;
            case "allow_snapshots":
                originalStore.setAllowSnapshots( (Boolean) originalValue );
                break;
            case "nfc_timeout_seconds":
                originalStore.setNfcTimeoutSeconds( (Integer) originalValue );
                break;
            case "max_connections":
                originalStore.setMaxConnections( (Integer) originalValue );
                break;
            case "ignore_hostname_verification":
                originalStore.setIgnoreHostnameVerification( (Boolean) originalValue );
                break;
            case "cache_timeout_seconds":
                originalStore.setCacheTimeoutSeconds( (Integer) originalValue );
                break;
            case "metadata_timeout_seconds":
                originalStore.setMetadataTimeoutSeconds( (Integer) originalValue );
                break;
            case "is_passthrough":
                originalStore.setPassthrough( (Boolean) originalValue );
                break;
            case "prefetch_priority":
                originalStore.setPrefetchPriority( (Integer) originalValue );
                break;
            case "prefetch_rescan":
                originalStore.setPrefetchRescan( (Boolean) originalValue );
                break;
            case "prefetch_listing_type":
                originalStore.setPrefetchListingType( (String) originalValue );
                break;
            case "prefetch_rescan_time":
                originalStore.setPrefetchRescanTimestamp( (String) originalValue );
                break;
            case "url":
                originalStore.setUrl( (String) originalValue );
                break;
            case "key_password":
                originalStore.setKeyPassword( (String) originalValue );
                break;
            case "server_certificate_pem":
                originalStore.setServerCertPem( (String) originalValue );
                break;
            case "proxy_host":
                originalStore.setProxyHost( (String) originalValue );
                break;
            case "proxy_port":
                originalStore.setProxyPort( (Integer) originalValue );
                break;
            case "proxy_user":
                originalStore.setProxyUser( (String) originalValue );
                break;
            case "proxy_password":
                originalStore.setProxyPassword( (String) originalValue );
                break;
            case "server_trust_policy":
                originalStore.setServerTrustPolicy( (String) originalValue );
                break;
        }
    }

    private void revertOriginalHostedRepo( String metaName, Object originalValue, HostedRepository originalStore )
    {
        switch ( metaName )
        {
            case "allow_releases":
                originalStore.setAllowReleases( (Boolean) originalValue );
                break;
            case "allow_snapshots":
                originalStore.setAllowSnapshots( (Boolean) originalValue );
                break;
            case "storage":
                originalStore.setStorage( (String) originalValue );
                break;
            case "snapshotTimeoutSeconds":
                originalStore.setSnapshotTimeoutSeconds( (Integer) originalValue );
                break;
            case "readonly":
                originalStore.setReadonly( (Boolean) originalValue );
                break;
        }
    }

    @SuppressWarnings( "unchecked" )
    private void revertOriginalGroupRepo( String metaName, Object originalValue, Group originalStore )
    {
        switch ( metaName )
        {
            case "prepend_constituent":
                originalStore.setPrependConstituent( (Boolean) originalValue );
                break;
            case "constituents":
                originalStore.setConstituents( objToList( originalValue, StoreKey.class ) );
                break;
        }
    }

    private <T> List<T> objToList( Object obj, Class<T> cla )
    {
        List<T> list = new ArrayList<T>();
        if ( obj instanceof ArrayList<?> )
        {
            for ( Object o : (List<?>) obj )
            {
                list.add( cla.cast( o ) );
            }
        }
        return list;
    }
}
