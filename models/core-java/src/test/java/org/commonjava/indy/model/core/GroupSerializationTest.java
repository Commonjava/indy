package org.commonjava.indy.model.core;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class GroupSerializationTest
{
    @Test
    public void simpleRoundTrip()
            throws IOException, ClassNotFoundException
    {
        Group in = new Group( PKG_TYPE_MAVEN, "test", Arrays.asList( new StoreKey( PKG_TYPE_MAVEN, remote, "test1" ),
                                                                     new StoreKey( PKG_TYPE_MAVEN, remote,
                                                                                   "test2" ) ) );
        Group out = doRoundTrip( in );

        compareRepos( in, out );
    }

    @Test
    public void fullRoundTrip()
            throws IOException, ClassNotFoundException
    {
        Group in = new Group( PKG_TYPE_MAVEN, "test", Arrays.asList( new StoreKey( PKG_TYPE_MAVEN, remote, "test1" ),
                                                                     null,
                                                                     new StoreKey( PKG_TYPE_MAVEN, hosted, "test2" ),
                                                                     new StoreKey( PKG_TYPE_MAVEN, group, "test3" ) ) );

        in.setDescription( "Test description" );
        in.setPrependConstituent( true );
        in.setMetadata( "Test key 1", "foo" );
        in.setDisabled( true );
        in.setRescanInProgress( true );
        in.setPathMaskPatterns( Collections.singleton( "foo" ) );
        in.setPathStyle( PathStyle.hashed );
        in.setAuthoritativeIndex( true );
        in.setDisableTimeout( -1 );
        in.setTransientMetadata( "transient key", "value" );

        Group out = doRoundTrip( in );

        compareRepos( in, out );
    }

    private void compareRepos( final Group in, final Group out )
    {
//        long inCount = in.getConstituents().stream().filter( Objects::nonNull ).count();
//        List<StoreKey> inNonNull =
//                in.getConstituents().stream().filter( Objects::nonNull ).collect( Collectors.toList() );
//
//        long outCount = out.getConstituents().stream().filter( Objects::nonNull ).count();
//        assertThat( "Groups do not contain the same number of non-null constituent references", outCount, equalTo( inCount ) );
//
//        for(int i=0; i<inCount; i++)
//        {
//            assertThat( "Group constituents at index: " + i + " do not match.", out.getConstituents().get( i ),
//                        equalTo( inNonNull.get( i ) ) );
//        }

        assertThat( out.getConstituents(), equalTo( in.getConstituents() ) );
        assertThat( out.isPrependConstituent(), equalTo( in.isPrependConstituent() ) );

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

    private Group doRoundTrip( final Group in )
            throws IOException, ClassNotFoundException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( baos );
        oos.writeObject( in );
//        in.writeExternal( oos );

        oos.flush();
        ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( baos.toByteArray() ) );

//        Group out = new Group();
//        out.readExternal( ois );
//        return out;

        return (Group) ois.readObject();
    }
}
