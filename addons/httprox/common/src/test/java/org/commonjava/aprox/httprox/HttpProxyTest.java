package org.commonjava.aprox.httprox;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import groovy.text.GStringTemplateEngine;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.commonjava.aprox.boot.BootOptions;
import org.commonjava.aprox.boot.PortFinder;
import org.commonjava.aprox.content.ContentGenerator;
import org.commonjava.aprox.content.ContentManager;
import org.commonjava.aprox.content.DownloadManager;
import org.commonjava.aprox.core.content.DefaultContentManager;
import org.commonjava.aprox.core.content.DefaultDownloadManager;
import org.commonjava.aprox.core.ctl.ContentController;
import org.commonjava.aprox.httprox.conf.HttproxConfig;
import org.commonjava.aprox.mem.data.MemoryStoreDataManager;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.aprox.subsys.datafile.DataFileManager;
import org.commonjava.aprox.subsys.datafile.change.DataFileEventManager;
import org.commonjava.aprox.subsys.template.TemplatingEngine;
import org.commonjava.aprox.test.fixture.core.TestHttpServer;
import org.commonjava.aprox.util.MimeTyper;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.auth.MemoryPasswordManager;
import org.commonjava.maven.galley.maven.parse.PomPeek;
import org.commonjava.maven.galley.spi.transport.TransportManager;
import org.commonjava.maven.galley.testing.core.CoreFixture;
import org.commonjava.maven.galley.transport.TransportManagerImpl;
import org.commonjava.maven.galley.transport.htcli.HttpClientTransport;
import org.commonjava.maven.galley.transport.htcli.HttpImpl;
import org.commonjava.maven.galley.transport.htcli.util.HttpUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpProxyTest
{

    private static final String USER = "user";

    private static final String PASS = "password";

    private static final String HOST = "127.0.0.1";

    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    @Rule
    public TestHttpServer server = new TestHttpServer();

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Rule
    public CoreFixture core = new CoreFixture( temp );

    private int proxyPort;

    private HttpProxy proxy;

    private MemoryStoreDataManager storeManager;

    @Before
    public void setup()
        throws Exception
    {
        proxyPort = PortFinder.findOpenPort( 16 );

        final TransportManager transports =
            new TransportManagerImpl( new HttpClientTransport( new HttpImpl( new MemoryPasswordManager() ) ) );
        core.setTransports( transports );

        core.initMissingComponents();

        final HttproxConfig config = new HttproxConfig();
        config.setEnabled( true );
        config.setPort( proxyPort );

        final BootOptions bootOpts = new BootOptions();
        bootOpts.setBind( HOST );

        storeManager = new MemoryStoreDataManager( true );

        final AproxObjectMapper mapper = new AproxObjectMapper( true );

        final DownloadManager downloadManager =
            new DefaultDownloadManager( storeManager, core.getTransfers(), core.getLocations() );
        final ContentManager contentManager =
            new DefaultContentManager( storeManager, downloadManager, mapper, Collections.<ContentGenerator> emptySet() );

        final TemplatingEngine templates =
            new TemplatingEngine( new GStringTemplateEngine(), new DataFileManager( temp.newFolder(),
                                                                                    new DataFileEventManager() ) );
        final ContentController contentController =
            new ContentController( storeManager, contentManager, templates, mapper, new MimeTyper() );

        proxy = new HttpProxy( config, bootOpts, storeManager, contentController, core.getCache() );
        proxy.start();
    }

    public void teardown()
    {
        if ( proxy != null )
        {
            proxy.stop();
        }
    }

    @Test
    public void proxySimplePomAndAutoCreateRemoteRepo()
        throws Exception
    {
        final String testRepo = "test";
        final PomRef pom = loadPom( "simple.pom", Collections.<String, String> emptyMap() );
        final String url = server.formatUrl( testRepo, pom.path );
        server.expect( url, 200, pom.pom );

        final HttpGet get = new HttpGet( url );
        final CloseableHttpClient client = proxiedHttp();
        CloseableHttpResponse response = null;

        InputStream stream = null;
        try
        {
            response = client.execute( get, proxyContext( USER, PASS ) );
            stream = response.getEntity()
                             .getContent();
            final String resultingPom = IOUtils.toString( stream );

            assertThat( resultingPom, notNullValue() );
            assertThat( resultingPom, equalTo( pom.pom ) );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
            HttpUtil.cleanupResources( client, get, response );
        }

        final RemoteRepository remoteRepo =
            (RemoteRepository) storeManager.getArtifactStore( new StoreKey( StoreType.remote, "httprox_127-0-0-1" ) );

        assertThat( remoteRepo, notNullValue() );
        assertThat( remoteRepo.getUrl(), equalTo( server.getBaseUri() ) );
    }

    @Test
    public void proxy404()
        throws Exception
    {
        final String testRepo = "test";
        final PomRef pom = loadPom( "simple.pom", Collections.<String, String> emptyMap() );
        final String url = server.formatUrl( testRepo, pom.path );

        final HttpGet get = new HttpGet( url );
        final CloseableHttpClient client = proxiedHttp();
        CloseableHttpResponse response = null;

        final InputStream stream = null;
        try
        {
            response = client.execute( get, proxyContext( USER, PASS ) );
            assertThat( response.getStatusLine()
                                .getStatusCode(), equalTo( HttpStatus.SC_NOT_FOUND ) );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
            HttpUtil.cleanupResources( client, get, response );
        }

        final RemoteRepository remoteRepo =
            (RemoteRepository) storeManager.getArtifactStore( new StoreKey( StoreType.remote, "httprox_127-0-0-1" ) );

        assertThat( remoteRepo, notNullValue() );
        assertThat( remoteRepo.getUrl(), equalTo( server.getBaseUri() ) );
    }

    @Test
    public void proxy500As502()
        throws Exception
    {
        final String testRepo = "test";
        final PomRef pom = loadPom( "simple.pom", Collections.<String, String> emptyMap() );
        final String url = server.formatUrl( testRepo, pom.path );
        server.registerException( new File( "/" + testRepo, pom.path ).getPath(), "Expected exception", 500 );

        final HttpGet get = new HttpGet( url );
        final CloseableHttpClient client = proxiedHttp();
        CloseableHttpResponse response = null;

        final InputStream stream = null;
        try
        {
            response = client.execute( get, proxyContext( USER, PASS ) );
            assertThat( response.getStatusLine()
                                .getStatusCode(), equalTo( HttpStatus.SC_BAD_GATEWAY ) );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
            HttpUtil.cleanupResources( client, get, response );
        }

        final RemoteRepository remoteRepo =
            (RemoteRepository) storeManager.getArtifactStore( new StoreKey( StoreType.remote, "httprox_127-0-0-1" ) );

        assertThat( remoteRepo, notNullValue() );
        assertThat( remoteRepo.getUrl(), equalTo( server.getBaseUri() ) );
    }

    protected HttpClientContext proxyContext( final String user, final String pass )
    {
        final CredentialsProvider creds = new BasicCredentialsProvider();
        creds.setCredentials( new AuthScope( HOST, proxyPort ), new UsernamePasswordCredentials( user, pass ) );
        final HttpClientContext ctx = HttpClientContext.create();
        ctx.setCredentialsProvider( creds );

        return ctx;
    }

    protected CloseableHttpClient proxiedHttp()
        throws Exception
    {
        final HttpRoutePlanner planner = new DefaultProxyRoutePlanner( new HttpHost( HOST, proxyPort ) );
        return HttpClients.custom()
                          .setRoutePlanner( planner )
                          .build();
    }

    protected PomRef loadPom( final String name, final Map<String, String> substitutions )
    {
        try
        {
            final String resource = name.endsWith( ".pom" ) ? name : name + ".pom";
            logger.info( "Loading POM: {}", resource );

            final InputStream stream = Thread.currentThread()
                                             .getContextClassLoader()
                                             .getResourceAsStream( resource );

            String pom = IOUtils.toString( stream );
            IOUtils.closeQuietly( stream );

            for ( final Map.Entry<String, String> entry : substitutions.entrySet() )
            {
                pom = pom.replace( "@" + entry.getKey() + "@", entry.getValue() );
            }

            final PomPeek peek = new PomPeek( pom, false );
            final ProjectVersionRef gav = peek.getKey();

            final String path =
                String.format( "%s/%s/%s/%s-%s.pom", gav.getGroupId()
                                                        .replace( '.', '/' ), gav.getArtifactId(),
                               gav.getVersionString(), gav.getArtifactId(), gav.getVersionString() );

            return new PomRef( pom, path );
        }
        catch ( final Exception e )
        {
            e.printStackTrace();
            fail( "Failed to read POM from: " + name );
        }

        return null;
    }

    protected static final class PomRef
    {
        PomRef( final String pom, final String path )
        {
            this.pom = pom;
            this.path = path;
        }

        protected final String pom;

        protected final String path;
    }

}
