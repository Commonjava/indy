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
package org.commonjava.indy.promote.data;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.content.IndyLocationExpander;
import org.commonjava.indy.content.ContentGenerator;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.core.content.DefaultContentManager;
import org.commonjava.indy.core.content.DefaultDownloadManager;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.promote.conf.PromoteConfig;
import org.commonjava.indy.promote.model.PathsPromoteRequest;
import org.commonjava.indy.promote.model.PathsPromoteResult;
import org.commonjava.indy.promote.validate.PromoteValidationsManager;
import org.commonjava.indy.promote.validate.PromotionValidationTools;
import org.commonjava.indy.promote.validate.PromotionValidator;
import org.commonjava.indy.promote.validate.ValidationRuleParser;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.indy.subsys.datafile.change.DataFileEventManager;
import org.commonjava.indy.subsys.template.ScriptEngine;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.maven.rel.MavenModelProcessor;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.testing.maven.GalleyMavenFixture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class PromotionManagerTest
{

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private ContentManager contentManager;

    private DownloadManager downloadManager;

    private StoreDataManager storeManager;

    private GalleyMavenFixture galleyParts;

    private PromotionManager manager;

    private DataFileManager dataManager;

    private PromoteValidationsManager validationsManager;

    private PromotionValidator validator;

    @Before
    public void setup()
            throws Exception
    {
        galleyParts = new GalleyMavenFixture( true, temp );
        galleyParts.initMissingComponents();

        storeManager = new MemoryStoreDataManager( true );

        downloadManager = new DefaultDownloadManager( storeManager, galleyParts.getTransferManager(),
                                                      new IndyLocationExpander( storeManager ) );

        contentManager = new DefaultContentManager( storeManager, downloadManager, new IndyObjectMapper( true ),
                                                    Collections.<ContentGenerator>emptySet() );

        dataManager = new DataFileManager( temp.newFolder( "data" ), new DataFileEventManager() );
        validationsManager = new PromoteValidationsManager( dataManager, new PromoteConfig(),
                                                            new ValidationRuleParser( new ScriptEngine(),
                                                                                      new IndyObjectMapper( true ) ) );

        MavenModelProcessor modelProcessor = new MavenModelProcessor();
        validator = new PromotionValidator( validationsManager,
                                            new PromotionValidationTools( contentManager, storeManager,
                                                                          galleyParts.getPomReader(),
                                                                          galleyParts.getMavenMetadataReader(),
                                                                          modelProcessor,
                                                                          galleyParts.getTypeMapper(),
                                                                          galleyParts.getTransferManager() ) );
        manager = new PromotionManager( validator, contentManager, downloadManager, storeManager );
    }

    @Test
    public void promoteAll_PushTwoArtifactsToHostedRepo_VerifyCopiedToOtherHostedRepo()
            throws Exception
    {
        final HostedRepository source = new HostedRepository( "source" );
        storeManager.storeArtifactStore( source, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" ),
                                         new EventMetadata() );

        final String first = "/first/path";
        final String second = "/second/path";
        contentManager.store( source, first, new ByteArrayInputStream( "This is a test".getBytes() ),
                              TransferOperation.UPLOAD, new EventMetadata() );

        contentManager.store( source, second, new ByteArrayInputStream( "This is a test".getBytes() ),
                              TransferOperation.UPLOAD, new EventMetadata() );

        final HostedRepository target = new HostedRepository( "target" );
        storeManager.storeArtifactStore( target, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" ),
                                         new EventMetadata() );

        final PathsPromoteResult result =
                manager.promotePaths( new PathsPromoteRequest( source.getKey(), target.getKey() ) );

        assertThat( result.getRequest().getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest().getTarget(), equalTo( target.getKey() ) );

        final Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        final Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        Transfer ref = downloadManager.getStorageReference( target, first );
        assertThat( ref.exists(), equalTo( true ) );

        ref = downloadManager.getStorageReference( target, second );
        assertThat( ref.exists(), equalTo( true ) );
    }

    @Test
    public void promoteAll_PushTwoArtifactsToHostedRepo_DryRun_VerifyPendingPathsPopulated()
            throws Exception
    {
        final HostedRepository source = new HostedRepository( "source" );
        storeManager.storeArtifactStore( source, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" ),
                                         new EventMetadata() );

        final String first = "/first/path";
        final String second = "/second/path";
        contentManager.store( source, first, new ByteArrayInputStream( "This is a test".getBytes() ),
                              TransferOperation.UPLOAD, new EventMetadata() );

        contentManager.store( source, second, new ByteArrayInputStream( "This is a test".getBytes() ),
                              TransferOperation.UPLOAD, new EventMetadata() );

        final HostedRepository target = new HostedRepository( "target" );
        storeManager.storeArtifactStore( target, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" ),
                                         new EventMetadata() );

        final PathsPromoteResult result =
                manager.promotePaths( new PathsPromoteRequest( source.getKey(), target.getKey() ).setDryRun( true ) );

        assertThat( result.getRequest().getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest().getTarget(), equalTo( target.getKey() ) );

        final Set<String> completed = result.getCompletedPaths();
        assertThat( completed == null || completed.isEmpty(), equalTo( true ) );

        final Set<String> pending = result.getPendingPaths();
        assertThat( pending, notNullValue() );
        assertThat( pending.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        Transfer ref = downloadManager.getStorageReference( target, first );
        assertThat( ref.exists(), equalTo( false ) );

        ref = downloadManager.getStorageReference( target, second );
        assertThat( ref.exists(), equalTo( false ) );
    }

    @Test
    public void promoteAll_PurgeSource_PushTwoArtifactsToHostedRepo_VerifyCopiedToOtherHostedRepo()
            throws Exception
    {
        final HostedRepository source = new HostedRepository( "source" );
        storeManager.storeArtifactStore( source, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" ),
                                         new EventMetadata() );

        final String first = "/first/path";
        final String second = "/second/path";
        contentManager.store( source, first, new ByteArrayInputStream( "This is a test".getBytes() ),
                              TransferOperation.UPLOAD, new EventMetadata() );

        contentManager.store( source, second, new ByteArrayInputStream( "This is a test".getBytes() ),
                              TransferOperation.UPLOAD, new EventMetadata() );

        final HostedRepository target = new HostedRepository( "target" );
        storeManager.storeArtifactStore( target, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" ),
                                         new EventMetadata() );

        final PathsPromoteResult result = manager.promotePaths(
                new PathsPromoteRequest( source.getKey(), target.getKey() ).setPurgeSource( true ) );

        assertThat( result.getRequest().getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest().getTarget(), equalTo( target.getKey() ) );

        final Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        final Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        Transfer ref = downloadManager.getStorageReference( target, first );
        assertThat( ref.exists(), equalTo( true ) );

        ref = downloadManager.getStorageReference( target, second );
        assertThat( ref.exists(), equalTo( true ) );

        // source artifacts should be deleted.
        ref = downloadManager.getStorageReference( source, first );
        assertThat( ref.exists(), equalTo( false ) );

        ref = downloadManager.getStorageReference( source, second );
        assertThat( ref.exists(), equalTo( false ) );
    }

    @Test
    public void rollback_PushTwoArtifactsToHostedRepo_PromoteSuccessThenRollback()
            throws Exception
    {
        final HostedRepository source = new HostedRepository( "source" );
        storeManager.storeArtifactStore( source, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" ),
                                         new EventMetadata() );

        final String first = "/first/path";
        final String second = "/second/path";
        contentManager.store( source, first, new ByteArrayInputStream( "This is a test".getBytes() ),
                              TransferOperation.UPLOAD, new EventMetadata() );

        contentManager.store( source, second, new ByteArrayInputStream( "This is a test".getBytes() ),
                              TransferOperation.UPLOAD, new EventMetadata() );

        final HostedRepository target = new HostedRepository( "target" );
        storeManager.storeArtifactStore( target, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" ),
                                         new EventMetadata() );

        PathsPromoteResult result = manager.promotePaths( new PathsPromoteRequest( source.getKey(), target.getKey() ) );

        assertThat( result.getRequest().getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest().getTarget(), equalTo( target.getKey() ) );

        Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        result = manager.rollbackPathsPromote( result );

        assertThat( result.getRequest().getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest().getTarget(), equalTo( target.getKey() ) );

        completed = result.getCompletedPaths();
        assertThat( completed == null || completed.isEmpty(), equalTo( true ) );

        pending = result.getPendingPaths();
        assertThat( pending, notNullValue() );
        assertThat( pending.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        Transfer ref = downloadManager.getStorageReference( target, first );
        assertThat( ref.exists(), equalTo( false ) );

        ref = downloadManager.getStorageReference( target, second );
        assertThat( ref.exists(), equalTo( false ) );
    }

    @Test
    public void rollback_PurgeSource_PushTwoArtifactsToHostedRepo_PromoteSuccessThenRollback_VerifyContentInSource()
            throws Exception
    {
        final HostedRepository source = new HostedRepository( "source" );
        storeManager.storeArtifactStore( source, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" ),
                                         new EventMetadata() );

        final String first = "/first/path";
        final String second = "/second/path";
        contentManager.store( source, first, new ByteArrayInputStream( "This is a test".getBytes() ),
                              TransferOperation.UPLOAD, new EventMetadata() );

        contentManager.store( source, second, new ByteArrayInputStream( "This is a test".getBytes() ),
                              TransferOperation.UPLOAD, new EventMetadata() );

        final HostedRepository target = new HostedRepository( "target" );
        storeManager.storeArtifactStore( target, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" ),
                                         new EventMetadata() );

        PathsPromoteResult result = manager.promotePaths(
                new PathsPromoteRequest( source.getKey(), target.getKey() ).setPurgeSource( true ) );

        assertThat( result.getRequest().getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest().getTarget(), equalTo( target.getKey() ) );

        Set<String> pending = result.getPendingPaths();
        assertThat( pending == null || pending.isEmpty(), equalTo( true ) );

        Set<String> completed = result.getCompletedPaths();
        assertThat( completed, notNullValue() );
        assertThat( completed.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        result = manager.rollbackPathsPromote( result );

        assertThat( result.getRequest().getSource(), equalTo( source.getKey() ) );
        assertThat( result.getRequest().getTarget(), equalTo( target.getKey() ) );

        completed = result.getCompletedPaths();
        assertThat( completed == null || completed.isEmpty(), equalTo( true ) );

        pending = result.getPendingPaths();
        assertThat( pending, notNullValue() );
        assertThat( pending.size(), equalTo( 2 ) );

        assertThat( result.getError(), nullValue() );

        Transfer ref = downloadManager.getStorageReference( target, first );
        assertThat( ref.exists(), equalTo( false ) );

        ref = downloadManager.getStorageReference( target, second );
        assertThat( ref.exists(), equalTo( false ) );

        ref = downloadManager.getStorageReference( source, first );
        assertThat( ref.exists(), equalTo( true ) );

        ref = downloadManager.getStorageReference( source, second );
        assertThat( ref.exists(), equalTo( true ) );
    }
}
