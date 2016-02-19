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
