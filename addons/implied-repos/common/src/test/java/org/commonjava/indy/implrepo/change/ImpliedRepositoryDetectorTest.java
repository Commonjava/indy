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
package org.commonjava.indy.implrepo.change;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.ArtifactStoreValidator;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.implrepo.conf.ImpliedRepoConfig;
import org.commonjava.indy.implrepo.data.ImpliedRepoMetadataManager;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.model.galley.RepositoryLocation;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.indy.subsys.datafile.change.DataFileEventManager;
import org.commonjava.indy.subsys.template.ScriptEngine;
import org.commonjava.maven.galley.auth.MemoryPasswordManager;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.event.FileStorageEvent;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.testing.maven.GalleyMavenFixture;
import org.commonjava.maven.galley.transport.htcli.HttpClientTransport;
import org.commonjava.maven.galley.transport.htcli.HttpImpl;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Collections;
import java.util.concurrent.Executors;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ImpliedRepositoryDetectorTest
{
    public static final class TestImpliedRepositoryDetector
            extends ImpliedRepositoryDetector
    {
        public TestImpliedRepositoryDetector( MavenPomReader pomReader, StoreDataManager storeManager,
                                              ImpliedRepoMetadataManager metadataManager,
                                              ArtifactStoreValidator remoteValidator, ScriptEngine scriptEngine,
                                              ImpliedRepoConfig config,
                                              IndyObjectMapper mapper )
        {
            super( pomReader, storeManager, metadataManager, remoteValidator, scriptEngine,
                   Executors.newSingleThreadExecutor(), config, mapper );
        }
    }

    private TestImpliedRepositoryDetector detector;

    private StoreDataManager storeManager;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Rule
    public ExpectationServer server = new ExpectationServer();

    private GalleyMavenFixture fixture;

    private static final String GROUP_NAME = "group";

    private static final String REMOTE_NAME = "test";

    private ImpliedRepoMetadataManager metadataManager;

    private ArtifactStoreValidator validator;

    private ChangeSummary summary;

    @Before
    public void setup()
            throws Throwable
    {
        fixture = new GalleyMavenFixture( temp );
        fixture.initGalley();

        fixture.withEnabledTransports(
                new HttpClientTransport( new HttpImpl( new MemoryPasswordManager() ) ) );

        fixture.before();

        storeManager = new MemoryStoreDataManager( true );

        metadataManager = new ImpliedRepoMetadataManager( new IndyObjectMapper( true ) );

        final ImpliedRepoConfig config = new ImpliedRepoConfig();
        config.setEnabled( true );
        config.addEnabledGroupNamePattern( ".*" );

        File rootDir = temp.newFolder( "indy.root" );
        final DataFileManager dataFiles = new DataFileManager( rootDir, new DataFileEventManager() );

        validator = new ArtifactStoreValidator( fixture.getTransferManager() );

        final IndyObjectMapper mapper = new IndyObjectMapper( Collections.emptySet() );

        ScriptEngine engine = new ScriptEngine( dataFiles );
        detector = new TestImpliedRepositoryDetector( fixture.getPomReader(), storeManager, metadataManager, validator,
                                                      engine, config, mapper );

        summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" );

        RemoteRepository remote = new RemoteRepository( "test", "http://www.foo.com/repo" );
        Group group = new Group( "group", remote.getKey() );

        storeManager.storeArtifactStore( remote, summary, false, true, new EventMetadata() );
        storeManager.storeArtifactStore( group, summary, false, true, new EventMetadata() );

        server.expect( "HEAD", server.formatUrl( "/repo/" ), 200, "" );
    }

    private RemoteRepository getRemote()
            throws IndyDataException
    {
        return storeManager.query().packageType( MAVEN_PKG_KEY ).getRemoteRepository( REMOTE_NAME );
    }

    private Group getGroup()
            throws IndyDataException
    {
        return storeManager.query().packageType( MAVEN_PKG_KEY ).getGroup( GROUP_NAME );
    }

    //    @Test
    //    public void idWithSpaceInIt_ConvertToDashes()
    //    {
    //        String in = "my id";
    //        String out = detector.formatId( in );
    //        assertThat( out, equalTo( "i-my-id" ) );
    //    }
    //
    //    @Test
    //    public void idWithPlusInIt_ConvertToDashes()
    //    {
    //        String in = "my+id";
    //        String out = detector.formatId( in );
    //        assertThat( out, equalTo( "i-my-id" ) );
    //    }

    @Test
    public void addRepositoryFromPomStorageEvent()
            throws Exception
    {
        Transfer txfr = writeTransfer( "one-repo.pom" );

        final FileStorageEvent event = new FileStorageEvent( TransferOperation.DOWNLOAD, txfr, new EventMetadata() );
        detector.detectRepos( event );
        synchronized ( detector )
        {
            detector.wait();
        }

        assertThat( storeManager.query().packageType( MAVEN_PKG_KEY ).getRemoteRepository( "i-repo-one" ), notNullValue() );

        assertThat( getGroup().getConstituents().contains( new StoreKey( MAVEN_PKG_KEY, StoreType.remote, "i-repo-one" ) ),
                    equalTo( true ) );
    }

    @Test
    public void addImpliedPluginRepositoryToNewGroup()
            throws Exception
    {
        Transfer txfr = writeTransfer( "one-plugin-repo.pom" );

        final FileStorageEvent event = new FileStorageEvent( TransferOperation.DOWNLOAD, txfr, new EventMetadata() );

        detector.detectRepos( event );

        synchronized ( detector )
        {
            detector.wait();
        }

        assertThat( storeManager.query().packageType( MAVEN_PKG_KEY ).getRemoteRepository( "i-repo-one" ), notNullValue() );

        assertThat( getGroup().getConstituents().contains( new StoreKey( StoreType.remote, "i-repo-one" ) ),
                    equalTo( true ) );
    }

    private Transfer writeTransfer( final String resource )
            throws IndyDataException, IOException
    {
        final String path = "/path/to/1/to-1.pom";
        final Transfer txfr =
                fixture.getCache().getTransfer( new ConcreteResource( new RepositoryLocation( getRemote() ), path ) );

        try(OutputStream out = txfr.openOutputStream( TransferOperation.UPLOAD, false );
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream( resource ))
        {
            String pom = IOUtils.toString( in );
            pom = StringUtils.replace( pom, "${baseurl}", server.formatUrl() );

            IOUtils.copy( new StringReader( pom ), out );
        }

        return txfr;
    }

}
