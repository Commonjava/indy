package org.commonjava.indy.model.model;

import org.commonjava.indy.model.core.GenericPackageTypeDescriptor;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;

import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_PKG_KEY;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 5/12/17.
 */
public class StoreKeyTest
{

    @Test( expected = IllegalArgumentException.class )
    public void constructWithInvalidPackageType()
    {
        new StoreKey( "invalid", remote, "stuff" );
    }

    @Test
    public void parseDeprecated()
    {
        StoreKey key = StoreKey.fromString( "remote:central" );

        assertThat( key.getPackageType(), equalTo( MAVEN_PKG_KEY ) );
        assertThat( key.getType(), equalTo( remote ) );
        assertThat( key.getName(), equalTo( "central" ) );
    }

    @Test
    public void parseWithValidPackageType()
    {
        StoreKey key = StoreKey.fromString( "maven:remote:central" );

        assertThat( key.getPackageType(), equalTo( MAVEN_PKG_KEY ) );
        assertThat( key.getType(), equalTo( remote ) );
        assertThat( key.getName(), equalTo( "central" ) );

        key = StoreKey.fromString( "generic-http:remote:httprox_stuff" );

        assertThat( key.getPackageType(), equalTo( GENERIC_PKG_KEY ) );
        assertThat( key.getType(), equalTo( remote ) );
        assertThat( key.getName(), equalTo( "httprox_stuff" ) );
    }

    @Test( expected = IllegalArgumentException.class )
    public void parseWithInvalidPackageType()
    {
        System.out.println( StoreKey.fromString( "invalid:remote:stuff" ) );
    }

    @Test( expected = IllegalArgumentException.class )
    public void parseWithInvalidStoreType()
    {
        System.out.println( StoreKey.fromString( "maven:invalid:stuff" ) );
    }
}
