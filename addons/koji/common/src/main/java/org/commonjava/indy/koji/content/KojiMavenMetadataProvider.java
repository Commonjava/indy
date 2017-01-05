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
package org.commonjava.indy.koji.content;

import com.redhat.red.build.koji.KojiClient;
import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.model.xmlrpc.KojiArchiveInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildArchiveCollection;
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiTagInfo;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.koji.conf.IndyKojiConfig;
import org.commonjava.indy.koji.inject.KojiMavenVersionMetadataCache;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.pkg.maven.content.group.MavenMetadataProvider;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.maven.atlas.ident.ref.InvalidRefException;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.atlas.ident.util.VersionUtils;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.commonjava.indy.model.core.StoreType.group;

/**
 * Created by jdcasey on 11/1/16.
 */
@ApplicationScoped
public class KojiMavenMetadataProvider
        implements MavenMetadataProvider
{

    private static final java.lang.String LAST_UPDATED_FORMAT = "yyyyMMddHHmmss";

    @Inject
    @KojiMavenVersionMetadataCache
    private CacheHandle<ProjectRef, Metadata> versionMetadata;

    @Inject
    private KojiClient kojiClient;

    @Inject
    private IndyKojiConfig kojiConfig;

    @Inject
    private KojiBuildAuthority buildAuthority;

    private final Map<ProjectRef, ReentrantLock> versionMetadataLocks = new WeakHashMap<>();

    protected KojiMavenMetadataProvider(){}

    public KojiMavenMetadataProvider( CacheHandle<ProjectRef, Metadata> versionMetadata, KojiClient kojiClient,
                                      KojiBuildAuthority buildAuthority, IndyKojiConfig kojiConfig )
    {
        this.versionMetadata = versionMetadata;
        this.kojiClient = kojiClient;
        this.buildAuthority = buildAuthority;
        this.kojiConfig = kojiConfig;
    }

    public Metadata getMetadata( StoreKey targetKey, String path )
            throws IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        if ( group != targetKey.getType() )
        {
            logger.debug( "Not a group. Cannot supplement with metadata from Koji builds" );
            return null;
        }

        if ( !kojiConfig.isEnabled() )
        {
            logger.debug( "Koji add-on is disabled." );
            return null;
        }

        if ( !kojiConfig.isEnabledFor( targetKey.getName() ) )
        {
            logger.debug( "Koji integration is not enabled for group: {}", targetKey );
            return null;
        }

        File mdFile = new File( path );
        File artifactDir = mdFile.getParentFile();
        File groupDir = artifactDir == null ? null : artifactDir.getParentFile();

        if ( artifactDir == null || groupDir == null )
        {
            logger.debug( "Invalid groupId / artifactId directory structure: '{}' / '{}'", groupDir, artifactDir );
            return null;
        }

        String groupId = groupDir.getPath().replace( File.separatorChar, '.' );
        String artifactId = artifactDir.getName();

        ProjectRef ga = null;
        try
        {
            ga = new SimpleProjectRef( groupId, artifactId );
        }
        catch ( InvalidRefException e )
        {
            logger.debug( "Not a valid Maven GA: {}:{}. Skipping Koji metadata retrieval.", groupId, artifactId );
        }

        if ( ga == null )
        {
            logger.debug( "Could not render a valid Maven GA for path: '{}'", path );
            return null;
        }

        ReentrantLock lock;
        synchronized ( versionMetadataLocks )
        {
            lock = versionMetadataLocks.get( ga );
            if ( lock == null )
            {
                lock = new ReentrantLock();
                versionMetadataLocks.put( ga, lock );
            }
        }

        try
        {
            boolean locked = lock.tryLock( kojiConfig.getLockTimeoutSeconds(), TimeUnit.SECONDS );
            if ( !locked )
            {
                throw new IndyWorkflowException(
                        "Failed to acquire Koji GA version metadata lock on: %s in %d seconds.", ga,
                        kojiConfig.getLockTimeoutSeconds() );
            }

            Metadata metadata = versionMetadata.get( ga );
            ProjectRef ref = ga;
            if ( metadata == null )
            {
                try
                {
                    metadata = kojiClient.withKojiSession( ( session ) -> {

                        // short-term caches to help improve performance a bit by avoiding xml-rpc calls.
                        Map<Integer, KojiBuildArchiveCollection> seenBuildArchives = new HashMap<>();
                        Map<Integer, KojiBuildInfo> seenBuilds = new HashMap<>();
                        Map<Integer, List<KojiTagInfo>> seenBuildTags = new HashMap<>();

                        List<KojiArchiveInfo> archives = kojiClient.listArchivesMatching( ref, session );

                        Set<SingleVersion> versions = new HashSet<>();
                        for ( KojiArchiveInfo archive : archives )
                        {
                            if ( !archive.getFilename().endsWith( ".pom" ) )
                            {
                                logger.debug( "Skipping non-POM: {}", archive.getFilename() );
                                continue;
                            }

                            KojiBuildInfo build = seenBuilds.get(archive.getBuildId());
                            if( build == null ){
                                build = kojiClient.getBuildInfo( archive.getBuildId(), session );
                                seenBuilds.put( archive.getBuildId(), build );
                            }

                            if ( build == null )
                            {
                                logger.debug( "Cannot retrieve build info: {}. Skipping: {}", archive.getBuildId(),
                                              archive.getFilename() );
                                continue;
                            }

                            if ( build.getTaskId() == null )
                            {
                                logger.debug( "Build: {} is not a real build. It looks like a binary import. Skipping.",
                                              build.getNvr() );
                                // This is not a real build, it's a binary import.
                                continue;
                            }

                            logger.debug( "Checking for builds/tags of: {}", archive );
                            List<KojiTagInfo> tags = seenBuildTags.get( archive.getBuildId() );
                            if ( tags == null )
                            {
                                tags = kojiClient.listTags( archive.getBuildId(), session );
                                seenBuildTags.put( archive.getBuildId(), tags );
                            }

                            boolean buildAllowed = false;
                            for ( KojiTagInfo tag : tags )
                            {
                                if ( kojiConfig.isTagAllowed( tag.getName() ) )
                                {
                                    logger.debug( "Koji tag: {} is allowed for proxying.", tag.getName() );
                                    buildAllowed = true;
                                    break;
                                }
                                else
                                {
                                    logger.debug( "Koji tag: {} is not allowed for proxying.", tag.getName() );
                                }
                            }

                            logger.debug(
                                    "Checking if build passed tag whitelist check and doesn't collide with something in authority store (if configured)..." );

                            if ( buildAllowed && buildAuthority.isAuthorized( path, new EventMetadata(), ref, build, session, seenBuildArchives ) )
                            {
                                try
                                {
                                    logger.debug( "Adding version: {} for: {}", archive.getVersion(), path );
                                    versions.add( VersionUtils.createSingleVersion( archive.getVersion() ) );
                                }
                                catch ( InvalidVersionSpecificationException e )
                                {
                                    logger.warn( String.format(
                                            "Encountered invalid version: %s for archive: %s. Reason: %s",
                                            archive.getVersion(), archive.getArchiveId(), e.getMessage() ), e );
                                }
                            }
                        }

                        if ( versions.isEmpty() )
                        {
                            logger.debug( "No versions found in Koji builds for metadata: {}", path );
                            return null;
                        }

                        List<SingleVersion> sortedVersions = new ArrayList<>( versions );
                        Collections.sort( sortedVersions );

                        Metadata md = new Metadata();
                        md.setGroupId( ref.getGroupId() );
                        md.setArtifactId( ref.getArtifactId() );

                        Versioning versioning = new Versioning();
                        versioning.setRelease( sortedVersions.get( versions.size() - 1 ).renderStandard() );
                        versioning.setLatest( sortedVersions.get( versions.size() - 1 ).renderStandard() );
                        versioning.setVersions(
                                sortedVersions.stream().map( SingleVersion::renderStandard ).collect( Collectors.toList() ) );

                        Date lastUpdated = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ).getTime();
                        versioning.setLastUpdated( new SimpleDateFormat( LAST_UPDATED_FORMAT ).format( lastUpdated ) );

                        md.setVersioning( versioning );

                        return md;
                    } );
                }
                catch ( KojiClientException e )
                {
                    throw new IndyWorkflowException(
                            "Failed to retrieve version metadata for: %s from Koji. Reason: %s", e, ga,
                            e.getMessage() );
                }

                Metadata md = metadata;

                if ( metadata != null )
                {
                    // FIXME: Need a way to listen for cache expiration and re-request this?
                    versionMetadata.execute( ( cache ) -> cache.getAdvancedCache()
                                                               .put( ref, md, kojiConfig.getMetadataTimeoutSeconds(),
                                                                     TimeUnit.SECONDS ) );
                }
            }

            return metadata;
        }
        catch ( InterruptedException e )
        {
            logger.warn( "Interrupted waiting for Koji GA version metadata lock on target: {}", ga );
        }
        finally
        {
            lock.unlock();
        }

        logger.debug( "Returning null metadata result for unknown reason (path: '{}')", path );
        return null;
    }
}
