/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildState;
import com.redhat.red.build.koji.model.xmlrpc.KojiSessionInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiTagInfo;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.commonjava.cdi.util.weft.Locker;
import org.commonjava.indy.IndyMetricsNames;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.koji.conf.IndyKojiConfig;
import org.commonjava.indy.koji.inject.KojiMavenVersionMetadataCache;
import org.commonjava.indy.koji.inject.KojiMavenVersionMetadataLocks;
import org.commonjava.indy.koji.metrics.IndyMetricsKojiNames;
import org.commonjava.indy.measure.annotation.IndyMetrics;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.measure.annotation.MetricNamed;
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
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
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

    @KojiMavenVersionMetadataLocks
    @Inject
    private Locker<ProjectRef> versionMetadataLocks;

    protected KojiMavenMetadataProvider(){}

    public KojiMavenMetadataProvider( CacheHandle<ProjectRef, Metadata> versionMetadata, KojiClient kojiClient,
                                      KojiBuildAuthority buildAuthority, IndyKojiConfig kojiConfig )
    {
        this.versionMetadata = versionMetadata;
        this.kojiClient = kojiClient;
        this.buildAuthority = buildAuthority;
        this.kojiConfig = kojiConfig;
    }

    @Override
    @IndyMetrics( measure = @Measure( timers = @MetricNamed( name =
                    IndyMetricsKojiNames.METHOD_MAVENMETADATA_GETMETADATA
                                    + IndyMetricsNames.TIMER ), meters = @MetricNamed( name =
                    IndyMetricsKojiNames.METHOD_MAVENMETADATA_GETMETADATA + IndyMetricsNames.METER ) ) )
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

        ProjectRef ref = null;
        try
        {
            ref = new SimpleProjectRef( groupId, artifactId );
        }
        catch ( InvalidRefException e )
        {
            logger.warn( "Not a valid Maven GA: {}:{}. Skipping Koji metadata retrieval.", groupId, artifactId );
        }

        if ( ref == null )
        {
            logger.debug( "Could not render a valid Maven GA for path: '{}'", path );
            return null;
        }

        ProjectRef ga = ref;
        return versionMetadataLocks.lockAnd( ga, kojiConfig.getLockTimeoutSeconds(), k -> {
            Metadata metadata = versionMetadata.get( ga );
            if ( metadata == null )
            {
                try
                {
                    metadata = executeKojiMetadataLookup( ga, path );
                }
                catch ( KojiClientException e )
                {
                    Throwable cause = e.getCause();
                    logger.error(
                            String.format( "Failed to retrieve version metadata for: %s from Koji. Reason: %s", ga,
                                           e.getMessage() ), e );

                    if ( cause instanceof RuntimeException )
                    {
                        logger.error( "Previous exception's nested cause was a RuntimeException variant:", cause );
                    }

                    metadata = null;
                }

                if ( metadata != null )
                {
                    Metadata md = metadata;

                    // FIXME: Need a way to listen for cache expiration and re-request this?
                    versionMetadata.execute( ( cache ) -> cache.getAdvancedCache()
                                                               .put( ga, md, kojiConfig.getMetadataTimeoutSeconds(),
                                                                     TimeUnit.SECONDS ) );
                }
                else
                {
                    logger.debug( "Returning null metadata result for unknown reason (path: '{}')", path );
                }
            }

            return metadata;
        }, (k,lock) -> {
            logger.error( "Failed to acquire Koji GA version metadata lock on: '{}' in {} seconds.", ga,
                          kojiConfig.getLockTimeoutSeconds() );
            return false;
        } );
    }

    private Metadata executeKojiMetadataLookup(ProjectRef ga, String path )
            throws KojiClientException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        return kojiClient.withKojiSession( ( session ) -> {

            // short-term caches to help improve performance a bit by avoiding xml-rpc calls.
            Map<Integer, KojiBuildArchiveCollection> seenBuildArchives = new HashMap<>();
            Set<Integer> seenBuilds = new HashSet<>();

            List<KojiArchiveInfo> archives = kojiClient.listArchivesMatching( ga, session );

            Set<SingleVersion> versions = new HashSet<>();
            for ( KojiArchiveInfo archive : archives )
            {
                ArchiveScan scan = scanArchive( archive, session, versions, seenBuilds );
                if ( scan.isDisqualified() )
                {
                    continue;
                }

                KojiBuildInfo build = scan.getBuild();
                SingleVersion singleVersion = scan.getSingleVersion();

                boolean buildAllowed = false;
                if ( !kojiConfig.isTagPatternsEnabled() )
                {
                    buildAllowed = true;
                }
                else
                {
                    logger.trace( "Checking for builds/tags of: {}", archive );

                    List<KojiTagInfo> tags = kojiClient.listTags( build.getId(), session );
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
                }

                logger.debug(
                        "Checking if build passed tag whitelist check and doesn't collide with something in authority store (if configured)..." );

                if ( buildAllowed && buildAuthority.isAuthorized( path, new EventMetadata(), ga, build,
                                                                  session, seenBuildArchives ) )
                {
                    try
                    {
                        logger.debug( "Adding version: {} for: {}", archive.getVersion(), path );
                        versions.add( singleVersion );
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
            md.setGroupId( ga.getGroupId() );
            md.setArtifactId( ga.getArtifactId() );

            Versioning versioning = new Versioning();
            versioning.setRelease( sortedVersions.get( versions.size() - 1 ).renderStandard() );
            versioning.setLatest( sortedVersions.get( versions.size() - 1 ).renderStandard() );
            versioning.setVersions( sortedVersions.stream()
                                                  .map( SingleVersion::renderStandard )
                                                  .collect( Collectors.toList() ) );

            Date lastUpdated = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ).getTime();
            versioning.setLastUpdated( new SimpleDateFormat( LAST_UPDATED_FORMAT ).format( lastUpdated ) );

            md.setVersioning( versioning );

            return md;
        } );
    }

    private ArchiveScan scanArchive( final KojiArchiveInfo archive, final KojiSessionInfo session,
                                       final Set<SingleVersion> versions, final Set<Integer> seenBuilds )
            throws KojiClientException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        ArchiveScan scan = new ArchiveScan();

        if ( !archive.getFilename().endsWith( ".pom" ) )
        {
            logger.debug( "Skipping non-POM: {}", archive.getFilename() );
            scan.setDisqualified( true );
            return scan;
        }

        if ( !isVerSignedAllowed( archive.getVersion() ) )
        {
            logger.debug( "version filter pattern not matched: {}", archive.getVersion() );
            scan.setDisqualified( true );
            return scan;
        }

        SingleVersion singleVersion = null;
        try
        {
            singleVersion = VersionUtils.createSingleVersion( archive.getVersion() );
            scan.setSingleVersion( singleVersion );
        }
        catch ( InvalidVersionSpecificationException ivse )
        {
            logger.warn( "Skipping mal-formatted version: {}, relPath: {}, buildId: {}", archive.getVersion(),
                         archive.getRelPath(), archive.getBuildId() );
            scan.setDisqualified( true );
            return scan;
        }

        if ( versions.contains( singleVersion ) )
        {
            logger.debug( "Skipping already collected version: {}", archive.getVersion() );
            scan.setDisqualified( true );
            return scan;
        }

        KojiBuildInfo build = null;
        if ( seenBuilds.contains( archive.getBuildId() ) )
        {
            logger.debug( "Skipping already seen build: {}", archive.getBuildId() );
            scan.setDisqualified( true );
            return scan;
        }
        else
        {
            build = kojiClient.getBuildInfo( archive.getBuildId(), session );
            seenBuilds.add( archive.getBuildId() );
            scan.setBuild( build );
        }

        if ( build == null )
        {
            logger.debug( "Cannot retrieve build info: {}. Skipping: {}", archive.getBuildId(), archive.getFilename() );
            scan.setDisqualified( true );
            return scan;
        }

        if ( build.getBuildState() != KojiBuildState.COMPLETE )
        {
            logger.debug( "Build: {} is not completed. The state is {}. Skipping.", build.getNvr(),
                          build.getBuildState() );
            scan.setDisqualified( true );
            return scan;
        }

        if ( build.getTaskId() == null )
        {
            logger.debug( "Build: {} is not a real build. It looks like a binary import. Skipping.", build.getNvr() );
            // This is not a real build, it's a binary import.
            scan.setDisqualified( true );
            return scan;
        }

        return scan;
    }

    private static final class ArchiveScan
    {
        private boolean disqualified = false;
        private KojiBuildInfo build;
        private SingleVersion singleVersion;

        public boolean isDisqualified()
        {
            return disqualified;
        }

        public void setDisqualified( final boolean disqualified )
        {
            this.disqualified = disqualified;
        }

        public KojiBuildInfo getBuild()
        {
            return build;
        }

        public void setBuild( final KojiBuildInfo build )
        {
            this.build = build;
        }

        public SingleVersion getSingleVersion()
        {
            return singleVersion;
        }

        public void setSingleVersion( final SingleVersion singleVersion )
        {
            this.singleVersion = singleVersion;
        }
    }

    private boolean isVerSignedAllowed ( String version )
    {
        final String versionFilter = kojiConfig.getVersionFilter();

        if ( versionFilter == null )
        {
            return true;
        }

        if ( Pattern.compile( versionFilter ).matcher( version ).matches())
        {
            return true;
        }
        return false;
    }
}
