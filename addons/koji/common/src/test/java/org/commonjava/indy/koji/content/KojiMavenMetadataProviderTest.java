package org.commonjava.indy.koji.content;

import com.redhat.red.build.koji.KojiClient;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.commonjava.indy.koji.conf.IndyKojiConfig;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.rwx.binding.error.BindException;
import org.commonjava.test.http.expect.ExpectationServer;
import org.commonjava.util.jhttpc.auth.MemoryPasswordManager;
import org.hamcrest.CoreMatchers;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.commonjava.indy.koji.content.testutil.KojiMockHandlers.configureKojiServer;
import static org.commonjava.indy.model.core.StoreType.group;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 11/3/16.
 */
public class KojiMavenMetadataProviderTest
{
    private static final String KOJI_BASEPATH = "koji";

    private AtomicInteger counter = new AtomicInteger( 0 );

    @Rule
    public ExpectationServer server = new ExpectationServer();

    private CacheHandle<ProjectRef, Metadata> cache;

    private IndyKojiConfig kojiConfig;

    private KojiClient kojiClient;

    private DefaultCacheManager cacheMgr;

    private KojiMavenMetadataProvider provider;

    @Test
    public void retrieveVersionMetadata()
            throws Exception
    {
        initKojiClient( "simple-metadata-generate" );

        Metadata metadata =
                provider.getMetadata( new StoreKey( group, "public" ), "commons-io/commons-io/maven-metadata.xml" );

        assertThat( metadata, notNullValue() );

        StringWriter sw = new StringWriter();
        new MetadataXpp3Writer().write( sw, metadata );
        System.out.println( sw.toString() );

        Versioning versioning = metadata.getVersioning();
        assertThat( versioning, notNullValue() );

        assertThat( versioning.getLatest(), equalTo( "2.4.0.redhat-1" ) );
        assertThat( versioning.getRelease(), equalTo( "2.4.0.redhat-1" ) );

        List<String> versions = versioning.getVersions();
        assertThat( versions, notNullValue() );
        assertThat( versions.size(), equalTo( 2 ) );

        int idx = 0;
        assertThat( versions.get( idx ), equalTo( "2.1-redhat-1" ) );
        idx++;
        assertThat( versions.get( idx ), equalTo( "2.4.0.redhat-1" ) );

    }

    @Test
    public void metadataNullWhenNoVersionsFound()
            throws Exception
    {
        initKojiClient( "no-metadata-generate" );

        Metadata metadata =
                provider.getMetadata( new StoreKey( group, "public" ), "commons-io/commons-io/maven-metadata.xml" );

        assertThat( metadata, nullValue() );
    }

    @Test
    public void allowVersionMetadataToExpire()
            throws Exception
    {
        initKojiClient( "simple-metadata-generate" );

        StoreKey sk = new StoreKey( group, "public" );
        String path = "commons-io/commons-io/maven-metadata.xml";

        Metadata metadata = provider.getMetadata( sk, path );

        assertThat( metadata, notNullValue() );

        StringWriter sw = new StringWriter();
        new MetadataXpp3Writer().write( sw, metadata );
        System.out.println( sw.toString() );

        Versioning versioning = metadata.getVersioning();
        assertThat( versioning, notNullValue() );

        assertThat( versioning.getLatest(), equalTo( "2.4.0.redhat-1" ) );
        assertThat( versioning.getRelease(), equalTo( "2.4.0.redhat-1" ) );

        List<String> versions = versioning.getVersions();
        assertThat( versions, notNullValue() );
        assertThat( versions.size(), equalTo( 2 ) );

        int idx = 0;
        assertThat( versions.get( idx ), equalTo( "2.1-redhat-1" ) );
        idx++;
        assertThat( versions.get( idx ), equalTo( "2.4.0.redhat-1" ) );

        String originalLastUpdated = versioning.getLastUpdated();

        Thread.sleep( 4000 );

        // reset to just after getAPIVersion, since this is only called when the client initializes.
        counter.set( 1 );

        metadata = provider.getMetadata( sk, path );

        assertThat( metadata, notNullValue() );

        sw = new StringWriter();
        new MetadataXpp3Writer().write( sw, metadata );
        System.out.println( sw.toString() );

        assertThat( metadata.getVersioning(), notNullValue() );

        System.out.printf( "\n\nOriginal lastUpdated: '%s'\nNew lastUpdated: '%s'\n\n", originalLastUpdated,
                           metadata.getVersioning().getLastUpdated() );
        assertThat( metadata.getVersioning().getLastUpdated(),
                    CoreMatchers.not( CoreMatchers.equalTo( originalLastUpdated ) ) );
    }

    private void initKojiClient( String exchangeName )
            throws BindException
    {
        configureKojiServer( server, KOJI_BASEPATH, counter, "koji-metadata/" + exchangeName );
        kojiClient = new KojiClient( kojiConfig, new MemoryPasswordManager(), Executors.newCachedThreadPool() );
        provider = new KojiMavenMetadataProvider( cache, kojiClient, kojiConfig );
    }

    @Before
    public void setup()
            throws Throwable
    {
        cacheMgr = new DefaultCacheManager();
        String mdCacheName = "koji-maven-metadata";

        cache = new CacheHandle( mdCacheName, cacheMgr.getCache( mdCacheName, true ) );

        kojiConfig = new IndyKojiConfig();
        kojiConfig.setEnabled( true );
        kojiConfig.setLockTimeoutSeconds( 2 );
        kojiConfig.setMaxConnections( 2 );
        kojiConfig.setMetadataTimeoutSeconds( 2 );
        kojiConfig.setRequestTimeoutSeconds( 1 );
        kojiConfig.setStorageRootUrl( server.formatUrl( "kojiroot" ) );
        kojiConfig.setUrl( server.formatUrl( "koji" ) );

        kojiConfig.setTagPatterns( Collections.singletonList( "jb-.+" ) );
        kojiConfig.setTargetGroups( Collections.singletonMap( "public", "public" ) );
    }

    @After
    public void shutdown()
    {
        if ( kojiClient != null )
        {
            kojiClient.close();
        }

        if ( cacheMgr != null )
        {
            cacheMgr.stop();
        }
    }
}
