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
package org.commonjava.aprox.core.ctl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import groovy.text.GStringTemplateEngine;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.content.ContentGenerator;
import org.commonjava.aprox.content.ContentManager;
import org.commonjava.aprox.content.DownloadManager;
import org.commonjava.aprox.core.content.DefaultContentManager;
import org.commonjava.aprox.core.content.DefaultDownloadManager;
import org.commonjava.aprox.core.data.DefaultStoreEventDispatcher;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.mem.data.MemoryStoreDataManager;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.aprox.subsys.datafile.DataFileManager;
import org.commonjava.aprox.subsys.datafile.change.DataFileEventManager;
import org.commonjava.aprox.subsys.template.TemplatingEngine;
import org.commonjava.aprox.util.MimeTyper;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.SimpleLocation;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.testing.core.CoreFixture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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
            new DefaultContentManager( storeManager, fileManager, new AproxObjectMapper( true ),
                                       Collections.<ContentGenerator> emptySet() );

        final TemplatingEngine templates =
            new TemplatingEngine( new GStringTemplateEngine(), new DataFileManager( fixture.getTemp()
                                                                                           .newFolder( "aprox-home" ),
                                                                                    new DataFileEventManager() ) );

        content =
            new ContentController( storeManager, contentManager, templates, new AproxObjectMapper( true ),
                                   new MimeTyper() );
    }

    @Test
    public void detectHtml_HtmlDoctypeDeclaration()
        throws Exception
    {
        final ConcreteResource res = new ConcreteResource( new SimpleLocation( "test:uri" ), "file.html" );
        final Transfer tx = fixture.getCache()
                                   .getTransfer( res );

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
        final Transfer tx = fixture.getCache()
                                   .getTransfer( res );

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
        final Transfer tx = fixture.getCache()
                                   .getTransfer( res );

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
        final Transfer tx = fixture.getCache()
                                   .getTransfer( res );

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
        final Transfer tx = fixture.getCache()
                                   .getTransfer( res );

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
