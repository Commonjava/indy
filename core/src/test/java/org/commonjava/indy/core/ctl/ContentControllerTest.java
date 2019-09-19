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
package org.commonjava.indy.core.ctl;

import groovy.text.GStringTemplateEngine;
import org.apache.commons.io.IOUtils;
import org.commonjava.cdi.util.weft.PoolWeftExecutorService;
import org.commonjava.cdi.util.weft.WeftExecutorService;
import org.commonjava.indy.content.ContentGenerator;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.core.content.ContentGeneratorManager;
import org.commonjava.indy.core.content.DefaultContentDigester;
import org.commonjava.indy.core.content.DefaultContentManager;
import org.commonjava.indy.core.content.DefaultDirectContentAccess;
import org.commonjava.indy.core.content.DefaultDownloadManager;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.indy.subsys.datafile.change.DataFileEventManager;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.template.TemplatingEngine;
import org.commonjava.indy.util.MimeTyper;
import org.commonjava.maven.galley.io.SpecialPathManagerImpl;
import org.commonjava.maven.galley.io.checksum.TransferMetadata;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.SimpleLocation;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.nfc.MemoryNotFoundCache;
import org.commonjava.maven.galley.testing.core.CoreFixture;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ContentControllerTest
{

    @Rule
    public CoreFixture fixture = new CoreFixture();

    private ContentController content;

    private static DefaultCacheManager cacheManager;

    private static Cache<String, TransferMetadata> contentMetadata;

    @BeforeClass
    public static void setupClass()
    {
        GlobalConfiguration globalConfiguration =
                new GlobalConfigurationBuilder().globalJmxStatistics().allowDuplicateDomains( true ).build();
        cacheManager =
                new DefaultCacheManager( globalConfiguration, new ConfigurationBuilder().simpleCache( true ).build() );

        contentMetadata = cacheManager.getCache( "content-metadata", true );

    }

    @Before
    public void setup()
            throws Exception
    {
        contentMetadata.clear();

        fixture.initMissingComponents();

        final StoreDataManager storeManager = new MemoryStoreDataManager( true );

        WeftExecutorService rescanService =
                        new PoolWeftExecutorService( "test-rescan-executor", (ThreadPoolExecutor) Executors.newCachedThreadPool(), 2, 10f, false, null, null );

        final DownloadManager fileManager =
                new DefaultDownloadManager( storeManager, fixture.getTransferManager(), fixture.getLocationExpander(), rescanService );

        WeftExecutorService contentAccessService =
                        new PoolWeftExecutorService( "test-content-access-executor", (ThreadPoolExecutor) Executors.newCachedThreadPool(), 2, 10f, false,null, null );
        final DirectContentAccess dca =
                new DefaultDirectContentAccess( fileManager, contentAccessService );

        final ContentManager contentManager =
                new DefaultContentManager( storeManager, fileManager, new IndyObjectMapper( true ),
                                           new SpecialPathManagerImpl(), new MemoryNotFoundCache(),
                                           new DefaultContentDigester( dca, new CacheHandle<String, TransferMetadata>(
                                                   "content-metadata", contentMetadata ) ),
                                           new ContentGeneratorManager() );

        final TemplatingEngine templates = new TemplatingEngine( new GStringTemplateEngine(), new DataFileManager(
                fixture.getTemp().newFolder( "indy-home" ), new DataFileEventManager() ) );

        content = new ContentController( storeManager, contentManager, templates, new IndyObjectMapper( true ),
                                         new MimeTyper() );
    }

    @Test
    public void detectHtml_HtmlDoctypeDeclaration()
            throws Exception
    {
        final ConcreteResource res = new ConcreteResource( new SimpleLocation( "test:uri" ), "file.html" );
        final Transfer tx = fixture.getCache().getTransfer( res );

        PrintWriter writer = null;
        try
        {
            writer = new PrintWriter( new OutputStreamWriter( tx.openOutputStream( TransferOperation.GENERATE ) ) );
            writer.print( "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n"
                                  + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" );

            writer.flush();
        }
        finally
        {
            IOUtils.closeQuietly( writer );
        }

        assertThat( content.isHtmlContent( tx ), equalTo( true ) );
    }

    @Test
    public void detectHtml_SingleHtmlElementLine()
            throws Exception
    {
        final ConcreteResource res = new ConcreteResource( new SimpleLocation( "test:uri" ), "file.html" );
        final Transfer tx = fixture.getCache().getTransfer( res );

        PrintWriter writer = null;
        try
        {
            writer = new PrintWriter( new OutputStreamWriter( tx.openOutputStream( TransferOperation.GENERATE ) ) );
            writer.print( "<html>" );

            writer.flush();
        }
        finally
        {
            IOUtils.closeQuietly( writer );
        }

        assertThat( content.isHtmlContent( tx ), equalTo( true ) );
    }

    @Test
    public void detectHtml_SingleHtmlElementBeginsOnLine()
            throws Exception
    {
        final ConcreteResource res = new ConcreteResource( new SimpleLocation( "test:uri" ), "file.html" );
        final Transfer tx = fixture.getCache().getTransfer( res );

        PrintWriter writer = null;
        try
        {
            writer = new PrintWriter( new OutputStreamWriter( tx.openOutputStream( TransferOperation.GENERATE ) ) );
            writer.print( "<html ng-app=\"foo\"" );

            writer.flush();
        }
        finally
        {
            IOUtils.closeQuietly( writer );
        }

        assertThat( content.isHtmlContent( tx ), equalTo( true ) );
    }

    @Test
    public void detectHtml_SingleHtmlElementLineWithPrecedingWhitespace()
            throws Exception
    {
        final ConcreteResource res = new ConcreteResource( new SimpleLocation( "test:uri" ), "file.html" );
        final Transfer tx = fixture.getCache().getTransfer( res );

        PrintWriter writer = null;
        try
        {
            writer = new PrintWriter( new OutputStreamWriter( tx.openOutputStream( TransferOperation.GENERATE ) ) );
            writer.print( "    <html>" );

            writer.flush();
        }
        finally
        {
            IOUtils.closeQuietly( writer );
        }

        assertThat( content.isHtmlContent( tx ), equalTo( true ) );
    }

    @Test
    public void detectHtml_MultipleHtmlElementsOnALine()
            throws Exception
    {
        final ConcreteResource res = new ConcreteResource( new SimpleLocation( "test:uri" ), "file.html" );
        final Transfer tx = fixture.getCache().getTransfer( res );

        PrintWriter writer = null;
        try
        {
            writer = new PrintWriter( new OutputStreamWriter( tx.openOutputStream( TransferOperation.GENERATE ) ) );
            writer.print( "<html><body><h1>FOO</h1>" );

            writer.flush();
        }
        finally
        {
            IOUtils.closeQuietly( writer );
        }

        assertThat( content.isHtmlContent( tx ), equalTo( true ) );
    }

}
