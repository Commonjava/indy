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
package org.commonjava.aprox.implrepo.change;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.implrepo.ImpliedReposException;
import org.commonjava.aprox.implrepo.conf.ImpliedRepoConfig;
import org.commonjava.aprox.implrepo.data.ImpliedRepoMetadataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.ArtifactPathInfo;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.maven.galley.event.FileStorageEvent;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.model.view.RepositoryView;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImpliedRepositoryDetector
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private MavenPomReader pomReader;

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private ImpliedRepoMetadataManager metadataManager;

    @Inject
    private ImpliedRepoConfig config;

    protected ImpliedRepositoryDetector()
    {
    }

    public ImpliedRepositoryDetector( final MavenPomReader pomReader, final StoreDataManager storeManager,
                                      final ImpliedRepoMetadataManager metadataManager, final ImpliedRepoConfig config )
    {
        this.pomReader = pomReader;
        this.storeManager = storeManager;
        this.metadataManager = metadataManager;
        this.config = config;
    }

    public void detectRepos( @Observes final FileStorageEvent event )
    {
        if ( !config.isEnabled() )
        {
            logger.debug( "Implied-repository processing is not enabled." );
            return;
        }

        logger.debug( "STARTED Processing: {}", event );
        final ImplicationsJob job = new ImplicationsJob( event );
        if ( !initJob( job ) )
        {
            return;
        }

        addImpliedRepositories( job );

        if ( job.implied != null && !job.implied.isEmpty() )
        {
            // Store in source remote repo metadata for future groups.
            if ( !addImpliedMetadata( job ) )
            {
                return;
            }

            // Update existing groups
            if ( !updateExistingGroups( job ) )
            {
                return;
            }
        }

        logger.debug( "FINISHED Processing: {}", event );
    }

    private boolean initJob( final ImplicationsJob job )
    {
        switch ( job.event.getType() )
        {
            case DOWNLOAD:
            case UPLOAD:
                break;

            default:
                // we're not interested in these.
                return false;
        }

        final Transfer transfer = job.transfer;
        if ( !transfer.getPath()
                      .endsWith( ".pom" ) )
        {
            return false;
        }

        final Location location = transfer.getLocation();
        if ( !( location instanceof KeyedLocation ) )
        {
            return false;
        }

        final StoreKey key = ( (KeyedLocation) location ).getKey();
        try
        {
            job.store = storeManager.getArtifactStore( key );
        }
        catch ( final AproxDataException e )
        {
            logger.error( String.format( "Cannot retrieve artifact store for: %s. Failed to process implied repositories.",
                                         key ), e );
        }

        if ( job.store == null )
        {
            return false;
        }

        job.pathInfo = ArtifactPathInfo.parse( transfer.getPath() );

        if ( job.pathInfo == null )
        {
            return false;
        }

        try
        {
            logger.debug( "Parsing: {}", transfer );

            job.pomView = pomReader.readLocalPom( job.pathInfo.getProjectId(), transfer, MavenPomView.ALL_PROFILES );
        }
        catch ( final GalleyMavenException e )
        {
            logger.error( String.format( "Cannot parse: %s from: %s. Failed to process implied repositories.",
                                         job.pathInfo.getProjectId(), transfer ), e );
        }

        if ( job.pomView == null )
        {
            return false;
        }

        return true;
    }

    private boolean updateExistingGroups( final ImplicationsJob job )
    {
        final StoreKey key = job.store.getKey();
        try
        {
            logger.debug( "Looking for groups that contain: {}", key );
            final Set<Group> groups = storeManager.getGroupsContaining( key );
            if ( groups != null )
            {
                final String message =
                    String.format( "Adding repositories implied by: %s\n\n  %s", key,
                                   StringUtils.join( job.implied, "\n  " ) );

                final ChangeSummary summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, message );
                for ( final Group group : groups )
                {
                    boolean changed = false;
                    for ( final ArtifactStore implied : job.implied )
                    {
                        changed = group.addConstituent( implied ) || changed;
                    }

                    if ( changed )
                    {
                        storeManager.storeArtifactStore( group, summary, false, false );
                    }
                }
            }
        }
        catch ( final AproxDataException e )
        {
            logger.error( "Failed to lookup groups containing: " + key, e );
        }

        return false;
    }

    private boolean addImpliedMetadata( final ImplicationsJob job )
    {
        try
        {
            logger.debug( "Adding implied-repo metadata to: {} and {}", job.store, new JoinString( ", ", job.implied ) );
            metadataManager.addImpliedMetadata( job.store, job.implied );
            return true;
        }
        catch ( final ImpliedReposException e )
        {
            logger.error( "Failed to store list of implied stores in: " + job.store.getKey(), e );
        }

        return false;
    }

    private void addImpliedRepositories( final ImplicationsJob job )
    {
        job.implied = new ArrayList<ArtifactStore>();

        logger.debug( "Retrieving repository/pluginRepository declarations from:\n  ",
                     new JoinString( "\n  ", job.pomView.getDocRefStack() ) );

        final List<List<RepositoryView>> repoLists =
            Arrays.asList( job.pomView.getAllRepositories(), job.pomView.getAllPluginRepositories() );

        for ( final List<RepositoryView> repos : repoLists )
        {
            if ( repos == null || repos.isEmpty() )
            {
                continue;
            }

            for ( final RepositoryView repo : repos )
            {
                if ( !config.isIncludeSnapshotRepos() && !repo.isReleasesEnabled() )
                {
                    logger.debug( "Discarding snapshot repository: {}", repo );
                    continue;
                }

                logger.debug( "Detected POM-declared repository: {}", repo );
                RemoteRepository rr = storeManager.findRemoteRepository( repo.getUrl() );
                if ( rr == null )
                {
                    logger.debug( "Creating new RemoteRepository for: {}", repo );
                    final ProjectVersionRef gav = job.pathInfo.getProjectId();

                    rr = new RemoteRepository( formatId( repo ), repo.getUrl() );
                    rr.setDescription( "Implicitly created repo for: " + repo.getName() + " (" + repo.getId()
                        + ") from repository declaration in POM: " + gav );

                    final String changelog =
                        String.format( "Adding remote repository: %s (url: %s, name: %s), which is implied by the POM: %s (at: %s/%s)",
                                       repo.getId(), repo.getUrl(), repo.getName(), gav, job.transfer.getLocation()
                                                                                                     .getUri(),
                                       job.transfer.getPath() );

                    final ChangeSummary summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, changelog );
                    try
                    {
                        final boolean result = storeManager.storeArtifactStore( rr, summary, true, false );
                        logger.debug( "Stored new RemoteRepository: {}. (successful? {})", rr, result );
                        job.implied.add( rr );
                    }
                    catch ( final AproxDataException e )
                    {
                        logger.error( String.format( "Cannot add implied remote repo: %s from: %s (transfer: %s). Failed to store new remote repository.",
                                                     repo.getUrl(), gav, job.transfer ), e );
                    }
                }
                else
                {
                    logger.debug( "Found existing RemoteRepository: {}", rr );
                }
            }
        }
    }

    private String formatId( final RepositoryView repo )
    {
        //        return "implied-" + repo.getId() + "-" + formatNow();
        return repo.getId();
    }

    //    private String formatNow()
    //    {
    //        return new SimpleDateFormat( "yyyyMMdd_HHmm" ).format( new Date() );
    //    }

    public class ImplicationsJob
    {
        private final FileStorageEvent event;

        private final Transfer transfer;

        private ArtifactStore store;

        private MavenPomView pomView;

        private ArtifactPathInfo pathInfo;

        private ArrayList<ArtifactStore> implied;

        public ImplicationsJob( final FileStorageEvent event )
        {
            this.event = event;
            this.transfer = event.getTransfer();
        }

    }

}
