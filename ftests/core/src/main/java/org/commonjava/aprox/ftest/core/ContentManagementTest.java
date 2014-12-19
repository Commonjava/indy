package org.commonjava.aprox.ftest.core;

import static org.commonjava.aprox.model.core.StoreType.group;
import static org.commonjava.aprox.model.core.StoreType.hosted;
import static org.commonjava.aprox.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.commonjava.aprox.client.core.helper.PathInfo;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.junit.Before;
import org.junit.Test;

public class ContentManagementTest
    extends AbstractAproxFunctionalTest
{

    private static final String STORE = "test";

    private static final String CENTRAL = "central";

    private static final String PUBLIC = "public";

    @Before
    public void before()
        throws Exception
    {
        final HostedRepository hosted = this.client.stores()
                   .create( new HostedRepository( STORE ), HostedRepository.class );

        RemoteRepository central = null;
        if ( !client.stores()
                    .exists( remote, CENTRAL ) )
        {
            central =
                client.stores()
                  .create( new RemoteRepository( CENTRAL, "http://repo.maven.apache.org/maven2/" ),
                           RemoteRepository.class );
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
                      .create( new Group( PUBLIC ), Group.class );
        }

        g.setConstituents( Arrays.asList( hosted.getKey(), central.getKey() ) );
        client.stores()
              .update( g );
    }

    @Test
    public void storeFileAndVerifyReturnedInfo()
        throws Exception
    {
        final InputStream stream = new ByteArrayInputStream( ( "This is a test: " + System.nanoTime() ).getBytes() );

        final String path = "/path/to/foo.class";
        final PathInfo result = client.content()
                                      .store( hosted, STORE, path, stream );

        System.out.println( result );
        assertThat( result, notNullValue() );
        assertThat( result.exists(), equalTo( true ) );
    }

    @Test
    public void storeFileAndVerifyExistence()
        throws Exception
    {
        final InputStream stream = new ByteArrayInputStream( ( "This is a test: " + System.nanoTime() ).getBytes() );

        final String path = "/path/to/foo.class";

        assertThat( client.content()
                          .exists( hosted, STORE, path ), equalTo( false ) );

        client.content()
              .store( hosted, STORE, path, stream );

        assertThat( client.content()
                          .exists( hosted, STORE, path ), equalTo( true ) );
    }

    @Test
    public void storeFileInConstituentAndVerifyExistenceInGroup()
        throws Exception
    {
        final Group g = client.stores()
                              .load( group, PUBLIC, Group.class );

        System.out.printf( "\n\nGroup constituents are:\n  %s\n\n", StringUtils.join( g.getConstituents(), "\n  " ) );

        assertThat( g.getConstituents()
                     .contains( new StoreKey( hosted, STORE ) ), equalTo( true ) );

        final InputStream stream = new ByteArrayInputStream( ( "This is a test: " + System.nanoTime() ).getBytes() );

        final String path = "/path/to/foo.class";

        assertThat( client.content()
                          .exists( hosted, STORE, path ), equalTo( false ) );

        client.content()
              .store( hosted, STORE, path, stream );

        assertThat( client.content()
                          .exists( group, PUBLIC, path ), equalTo( true ) );
    }

    @Test
    public void storeAndRetrieveFile()
        throws Exception
    {
        final byte[] bytes = ( "This is a test: " + System.nanoTime() ).getBytes();
        final InputStream stream = new ByteArrayInputStream( bytes );

        final String path = "/path/to/foo.class";
        client.content()
              .store( hosted, STORE, path, stream );

        final InputStream result = client.content()
                                         .get( hosted, STORE, path );
        final byte[] resultBytes = IOUtils.toByteArray( result );

        assertThat( Arrays.equals( bytes, resultBytes ), equalTo( true ) );
    }

    @Test
    public void downloadFileFromRemoteRepository()
        throws Exception
    {
        final InputStream result = client.content()
                                         .get( remote, CENTRAL, "org/commonjava/commonjava/2/commonjava-2.pom" );
        assertThat( result, notNullValue() );

        final String pom = IOUtils.toString( result );
        assertThat( pom.contains( "<groupId>org.commonjava</groupId>" ), equalTo( true ) );
    }

}
