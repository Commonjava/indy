/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.model.model;

import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.PathStyle;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_PKG_KEY;
import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
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

    @Test
    public void testPrepend(){
        Group src =
                new Group( MAVEN_PKG_KEY, "test" );
        StoreKey remote1 = new StoreKey( MAVEN_PKG_KEY, remote, "remote1" );
        src.addConstituent( remote1 );
        assertThat(src.getConstituents().get( 0 ), equalTo(  remote1 ));

        StoreKey remote2 = new StoreKey( MAVEN_PKG_KEY, remote, "remote2" );
        src.addConstituent( remote2 );
        assertThat(src.getConstituents().get( 0 ), equalTo(  remote1 ));

        src.setPrependConstituent( true );

        StoreKey hosted1 = new StoreKey( MAVEN_PKG_KEY, hosted, "hosted1" );
        src.addConstituent( hosted1 );
        assertThat(src.getConstituents().get( 0 ), equalTo(  hosted1 ));

        StoreKey remote3 = new StoreKey( MAVEN_PKG_KEY, remote, "remote3" );
        src.addConstituent( remote3 );
        assertThat(src.getConstituents().get( 0 ), equalTo(  remote3 ));

        StoreKey group = new StoreKey( MAVEN_PKG_KEY, StoreType.group, "group" );
        src.addConstituent( group );
        assertThat(src.getConstituents().get( 0 ), equalTo(  group ));
    }
}
