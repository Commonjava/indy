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
package org.commonjava.indy.core.ctl;

import groovy.text.GStringTemplateEngine;
import org.apache.commons.io.IOUtils;
import org.commonjava.indy.content.ContentDigester;
import org.commonjava.indy.content.ContentGenerator;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.core.content.DefaultContentManager;
import org.commonjava.indy.core.content.DefaultDownloadManager;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.indy.subsys.datafile.change.DataFileEventManager;
import org.commonjava.indy.subsys.template.TemplatingEngine;
import org.commonjava.indy.util.MimeTyper;
import org.commonjava.maven.galley.io.SpecialPathManagerImpl;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.SimpleLocation;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.nfc.MemoryNotFoundCache;
import org.commonjava.maven.galley.testing.core.CoreFixture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ContentControllerTest
{

    @Rule
    public CoreFixture fixture = new CoreFixture();

    private ContentController content;

    @Before
    public void setup()
            throws Exception
    {
        fixture.initMissingComponents();

        final StoreDataManager storeManager = new MemoryStoreDataManager( true );
        final DownloadManager fileManager =
                new DefaultDownloadManager( storeManager, fixture.getTransferManager(), fixture.getLocationExpander() );

        final ContentManager contentManager =
                new DefaultContentManager( storeManager, fileManager, new IndyObjectMapper( true ),
                                           new SpecialPathManagerImpl(), new MemoryNotFoundCache(),
                                           new ContentDigester( fileManager ),
                                           Collections.<ContentGenerator>emptySet() );

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
