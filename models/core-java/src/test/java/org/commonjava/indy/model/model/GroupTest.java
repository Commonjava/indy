package org.commonjava.indy.model.model;

import org.commonjava.indy.model.core.GenericPackageTypeDescriptor;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.PathStyle;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.stream.Stream;

import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_PKG_KEY;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Created by jdcasey on 5/11/17.
 */
public class GroupTest
{
    @Test
    public void copyFidelity()
    {
        Group src =
                new Group( GENERIC_PKG_KEY, "test" );

        src.setMetadata( "key", "value" );
        src.setDescription( "some description" );
        src.setDisableTimeout( 500 );
        src.setDisabled( true );
        src.setTransientMetadata( "transient", "someval" );
        src.setPathStyle( PathStyle.hashed );
        src.addConstituent( new StoreKey( GENERIC_PKG_KEY, remote, "foo" ) );

        Group target = src.copyOf();

        Stream.of( Group.class.getMethods() )
              .filter( m -> m.getName().startsWith( "get" ) && m.isAccessible() && m.getParameterCount() == 0 )
              .forEach( m ->
                        {
                            try
                            {
                                assertThat( m.getName() + " didn't get copied correctly!", m.invoke( target ),
                                            equalTo( m.invoke( src ) ) );
                            }
                            catch ( IllegalAccessException e )
                            {
                                e.printStackTrace();
                                fail( "Failed to invoke: " + m.getName() );
                            }
                            catch ( InvocationTargetException e )
                            {
                                e.printStackTrace();
                            }
                        } );
    }
}
