/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Test;

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
}
