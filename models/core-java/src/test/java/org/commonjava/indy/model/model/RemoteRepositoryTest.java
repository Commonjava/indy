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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.commonjava.indy.model.core.GenericPackageTypeDescriptor;
import org.commonjava.indy.model.core.PathStyle;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Created by jdcasey on 2/15/16.
 */
public class RemoteRepositoryTest
{
    @Test
    public void serializeRemoteWithServerPem()
            throws JsonProcessingException
    {
        RemoteRepository remote = new RemoteRepository( "test", "http://test.com/repo" );
        remote.setServerCertPem( "AAAAFFFASDFADSFASDFSADFa" );
        remote.setServerTrustPolicy( "self-signed" );

        String json = new IndyObjectMapper( true ).writeValueAsString( remote );

        System.out.println( json );
    }

    @Test
    public void serializeRemoteWithKeyPemAndPassword()
            throws JsonProcessingException
    {
        RemoteRepository remote = new RemoteRepository( "test", "http://test.com/repo" );
        remote.setKeyCertPem( "AAAAFFFASDFADSFASDFSADFa" );
        remote.setKeyPassword( "testme" );

        String json = new IndyObjectMapper( true ).writeValueAsString( remote );

        System.out.println( json );
    }

    @Test
    public void copyFidelity()
    {
        RemoteRepository src =
                new RemoteRepository( GenericPackageTypeDescriptor.GENERIC_PKG_KEY, "test", "http://test.com/repo" );

        src.setTimeoutSeconds( 100 );
        src.setPassword( "foo" );
        src.setUser( "bar" );
        src.setMetadata( "key", "value" );
        src.setCacheTimeoutSeconds( 200 );
        src.setKeyCertPem( "THISISACERTIFICATEPEM" );
        src.setKeyPassword( "certpass" );
        src.setMetadataTimeoutSeconds( 300 );
        src.setNfcTimeoutSeconds( 400 );
        src.setPassthrough( false );
        src.setProxyHost( "127.0.0.1" );
        src.setProxyPort( 8888 );
        src.setProxyUser( "proxyuser" );
        src.setProxyPassword( "proxypass" );
        src.setServerCertPem( "ANOTHERCERTIFICATEPEM" );
        src.setServerTrustPolicy( "all" );
        src.setAllowReleases( false );
        src.setAllowSnapshots( false );
        src.setDescription( "some description" );
        src.setDisableTimeout( 500 );
        src.setDisabled( true );
        src.setPathMaskPatterns( Collections.singleton( "some/path" ) );
        src.setTransientMetadata( "transient", "someval" );
        src.setPathStyle( PathStyle.hashed );

        RemoteRepository target = src.copyOf();

        Stream.of( RemoteRepository.class.getMethods() )
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
