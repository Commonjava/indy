package org.commonjava.indy.model.core;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;

import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class HostedRepositorySerializationTest
{
    @Test
    public void simpleRoundTrip()
            throws IOException, ClassNotFoundException
    {
        HostedRepository in = new HostedRepository( PKG_TYPE_MAVEN, "test" );
        HostedRepository out = doRoundTrip( in );

        compareRepos( in, out );
    }

    @Test
    public void fullRoundTrip()
            throws IOException, ClassNotFoundException
    {
        HostedRepository in = new HostedRepository( PKG_TYPE_MAVEN, "test" );

        in.setReadonly( true );
        in.setAuthoritativeIndex( true );
        in.setSnapshotTimeoutSeconds( 100 );
        in.setStorage( "/path/to/alt/storage" );
        in.setMetadata( "foo", "bar" );
        in.setDisabled( true );
        in.setRescanInProgress( true );
        in.setDescription( "This is a description" );
        in.setPathMaskPatterns( Collections.singleton( "somepath" ) );
        in.setPathStyle( PathStyle.hashed );
        in.setAllowReleases( false );
        in.setAllowSnapshots( true );
        in.setDisableTimeout( -1 );
        in.setTransientMetadata( "transient", "value" );

        HostedRepository out = doRoundTrip( in );

        compareRepos( in, out );
    }

    private void compareRepos( final HostedRepository in, final HostedRepository out )
    {
        assertThat( out.getStorage(), equalTo( in.getStorage() ) );
        assertThat( out.getSnapshotTimeoutSeconds(), equalTo( in.getSnapshotTimeoutSeconds() ) );
        assertThat( out.isReadonly(), equalTo( in.isReadonly() ) );

        assertThat( out.isAllowReleases(), equalTo( in.isAllowReleases() ) );
        assertThat( out.isAllowSnapshots(), equalTo( in.isAllowSnapshots() ) );

        assertThat( out.getKey(), equalTo( in.getKey() ) );
        assertThat( out.getDescription(), equalTo( in.getDescription() ) );
        assertThat( out.getMetadata(), equalTo( in.getMetadata() ) );
        assertThat( out.isDisabled(), equalTo( in.isDisabled() ) );
        assertThat( out.getDisableTimeout(), equalTo( in.getDisableTimeout() ) );
        assertThat( out.getPathStyle(), equalTo( in.getPathStyle() ) );
        assertThat( out.getPathMaskPatterns(), equalTo( in.getPathMaskPatterns() ) );
        assertThat( out.isAuthoritativeIndex(), equalTo( in.isAuthoritativeIndex() ) );
        assertThat( out.getCreateTime(), equalTo( in.getCreateTime() ) );
        assertThat( out.isRescanInProgress(), equalTo( in.isRescanInProgress() ) );

        if ( out.getTransientMetadata() != null && !out.getTransientMetadata().isEmpty())
        {
            fail( "Transient metadata should be empty from deserialized object. Was: " + out.getTransientMetadata() );
        }
    }

    private HostedRepository doRoundTrip( final HostedRepository in )
            throws IOException, ClassNotFoundException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( baos );
        oos.writeObject( in );
//        in.writeExternal( oos );

        oos.flush();
        ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( baos.toByteArray() ) );

//        HostedRepository out = new HostedRepository();
//        out.readExternal( ois );
//        return out;
        return (HostedRepository) ois.readObject();
    }
}
