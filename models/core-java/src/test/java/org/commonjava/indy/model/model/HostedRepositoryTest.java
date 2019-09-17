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

import org.commonjava.indy.model.core.GenericPackageTypeDescriptor;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.PathStyle;
import org.commonjava.indy.model.core.RemoteRepository;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Created by jdcasey on 5/11/17.
 */
public class HostedRepositoryTest
{

    @Test
    public void copyFidelity()
    {
        HostedRepository src =
                new HostedRepository( GenericPackageTypeDescriptor.GENERIC_PKG_KEY, "test" );

        src.setMetadata( "key", "value" );
        src.setAllowReleases( false );
        src.setAllowSnapshots( false );
        src.setDescription( "some description" );
        src.setDisableTimeout( 500 );
        src.setDisabled( true );
        src.setPathMaskPatterns( Collections.singleton( "some/path" ) );
        src.setTransientMetadata( "transient", "someval" );
        src.setSnapshotTimeoutSeconds( 600 );
        src.setStorage( "/path/to/storage" );
        src.setPathStyle( PathStyle.hashed );

        HostedRepository target = src.copyOf();

        Stream.of( HostedRepository.class.getMethods() )
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
