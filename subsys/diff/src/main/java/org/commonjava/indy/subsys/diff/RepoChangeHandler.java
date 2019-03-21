/**
 * Copyright (C) 2013 Red Hat, Inc.
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
package org.commonjava.indy.subsys.diff;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.Patch;
import org.commonjava.indy.change.event.ArtifactStorePreUpdateEvent;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.model.change.RepositoryChangeLog;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.diff.cache.RepoChangelogCache;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.jgroups.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@ApplicationScoped
public class RepoChangeHandler
{
    private static final int DIFF_PATCH_CONTEXT_LINES = 3;

    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Inject
    private IndyObjectMapper objectMapper;

    @Inject
    @RepoChangelogCache
    private CacheHandle<String, RepositoryChangeLog> repoChangelogCache;

    public void generateRepoChangeLog( @Observes ArtifactStorePreUpdateEvent event )
    {
        Collection<ArtifactStore> stores = event.getChanges();

        for ( ArtifactStore store : stores )
        {
            try
            {
                ArtifactStore origin = event.getOriginal( store );
                String patchString = diffPatchString( store, origin );

                RepositoryChangeLog changeLog = new RepositoryChangeLog();
                changeLog.setStoreKey( store.getKey() );
                changeLog.setChangeTime( new Date() );
                changeLog.setDiffContent( patchString );
                changeLog.setChangeType( "update" );
                //FIXME: how to define version string and summary?
                changeLog.setVersion( "" );
                changeLog.setSummary( "" );
                //FIXME: seems we don't have a user provider globally
                changeLog.setUser( "" );
                String key = changeLog.getStoreKey() + "_" + changeLog.getVersion();
                repoChangelogCache.put( key, changeLog );
            }
            catch ( JsonProcessingException | DiffException e )
            {
                String error =
                        String.format( "Something wrong happened when doing repo change log generation for store %s",
                                       store.getKey() );
                logger.error(error, e);

            }
        }
    }

    private String diffPatchString( final ArtifactStore changed, final ArtifactStore origin )
            throws JsonProcessingException, DiffException
    {
        String s = objectMapper.writeValueAsString( changed );
        List<String> storeNewStrings = Arrays.asList( s.split( "\n" ) );
        s = objectMapper.writeValueAsString( origin );
        List<String> storeOriginStrings = Arrays.asList( s.split( "\n" ) );
        Patch<String> patch = DiffUtils.diff( storeNewStrings, storeOriginStrings );
        String storeFileName = changed.getName() + ".json";
        List<String> patchDiff =
                UnifiedDiffUtils.generateUnifiedDiff( "a/" + storeFileName, "b/" + storeFileName, storeOriginStrings,
                                                      patch, DIFF_PATCH_CONTEXT_LINES );
        StringBuilder builder = new StringBuilder();
        patchDiff.forEach( ps -> builder.append( ps ).append( "\n" ) );
        builder.deleteCharAt( builder.lastIndexOf( "\n" ) );
        return builder.toString();
    }
}
