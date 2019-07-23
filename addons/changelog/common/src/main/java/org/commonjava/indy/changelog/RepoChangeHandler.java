/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.changelog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.difflib.algorithm.DiffException;
import org.commonjava.auditquery.history.ChangeEvent;
import org.commonjava.auditquery.history.ChangeType;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.change.event.ArtifactStoreDeletePreEvent;
import org.commonjava.indy.change.event.ArtifactStorePreUpdateEvent;
import org.commonjava.indy.changelog.cache.RepoChangelogCache;
import org.commonjava.indy.changelog.conf.RepoChangelogConfiguration;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

@ApplicationScoped
public class RepoChangeHandler
{
    private static final int DIFF_PATCH_CONTEXT_LINES = 3;

    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Inject
    private IndyObjectMapper objectMapper;

    @Inject
    private RepoChangelogConfiguration config;

    @Inject
    @RepoChangelogCache
    private CacheHandle<String, ChangeEvent> repoChangelogCache;

    public void generateRepoUpdateLog( @Observes final ArtifactStorePreUpdateEvent event )
    {
        handleRepoChange( new DiffStoreFetcher()
        {
            @Override
            public ArtifactStore getOriginal( ArtifactStore store )
            {
                return event.getOriginal( store );
            }

            @Override
            public ArtifactStore getChanged( ArtifactStore store )
            {
                return store;
            }

            @Override
            public Collection<ArtifactStore> getStores()
            {
                return event.getChanges();
            }

            @Override
            public ChangeSummary getSummary()
            {
                return (ChangeSummary) event.getEventMetadata().get( StoreDataManager.CHANGE_SUMMARY );
            }
        }, false );
    }

    public void generateRepoDeleteLog( @Observes final ArtifactStoreDeletePreEvent event )
    {
        handleRepoChange( new DiffStoreFetcher()
        {
            @Override
            public ArtifactStore getOriginal( ArtifactStore store )
            {
                return store;
            }

            @Override
            public ArtifactStore getChanged( ArtifactStore store )
            {
                return null;
            }

            @Override
            public Collection<ArtifactStore> getStores()
            {
                return event.getStores();
            }

            @Override
            public ChangeSummary getSummary()
            {
                return (ChangeSummary) event.getEventMetadata().get( StoreDataManager.CHANGE_SUMMARY );
            }
        }, true );
    }

    private void handleRepoChange( DiffStoreFetcher fetcher, boolean isDelete )
    {
        if ( !config.isEnabled() )
        {
            logger.info( "Repository changelog module is not enabled. Will ignore all change logs for propagation." );
            return;
        }
        ChangeSummary changeSummary = fetcher.getSummary();
        Collection<ArtifactStore> stores = fetcher.getStores();
        String user = ChangeSummary.SYSTEM_USER;
        String summary = "";
        String version = "";
        Date timeStamp = new Date();
        if ( changeSummary != null )
        {
            if ( changeSummary.getUser() != null )
            {
                user = changeSummary.getUser();
            }
            if ( changeSummary.getSummary() != null )
            {
                summary = changeSummary.getSummary();
            }
            if ( changeSummary.getRevisionId() != null )
            {
                version = changeSummary.getRevisionId();
            }
            else
            {
                //FIXME: we need version not null here to let it be a key, not sure if timestamp is good here
                version = String.format( "%s", timeStamp.getTime() );
            }
        }

        for ( ArtifactStore store : stores )
        {
            try
            {
                ArtifactStore origin = fetcher.getOriginal( store );
                ArtifactStore changed = fetcher.getChanged( store );
                String patchString = diffRepoChanges( changed, origin );

                ChangeEvent changeLog = new ChangeEvent();
                changeLog.setEventId( UUID.randomUUID().toString().replace( "-", "" ) );
                changeLog.setStoreKey( store.getKey().toString() );
                changeLog.setChangeTime( new Date() );
                changeLog.setDiffContent( patchString );
                if ( isDelete )
                {
                    changeLog.setChangeType( ChangeType.DELETE );
                }
                else
                {
                    changeLog.setChangeType( origin == null ? ChangeType.CREATE : ChangeType.UPDATE );
                }
                changeLog.setUser( user );
                changeLog.setSummary( summary );
                changeLog.setVersion( version );
                String key = changeLog.getStoreKey() + "_" + changeLog.getVersion();
                repoChangelogCache.put( key, changeLog );
            }
            catch ( JsonProcessingException | DiffException e )
            {
                String error =
                        String.format( "Something wrong happened when doing repo change log generation for store %s",
                                       store.getKey() );
                logger.error( error, e );

            }
        }
    }

    private interface DiffStoreFetcher
    {
        ArtifactStore getOriginal( ArtifactStore store );
        ArtifactStore getChanged(ArtifactStore store);
        Collection<ArtifactStore> getStores();
        ChangeSummary getSummary();
    }

    private String diffRepoChanges( final ArtifactStore changed, final ArtifactStore origin )
            throws JsonProcessingException, DiffException
    {
        String changedString = changed == null ? "{}" : objectMapper.writeValueAsString( changed );
        String originString = objectMapper.writeValueAsString( origin );

        return DiffUtil.diffPatch( changed == null ? origin.getName() : changed.getName() + ".json", changedString,
                                   originString );
    }
}
