package org.commonjava.aprox.core.ctl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import groovy.text.GStringTemplateEngine;

import java.util.Collections;

import org.apache.commons.io.FileUtils;
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
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.SimpleLocation;
import org.commonjava.maven.galley.model.Transfer;
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
    {
        fixture.initMissingComponents();

        final StoreDataManager storeManager = new MemoryStoreDataManager( new DefaultStoreEventDispatcher() );
        final DownloadManager fileManager =
            new DefaultDownloadManager( storeManager, fixture.getTransfers(), fixture.getLocations() );

        final ContentManager contentManager =
            new DefaultContentManager( storeManager, fileManager, Collections.<ContentGenerator> emptySet() );

        final TemplatingEngine templates =
            new TemplatingEngine( new GStringTemplateEngine(), new DataFileManager( fixture.getTemp()
                                                                                           .newFolder( "aprox-home" ),
                                                                                    new DataFileEventManager() ) );

        content = new ContentController( storeManager, contentManager, templates, new AproxObjectMapper( true ) );
    }

    @Test
    public void detectHtml_HtmlDoctypeDeclaration()
        throws Exception
    {
        final ConcreteResource res = new ConcreteResource( new SimpleLocation( "test:uri" ), "file.html" );
        final Transfer tx = fixture.getCache()
                                   .getTransfer( res );

        FileUtils.write( tx.getDetachedFile(), "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n"
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" );

        assertThat( content.isHtmlContent( tx ), equalTo( true ) );
    }

    @Test
    public void detectHtml_SingleHtmlElementLine()
        throws Exception
    {
        final ConcreteResource res = new ConcreteResource( new SimpleLocation( "test:uri" ), "file.html" );
        final Transfer tx = fixture.getCache()
                                   .getTransfer( res );

        FileUtils.write( tx.getDetachedFile(), "<html>" );

        assertThat( content.isHtmlContent( tx ), equalTo( true ) );
    }

    @Test
    public void detectHtml_SingleHtmlElementBeginsOnLine()
        throws Exception
    {
        final ConcreteResource res = new ConcreteResource( new SimpleLocation( "test:uri" ), "file.html" );
        final Transfer tx = fixture.getCache()
                                   .getTransfer( res );

        FileUtils.write( tx.getDetachedFile(), "<html ng-app=\"foo\"" );

        assertThat( content.isHtmlContent( tx ), equalTo( true ) );
    }

    @Test
    public void detectHtml_SingleHtmlElementLineWithPrecedingWhitespace()
        throws Exception
    {
        final ConcreteResource res = new ConcreteResource( new SimpleLocation( "test:uri" ), "file.html" );
        final Transfer tx = fixture.getCache()
                                   .getTransfer( res );

        FileUtils.write( tx.getDetachedFile(), "    <html>" );

        assertThat( content.isHtmlContent( tx ), equalTo( true ) );
    }

    @Test
    public void detectHtml_MultipleHtmlElementsOnALine()
        throws Exception
    {
        final ConcreteResource res = new ConcreteResource( new SimpleLocation( "test:uri" ), "file.html" );
        final Transfer tx = fixture.getCache()
                                   .getTransfer( res );

        FileUtils.write( tx.getDetachedFile(), "<html><body><h1>FOO</h1>" );

        assertThat( content.isHtmlContent( tx ), equalTo( true ) );
    }

}
