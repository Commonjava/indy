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
package org.commonjava.indy.pkg.maven.content;

import org.apache.commons.lang.StringUtils;
import org.commonjava.cdi.util.weft.PoolWeftExecutorService;
import org.commonjava.cdi.util.weft.WeftExecutorService;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.conf.DefaultIndyConfiguration;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.content.IndyLocationExpander;
import org.commonjava.indy.content.StoreResource;
import org.commonjava.indy.core.content.DefaultDirectContentAccess;
import org.commonjava.indy.core.content.DefaultDownloadManager;
import org.commonjava.indy.core.content.group.GroupMergeHelper;
import org.commonjava.indy.core.inject.ExpiringMemoryNotFoundCache;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.pkg.maven.content.group.MavenMetadataMerger;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.atlas.maven.ident.ref.SimpleProjectVersionRef;
import org.commonjava.atlas.maven.ident.util.SnapshotUtils;
import org.commonjava.atlas.maven.ident.util.VersionUtils;
import org.commonjava.atlas.maven.ident.version.SingleVersion;
import org.commonjava.atlas.maven.ident.version.part.SnapshotPart;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.maven.internal.type.StandardTypeMapper;
import org.commonjava.maven.galley.maven.model.view.meta.LatestSnapshotView;
import org.commonjava.maven.galley.maven.model.view.meta.MavenMetadataView;
import org.commonjava.maven.galley.maven.model.view.meta.SnapshotArtifactView;
import org.commonjava.maven.galley.maven.model.view.meta.VersioningView;
import org.commonjava.maven.galley.maven.parse.MavenMetadataReader;
import org.commonjava.maven.galley.maven.parse.XMLInfrastructure;
import org.commonjava.maven.galley.maven.spi.type.TypeMapper;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.ListingResult;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.nfc.MemoryNotFoundCache;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.commonjava.maven.galley.testing.core.transport.job.TestListing;
import org.commonjava.maven.galley.testing.maven.GalleyMavenFixture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class MavenMetadataGeneratorTest
{

    @Rule
    public GalleyMavenFixture fixture = new GalleyMavenFixture();

    private MavenMetadataGenerator generator;

    private MemoryStoreDataManager stores;

    private MavenMetadataReader metadataReader;

    private final ChangeSummary summary = new ChangeSummary( "test-user", "test" );

    @Before
    public void setup()
        throws Exception
    {
        stores = new MemoryStoreDataManager( true );

        final LocationExpander locations = new IndyLocationExpander( stores );

        final DefaultIndyConfiguration config = new DefaultIndyConfiguration();
        config.setNotFoundCacheTimeoutSeconds( 1 );
        final ExpiringMemoryNotFoundCache nfc = new ExpiringMemoryNotFoundCache( config );

        WeftExecutorService rescanService =
                        new PoolWeftExecutorService( "test-rescan-executor", (ThreadPoolExecutor) Executors.newCachedThreadPool(), 2, 10f, false,null, null );

        final DownloadManager downloads = new DefaultDownloadManager( stores, fixture.getTransferManager(), locations, null, nfc, rescanService );

        final XMLInfrastructure xml = new XMLInfrastructure();
        final TypeMapper types = new StandardTypeMapper();
        final MavenMetadataMerger merger = new MavenMetadataMerger();
        final GroupMergeHelper helper = new GroupMergeHelper( downloads );

        WeftExecutorService contentAccessService =
                        new PoolWeftExecutorService( "test-content-access-executor", (ThreadPoolExecutor) Executors.newCachedThreadPool(), 2, 10f, false,null, null );
        DefaultDirectContentAccess contentAccess = new DefaultDirectContentAccess( downloads, contentAccessService );

        WeftExecutorService mdService =
                        new PoolWeftExecutorService( "test-md-executor", (ThreadPoolExecutor) Executors.newCachedThreadPool(), 2, 10f, false,null, null );

        generator = new MavenMetadataGenerator( contentAccess, stores, xml, types, merger, helper, new MemoryNotFoundCache(), mdService );

        metadataReader =
            new MavenMetadataReader( xml, locations, fixture.getArtifactMetadataManager(), fixture.getXPathManager() );
    }

    @Test
    public void generateFileContent_SnapshotMetadataWith2Versions()
        throws Exception
    {
        final StoreResource resource = setupSnapshotDirWith2Snapshots();
        final EventMetadata emd = new EventMetadata();

        final Transfer transfer =
            generator.generateFileContent( stores.getArtifactStore( resource.getStoreKey() ),
                                           resource.getChild( "maven-metadata.xml" ).getPath(),
                                           emd );

        assertThat( transfer, notNullValue() );

        final MavenMetadataView metadata =
            metadataReader.readMetadata( new SimpleProjectVersionRef( "org.group", "artifact", "1.0-SNAPSHOT" ),
                                         Collections.singletonList( transfer ), emd );

        assertThat( metadata, notNullValue() );

        final VersioningView versioning = metadata.getVersioning();
        final LatestSnapshotView latestSnapshot = versioning.getLatestSnapshot();
        assertThat( latestSnapshot.isLocalCopy(), equalTo( false ) );

        assertThat( latestSnapshot.getTimestamp(), equalTo( SnapshotUtils.parseSnapshotTimestamp( "20140828.225800" ) ) );
        assertThat( latestSnapshot.getBuildNumber(), equalTo( 1 ) );

        final List<SnapshotArtifactView> snapshots = versioning.getSnapshotArtifacts();
        assertThat( snapshots.size(), equalTo( 4 ) );

        for ( final SnapshotArtifactView snap : snapshots )
        {
            final String extension = snap.getExtension();
            assertThat( extension.equals( "jar" ) || extension.equals( "pom" ), equalTo( true ) );

            final String version = snap.getVersion();
            System.out.println( version );

            final SingleVersion parsed = VersionUtils.createSingleVersion( version );
            assertThat( parsed.isSnapshot(), equalTo( true ) );
            assertThat( parsed.isLocalSnapshot(), equalTo( false ) );

            final SnapshotPart part = parsed.getSnapshotPart();
            final String tstamp = SnapshotUtils.generateSnapshotTimestamp( part.getTimestamp() );
            assertThat( tstamp.equals( "20140828.225800" ) || tstamp.equals( "20140828.221400" ), equalTo( true ) );
        }
    }

    @Test
    public void generateDirContent_SnapshotMetadataWith2Versions()
        throws Exception
    {
        final StoreResource resource = setupSnapshotDirWith2Snapshots();
        final EventMetadata emd = new EventMetadata();

        final List<StoreResource> result =
            generator.generateDirectoryContent( stores.getArtifactStore( resource.getStoreKey() ), resource.getPath(),
                                                Collections.<StoreResource> emptyList(), emd );

        System.out.println( StringUtils.join( result, "\n" ) );

        assertThat( result.size(), equalTo( 3 ) );

        for ( final StoreResource res : result )
        {
            if ( !( res.getPath()
                       .endsWith( MavenMetadataMerger.METADATA_MD5_NAME )
                || res.getPath()
                      .endsWith( MavenMetadataMerger.METADATA_NAME ) || res.getPath()
                                                                           .endsWith( MavenMetadataMerger.METADATA_SHA_NAME ) ) )
            {
                fail( "Invalid generated content: " + res );
            }
        }
    }

    @Test
    public void generateFileContent_VersionsMetadataWith2Versions()
        throws Exception
    {
        final StoreResource resource = setupVersionsStructureWith2Versions();
        final ConcreteResource metadataFile = resource.getChild( "maven-metadata.xml" );

        final Transfer transfer =
            generator.generateFileContent( stores.getArtifactStore( resource.getStoreKey() ), metadataFile.getPath(),
                                           new EventMetadata() );

        assertThat( transfer, notNullValue() );

        final MavenMetadataView metadata =
            metadataReader.readMetadata( new SimpleProjectVersionRef( "org.group", "artifact", "1.0-SNAPSHOT" ),
                                         Collections.singletonList( transfer ), new EventMetadata() );

        assertThat( metadata, notNullValue() );

        final VersioningView versioning = metadata.getVersioning();
        final List<SingleVersion> versions = versioning.getVersions();
        assertThat( versions, notNullValue() );

        assertThat( versions.get( 0 )
                            .renderStandard(), equalTo( "1.0" ) );

        assertThat( versions.get( 1 )
                            .renderStandard(), equalTo( "1.1" ) );

        assertThat( versioning.getReleaseVersion()
                              .renderStandard(), equalTo( "1.1" ) );
        assertThat( versioning.getLatestVersion()
                              .renderStandard(), equalTo( "1.1" ) );
    }

    @Test
    public void generateDirContent_VersionsMetadataWith2Versions()
        throws Exception
    {
        final StoreResource resource = setupVersionsStructureWith2Versions();

        final List<StoreResource> result =
            generator.generateDirectoryContent( stores.getArtifactStore( resource.getStoreKey() ), resource.getPath(),
                                                Collections.<StoreResource> emptyList(), new EventMetadata() );

        System.out.println( StringUtils.join( result, "\n" ) );

        assertThat( result.size(), equalTo( 3 ) );

        for ( final StoreResource res : result )
        {
            if ( !( res.getPath()
                       .endsWith( MavenMetadataMerger.METADATA_MD5_NAME )
                || res.getPath()
                      .endsWith( MavenMetadataMerger.METADATA_NAME ) || res.getPath()
                                                                           .endsWith( MavenMetadataMerger.METADATA_SHA_NAME ) ) )
            {
                fail( "Invalid generated content: " + res );
            }
        }
    }

    private StoreResource setupVersionsStructureWith2Versions()
        throws Exception
    {
        final RemoteRepository store = new RemoteRepository( MAVEN_PKG_KEY,  "testrepo", "http://foo.bar" );
        stores.storeArtifactStore( store, summary, false, true, new EventMetadata() );

        final String path = "org/group/artifact";

        final KeyedLocation location = LocationUtils.toLocation( store );
        final StoreResource resource = new StoreResource( location, path );

        fixture.getTransport()
               .registerListing( resource,
                                 new TestListing( new ListingResult( resource, new String[] { "1.0/", "1.1/" } ) ) );

        ConcreteResource versionDir = ( resource.getChild( "1.0/" ) );
        fixture.getTransport()
               .registerListing( versionDir,
                                 new TestListing( new ListingResult( versionDir, new String[] { "artifact-1.0.jar",
                                     "artifact-1.0.pom" } ) ) );

        versionDir = ( resource.getChild( "1.1/" ) );
        fixture.getTransport()
               .registerListing( versionDir,
                                 new TestListing( new ListingResult( versionDir, new String[] { "artifact-1.1.jar",
                                     "artifact-1.1.pom" } ) ) );

        return resource;
    }

    private StoreResource setupSnapshotDirWith2Snapshots()
        throws Exception
    {
        final RemoteRepository store = new RemoteRepository( MAVEN_PKG_KEY,  "testrepo", "http://foo.bar" );
        stores.storeArtifactStore( store, summary, false, true, new EventMetadata() );

        final String path = "org/group/artifact/1.0-SNAPSHOT";

        final KeyedLocation location = LocationUtils.toLocation( store );
        final StoreResource resource = new StoreResource( location, path );

        final TestListing listing =
            new TestListing( new ListingResult( resource, new String[] { "artifact-1.0-20140828.221400-1.pom",
                "artifact-1.0-20140828.221400-1.jar", "artifact-1.0-20140828.225800-1.pom",
                "artifact-1.0-20140828.225800-1.jar", } ) );

        fixture.getTransport()
               .registerListing( resource, listing );

        return resource;
    }

}
