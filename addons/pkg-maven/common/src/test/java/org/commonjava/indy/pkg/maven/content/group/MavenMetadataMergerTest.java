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
package org.commonjava.indy.pkg.maven.content.group;

import org.commonjava.maven.galley.cache.FileCacheProvider;
import org.commonjava.maven.galley.event.NoOpFileEventManager;
import org.commonjava.maven.galley.io.HashedLocationPathGenerator;
import org.commonjava.maven.galley.io.NoOpTransferDecorator;
import org.commonjava.maven.galley.io.TransferDecoratorManager;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Created by jdcasey on 11/2/16.
 */
@Ignore("Obsolete methods; providers are handled in MavenMetadataGenerator now, and we don't merge directly to file now")
public class MavenMetadataMergerTest
{
    private static final String VERSION_META = "metadata/version/";

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private CacheProvider cacheProvider;

    @Before
    public void setup()
            throws Exception
    {
        cacheProvider = new FileCacheProvider( temp.newFolder( "cache" ), new HashedLocationPathGenerator(),
                                               new NoOpFileEventManager(), new TransferDecoratorManager( new NoOpTransferDecorator() ), false );
    }

//    @Test
//    public void mergeTwoSimpleVersionMetadataFiles()
//            throws Exception
//    {
//        String path = "org/foo/bar/maven-metadata.xml";
//        HostedRepository h1 = new HostedRepository( "test-hosted-1" );
//        HostedRepository h2 = new HostedRepository( "test-hosted-2" );
//
//        Transfer t1 = cacheProvider.getTransfer( new ConcreteResource( LocationUtils.toLocation( h1 ), path ) );
//        initTestData( t1, VERSION_META + "simple-1.xml" );
//
//        Transfer t2 = cacheProvider.getTransfer( new ConcreteResource( LocationUtils.toLocation( h2 ), path ) );
//        initTestData( t2, VERSION_META + "simple-2.xml" );
//
//        Group g = new Group( "test-group", h1.getKey(), h2.getKey() );
//
//        List<Transfer> sources = Arrays.asList( t1, t2 );
//
//        byte[] output =
//                new MavenMetadataMerger().merge( sources, g, path );
//
//        Metadata merged = new MetadataXpp3Reader().read( new ByteArrayInputStream( output ) );
//
//        assertThat( merged.getGroupId(), equalTo( "org.foo" ) );
//        assertThat( merged.getArtifactId(), equalTo( "bar" ) );
//
//        Versioning versioning = merged.getVersioning();
//        assertThat( versioning, notNullValue() );
//
//        List<String> versions = versioning.getVersions();
//        assertThat( versions, notNullValue() );
//        assertThat( versions.size(), equalTo( 2 ) );
//
//        assertThat( versioning.getRelease(), equalTo( "1.1" ) );
//        assertThat( versioning.getLatest(), equalTo( "1.1" ) );
//
//        int idx=0;
//        assertThat( versions.get( idx ), equalTo( "1.0" ) );
//
//        idx++;
//        assertThat( versions.get( idx ), equalTo( "1.1" ) );
//    }
//
//    @Test
//    public void mergeTwoFilesWithInterleavedVersions()
//            throws Exception
//    {
//        String path = "org/foo/bar/maven-metadata.xml";
//        HostedRepository h1 = new HostedRepository( "test-hosted-1" );
//        HostedRepository h2 = new HostedRepository( "test-hosted-2" );
//
//        Transfer t1 = cacheProvider.getTransfer( new ConcreteResource( LocationUtils.toLocation( h1 ), path ) );
//        initTestData( t1, VERSION_META + "simple-skip.xml" );
//
//        Transfer t2 = cacheProvider.getTransfer( new ConcreteResource( LocationUtils.toLocation( h2 ), path ) );
//        initTestData( t2, VERSION_META + "simple-2.xml" );
//
//        Group g = new Group( "test-group", h1.getKey(), h2.getKey() );
//
//        List<Transfer> sources = Arrays.asList( t1, t2 );
//
//        byte[] output =
//                new MavenMetadataMerger().merge( sources, g, path );
//
//        Metadata merged = new MetadataXpp3Reader().read( new ByteArrayInputStream( output ) );
//
//        assertThat( merged.getGroupId(), equalTo( "org.foo" ) );
//        assertThat( merged.getArtifactId(), equalTo( "bar" ) );
//
//        Versioning versioning = merged.getVersioning();
//        assertThat( versioning, notNullValue() );
//
//        List<String> versions = versioning.getVersions();
//        assertThat( versions, notNullValue() );
//        assertThat( versions.size(), equalTo( 3 ) );
//
//        assertThat( versioning.getRelease(), equalTo( "1.2" ) );
//        assertThat( versioning.getLatest(), equalTo( "1.2" ) );
//
//        int idx=0;
//        assertThat( versions.get( idx ), equalTo( "1.0" ) );
//
//        idx++;
//        assertThat( versions.get( idx ), equalTo( "1.1" ) );
//
//        idx++;
//        assertThat( versions.get( idx ), equalTo( "1.2" ) );
//    }
//
//    @Test
//    public void mergeWhenOneTransferIsMissing()
//            throws Exception
//    {
//        String path = "org/foo/bar/maven-metadata.xml";
//        HostedRepository h1 = new HostedRepository( "test-hosted-1" );
//        HostedRepository h2 = new HostedRepository( "test-hosted-2" );
//
//        Transfer t1 = cacheProvider.getTransfer( new ConcreteResource( LocationUtils.toLocation( h1 ), path ) );
//        initTestData( t1, VERSION_META + "simple-1.xml" );
//
//        Transfer t2 = cacheProvider.getTransfer( new ConcreteResource( LocationUtils.toLocation( h2 ), path ) );
//
//        Group g = new Group( "test-group", h1.getKey(), h2.getKey() );
//
//        List<Transfer> sources = Arrays.asList( t1, t2 );
//
//        byte[] output =
//                new MavenMetadataMerger().merge( sources, g, path );
//
//        Metadata merged = new MetadataXpp3Reader().read( new ByteArrayInputStream( output ) );
//
//        assertThat( merged.getGroupId(), equalTo( "org.foo" ) );
//        assertThat( merged.getArtifactId(), equalTo( "bar" ) );
//
//        Versioning versioning = merged.getVersioning();
//        assertThat( versioning, notNullValue() );
//
//        List<String> versions = versioning.getVersions();
//        assertThat( versions, notNullValue() );
//        assertThat( versions.size(), equalTo( 1 ) );
//
//        assertThat( versioning.getRelease(), equalTo( "1.0" ) );
//        assertThat( versioning.getLatest(), equalTo( "1.0" ) );
//
//        int idx=0;
//        assertThat( versions.get( idx ), equalTo( "1.0" ) );
//    }
//
//    @Test
//    public void mergeWhenOneTransferIsInvalidXml()
//            throws Exception
//    {
//        String path = "org/foo/bar/maven-metadata.xml";
//        HostedRepository h1 = new HostedRepository( "test-hosted-1" );
//        HostedRepository h2 = new HostedRepository( "test-hosted-2" );
//
//        Transfer t1 = cacheProvider.getTransfer( new ConcreteResource( LocationUtils.toLocation( h1 ), path ) );
//        initTestData( t1, VERSION_META + "simple-1.xml" );
//
//        Transfer t2 = cacheProvider.getTransfer( new ConcreteResource( LocationUtils.toLocation( h2 ), path ) );
//        initTestData( t2, VERSION_META + "invalid.xml" );
//
//        Group g = new Group( "test-group", h1.getKey(), h2.getKey() );
//
//        List<Transfer> sources = Arrays.asList( t1, t2 );
//
//        byte[] output =
//                new MavenMetadataMerger().merge( sources, g, path );
//
//        Metadata merged = new MetadataXpp3Reader().read( new ByteArrayInputStream( output ) );
//
//        assertThat( merged.getGroupId(), equalTo( "org.foo" ) );
//        assertThat( merged.getArtifactId(), equalTo( "bar" ) );
//
//        Versioning versioning = merged.getVersioning();
//        assertThat( versioning, notNullValue() );
//
//        List<String> versions = versioning.getVersions();
//        assertThat( versions, notNullValue() );
//        assertThat( versions.size(), equalTo( 1 ) );
//
//        assertThat( versioning.getRelease(), equalTo( "1.0" ) );
//        assertThat( versioning.getLatest(), equalTo( "1.0" ) );
//
//        int idx=0;
//        assertThat( versions.get( idx ), equalTo( "1.0" ) );
//    }
//
//    @Test
//    public void mergeOneTransferWithProviderContent()
//            throws Exception
//    {
//        String path = "org/foo/bar/maven-metadata.xml";
//        HostedRepository h1 = new HostedRepository( "test-hosted-1" );
//
//        Transfer t1 = cacheProvider.getTransfer( new ConcreteResource( LocationUtils.toLocation( h1 ), path ) );
//        initTestData( t1, VERSION_META + "simple-1.xml" );
//
//        Group g = new Group( "test-group", h1.getKey() );
//
//        List<Transfer> sources = Arrays.asList( t1 );
//
//        Versioning providedVersioning = new Versioning();
//        providedVersioning.setLatest( "1.1" );
//        providedVersioning.setRelease( "1.1" );
//        providedVersioning.addVersion( "1.1" );
//
//        Metadata provided = new Metadata();
//        provided.setGroupId( "org.foo" );
//        provided.setArtifactId( "bar" );
//        provided.setVersioning( providedVersioning );
//
//        TestMavenMetadataProvider testProvider = new TestMavenMetadataProvider( provided );
//
//        byte[] output =
//                new MavenMetadataMerger( Collections.singletonList( testProvider ) ).merge( sources, g, path );
//
//        Metadata merged = new MetadataXpp3Reader().read( new ByteArrayInputStream( output ) );
//
//        assertThat( merged.getGroupId(), equalTo( "org.foo" ) );
//        assertThat( merged.getArtifactId(), equalTo( "bar" ) );
//
//        Versioning versioning = merged.getVersioning();
//        assertThat( versioning, notNullValue() );
//
//        List<String> versions = versioning.getVersions();
//        assertThat( versions, notNullValue() );
//        assertThat( versions.size(), equalTo( 2 ) );
//
//        assertThat( versioning.getRelease(), equalTo( "1.1" ) );
//        assertThat( versioning.getLatest(), equalTo( "1.1" ) );
//
//        int idx=0;
//        assertThat( versions.get( idx ), equalTo( "1.0" ) );
//        idx++;
//        assertThat( versions.get( idx ), equalTo( "1.1" ) );
//    }
//
//    @Test
//    public void mergeOneTransferWithProviderError()
//            throws Exception
//    {
//        String path = "org/foo/bar/maven-metadata.xml";
//        HostedRepository h1 = new HostedRepository( "test-hosted-1" );
//
//        Transfer t1 = cacheProvider.getTransfer( new ConcreteResource( LocationUtils.toLocation( h1 ), path ) );
//        initTestData( t1, VERSION_META + "simple-1.xml" );
//
//        Group g = new Group( "test-group", h1.getKey() );
//
//        List<Transfer> sources = Arrays.asList( t1 );
//
//        TestMavenMetadataProvider testProvider =
//                new TestMavenMetadataProvider( new IndyWorkflowException( "Failed to get provider content" ) );
//
//        byte[] output =
//                new MavenMetadataMerger( Collections.singletonList( testProvider ) ).merge( sources, g, path );
//
//        Metadata merged = new MetadataXpp3Reader().read( new ByteArrayInputStream( output ) );
//
//        assertThat( merged.getGroupId(), equalTo( "org.foo" ) );
//        assertThat( merged.getArtifactId(), equalTo( "bar" ) );
//
//        Versioning versioning = merged.getVersioning();
//        assertThat( versioning, notNullValue() );
//
//        List<String> versions = versioning.getVersions();
//        assertThat( versions, notNullValue() );
//        assertThat( versions.size(), equalTo( 1 ) );
//
//        assertThat( versioning.getRelease(), equalTo( "1.0" ) );
//        assertThat( versioning.getLatest(), equalTo( "1.0" ) );
//
//        int idx=0;
//        assertThat( versions.get( idx ), equalTo( "1.0" ) );
//    }
//
//    private void initTestData( Transfer transfer, String resourcePath )
//            throws IOException
//    {
//        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream( resourcePath );
//             OutputStream out = transfer.openOutputStream( TransferOperation.DOWNLOAD, false ))
//        {
//
//            if ( in == null )
//            {
//                fail( "Cannot find test resource: " + resourcePath + " on classpath!" );
//            }
//
//            IOUtils.copy( in, out );
//        }
//    }
//
//    private static final class TestMavenMetadataProvider
//        implements MavenMetadataProvider
//    {
//        private Metadata provided;
//
//        private IndyWorkflowException error;
//
//        public TestMavenMetadataProvider( Metadata provided )
//        {
//            this.provided = provided;
//        }
//
//        public TestMavenMetadataProvider( IndyWorkflowException error )
//        {
//            this.error = error;
//        }
//
//        @Override
//        public Metadata getMetadata( StoreKey targetStore, String path )
//                throws IndyWorkflowException
//        {
//            if ( error != null )
//            {
//                throw error;
//            }
//            return provided;
//        }
//    }
}
