package org.commonjava.indy.pkg.npm.content;

import org.commonjava.cdi.util.weft.PoolWeftExecutorService;
import org.commonjava.cdi.util.weft.WeftExecutorService;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.conf.DefaultIndyConfiguration;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.content.IndyLocationExpander;
import org.commonjava.indy.content.IndyPathGenerator;
import org.commonjava.indy.content.StoreResource;
import org.commonjava.indy.core.content.DefaultDirectContentAccess;
import org.commonjava.indy.core.content.DefaultDownloadManager;
import org.commonjava.indy.core.content.group.GroupMergeHelper;
import org.commonjava.indy.core.inject.ExpiringMemoryNotFoundCache;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.pkg.npm.content.group.PackageMetadataMerger;
import org.commonjava.indy.pkg.npm.model.PackageMetadata;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.GalleyCore;
import org.commonjava.maven.galley.GalleyCoreBuilder;
import org.commonjava.maven.galley.cache.FileCacheProviderFactory;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.maven.internal.type.StandardTypeMapper;
import org.commonjava.maven.galley.maven.spi.type.TypeMapper;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.nfc.MemoryNotFoundCache;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class PackageMetadataGeneratorTest
{

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private PackageMetadataGenerator generator;

    private MemoryStoreDataManager stores;

    private DownloadManager fileManager;

    private GalleyCore core;

    @Before
    public void setup() throws Exception
    {

        stores = new MemoryStoreDataManager( true );

        core = new GalleyCoreBuilder( new FileCacheProviderFactory( temp.newFolder( "cache" ) ) ).build();

        final DefaultIndyConfiguration config = new DefaultIndyConfiguration();
        config.setNotFoundCacheTimeoutSeconds( 1 );

        final ExpiringMemoryNotFoundCache nfc = new ExpiringMemoryNotFoundCache( config );

        WeftExecutorService rescanService =
                        new PoolWeftExecutorService( "test-rescan-executor", (ThreadPoolExecutor) Executors.newCachedThreadPool(), 2, 10f, false,null, null );

        final LocationExpander locations = new IndyLocationExpander( stores );
        final PackageMetadataMerger merger = new PackageMetadataMerger(  );
        final TypeMapper types = new StandardTypeMapper();

        final DownloadManager
                        downloads = new DefaultDownloadManager( stores, core.getTransferManager(), locations, null, nfc, rescanService );
        WeftExecutorService contentAccessService =
                        new PoolWeftExecutorService( "test-content-access-executor", (ThreadPoolExecutor) Executors.newCachedThreadPool(), 2, 10f, false, null, null );
        DefaultDirectContentAccess contentAccess = new DefaultDirectContentAccess( downloads, contentAccessService );

        final GroupMergeHelper helper = new GroupMergeHelper( downloads );

        fileManager = new DefaultDownloadManager( stores, core.getTransferManager(), core.getLocationExpander(), rescanService );
        generator = new PackageMetadataGenerator( contentAccess, stores, types, merger, helper, new MemoryNotFoundCache(), new IndyPathGenerator( Collections.singleton( new NPMStoragePathCalculator() ) ) );

    }

    @Test
    public void generateMetadataWhenMissing() throws Exception
    {
        ChangeSummary summary = new ChangeSummary( "test","Init NPM hosted repo." );
        final HostedRepository hostedRepository = new HostedRepository( NPM_PKG_KEY, "npm-builds" );
        initStore(hostedRepository, summary);

        final KeyedLocation location = LocationUtils.toLocation( hostedRepository );

        storeFile( location, "jquery/-/jquery-9.0.5.tgz", "tarball/version-1.tgz");
        storeFile( location, "jquery/-/jquery-9.0.6.tgz", "tarball/version-2.tgz");
        storeFile( location, "jquery/9.0.5", "metadata/version-1.json" );
        storeFile( location, "jquery/9.0.6", "metadata/version-2.json" );

        // Check the package metadata before generation.
        Transfer before = fileManager.retrieve( hostedRepository, "jquery/package.json" );
        assertNull(before);

        Transfer metadataFile = generator.generateFileContent( hostedRepository, "jquery/package.json", new EventMetadata(  ) );
        assertNotNull(metadataFile);
        final IndyObjectMapper mapper = new IndyObjectMapper( true );
        try ( InputStream input = metadataFile.openInputStream() )
        {
            PackageMetadata packageMetadata = mapper.readValue( input, PackageMetadata.class );

            assertNotNull( packageMetadata );
            assertEquals( 2, packageMetadata.getVersions().size());
            assertEquals("Unexpected package name.", "json", packageMetadata.getName());
            assertEquals( "Unexpected latest version.","9.0.6", packageMetadata.getDistTags().getLatest() );
        }

        // Check the package metadata after generation.
        Transfer after = fileManager.retrieve( hostedRepository, "jquery/package.json" );
        assertNotNull(after);
    }

    private void initStore( HostedRepository hostedRepository, ChangeSummary summary ) throws Exception
    {
        stores.storeArtifactStore( hostedRepository, summary, false, true, new EventMetadata(  ) );
    }

    private void storeFile( KeyedLocation location, String targetPath, String sourcePath ) throws Exception
    {
        final StoreResource resource = new StoreResource( location, targetPath );
        try( InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream( sourcePath ) )
        {
            if ( in == null )
            {
                fail("Cannot find the test resource:" + sourcePath + " on classpath.");
            }

            core.getTransferManager().store( resource, in );
        }
    }


}
