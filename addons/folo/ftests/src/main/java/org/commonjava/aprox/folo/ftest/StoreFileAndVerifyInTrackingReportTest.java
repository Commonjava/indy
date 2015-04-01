package org.commonjava.aprox.folo.ftest;

import static org.commonjava.aprox.model.core.StoreType.group;
import static org.commonjava.aprox.model.core.StoreType.hosted;
import static org.commonjava.aprox.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.client.core.util.UrlUtils;
import org.commonjava.aprox.folo.client.AproxFoloAdminClientModule;
import org.commonjava.aprox.folo.client.AproxFoloContentClientModule;
import org.commonjava.aprox.folo.dto.TrackedContentDTO;
import org.commonjava.aprox.folo.dto.TrackedContentEntryDTO;
import org.commonjava.aprox.ftest.core.AbstractAproxFunctionalTest;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.junit.Before;
import org.junit.Test;

public class StoreFileAndVerifyInTrackingReportTest
    extends AbstractAproxFunctionalTest
{

    private static final String STORE = "test";

    private static final String CENTRAL = "central";

    private static final String PUBLIC = "public";

    @Before
    public void before()
        throws Exception
    {
        final String changelog = "Setup " + name.getMethodName();
        final HostedRepository hosted =
            this.client.stores()
                       .create( new HostedRepository( STORE ), changelog, HostedRepository.class );

        RemoteRepository central = null;
        if ( !client.stores()
                    .exists( remote, CENTRAL ) )
        {
            central =
                client.stores()
                      .create( new RemoteRepository( CENTRAL, "http://repo.maven.apache.org/maven2/" ), changelog,
                               RemoteRepository.class );
        }
        else
        {
            central = client.stores()
                            .load( remote, CENTRAL, RemoteRepository.class );
        }

        Group g;
        if ( client.stores()
                   .exists( group, PUBLIC ) )
        {
            g = client.stores()
                      .load( group, PUBLIC, Group.class );
        }
        else
        {
            g = client.stores()
                      .create( new Group( PUBLIC ), changelog, Group.class );
        }

        g.setConstituents( Arrays.asList( hosted.getKey(), central.getKey() ) );
        client.stores()
              .update( g, changelog );
    }

    @Test
    public void storeFileAndVerifyInReport()
        throws Exception
    {
        final String trackingId = newName();

        final InputStream stream = new ByteArrayInputStream( ( "This is a test: " + System.nanoTime() ).getBytes() );

        final String path = "/path/to/foo.class";
        client.module( AproxFoloContentClientModule.class )
              .store( trackingId, hosted, STORE, path, stream );

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final InputStream in = client.module( AproxFoloContentClientModule.class )
                                     .get( trackingId, hosted, STORE, path );

        IOUtils.copy( in, baos );
        in.close();

        final byte[] bytes = baos.toByteArray();

        final String md5 = md5Hex( bytes );
        final String sha256 = sha256Hex( bytes );

        assertThat( md5, equalTo( DigestUtils.md5Hex( bytes ) ) );
        assertThat( sha256, equalTo( DigestUtils.sha256Hex( bytes ) ) );

        final TrackedContentDTO report = client.module( AproxFoloAdminClientModule.class )
                                               .getTrackingReport( trackingId, hosted, STORE );
        assertThat( report, notNullValue() );

        final Set<TrackedContentEntryDTO> uploads = report.getUploads();

        assertThat( uploads, notNullValue() );
        assertThat( uploads.size(), equalTo( 1 ) );

        final TrackedContentEntryDTO entry = uploads.iterator()
                                                    .next();

        System.out.println( entry );

        assertThat( entry, notNullValue() );
        assertThat( entry.getStoreKey(), equalTo( new StoreKey( hosted, STORE ) ) );
        assertThat( entry.getPath(), equalTo( path ) );
        assertThat( entry.getLocalUrl(),
                    equalTo( UrlUtils.buildUrl( client.getBaseUrl(), hosted.singularEndpointName(), STORE, path ) ) );
        assertThat( entry.getOriginUrl(), nullValue() );
        assertThat( entry.getMd5(), equalTo( md5 ) );
        assertThat( entry.getSha256(), equalTo( sha256 ) );
    }

    private String sha256Hex( final byte[] bytes )
        throws Exception
    {
        System.out.println( "sha256" );
        return digest( bytes, MessageDigest.getInstance( "SHA-256" ) );
    }

    private String md5Hex( final byte[] bytes )
        throws Exception
    {
        System.out.println( "md5" );
        return digest( bytes, MessageDigest.getInstance( "MD5" ) );
    }

    private String digest( final byte[] bytes, final MessageDigest md )
        throws Exception
    {
        final StringBuilder sb = new StringBuilder();
        for ( final byte b : md.digest( bytes ) )
        {
            final String hex = Integer.toHexString( b & 0xff );
            if ( hex.length() < 2 )
            {
                sb.append( '0' );
            }
            sb.append( hex );
        }

        return sb.toString();
    }

    @Override
    protected Collection<AproxClientModule> getAdditionalClientModules()
    {
        return Arrays.asList( new AproxFoloAdminClientModule(), new AproxFoloContentClientModule() );
    }

}
