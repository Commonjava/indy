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
package org.commonjava.indy.httprox;

import org.apache.commons.io.IOUtils;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.indy.client.core.helper.HttpResources;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.dto.StoreListingDTO;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;

import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_PKG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Add this test to make sure MITM can download some specific tgz files.
 *
 * NOTE: yarn install always report:
 * error https://registry.npmjs.org/fsevents/-/fsevents-1.2.4.tgz:
 * 140079105800320:error:1408F119:SSL routines:SSL3_GET_RECORD:decryption failed or bad record mac:s3_pkt.c:535:
 */
public class ProxyHttpsDownloadTgzTest
                extends ProxyHttpsTest
{

    private static final String USER = "user";

    private static final String PASS = "password";

    String https_url = "https://registry.npmjs.org/fsevents/-/fsevents-1.2.4.tgz";

    @Test
    public void run() throws Exception
    {
        File ret = getDownloadedFile( https_url, true, USER, PASS );
        assertTrue( ret != null && ret.exists() );
        //System.out.println( "File size >>> " + ret.length() );
        assertEquals( ret.length(), 784846 ); // content-length: 784846

        StoreListingDTO<RemoteRepository> repo =
                        this.client.stores().getRemoteByUrl( "https://registry.npmjs.org:443/", GENERIC_PKG_KEY );
        repo.getItems().forEach( repository -> System.out.println(">>> " + repository) );
        assertTrue( repo.getItems().size() == 1 );
    }

    protected File getDownloadedFile( String url, boolean withCACert, String user, String pass ) throws Exception
    {
        CloseableHttpClient client;

        if ( withCACert )
        {
            File jks = new File( etcDir, "ssl/ca.jks" );
            KeyStore trustStore = getTrustStore( jks );
            SSLSocketFactory socketFactory = new SSLSocketFactory( trustStore );
            client = proxiedHttp( user, pass, socketFactory );
        }
        else
        {
            client = proxiedHttp( user, pass );
        }

        HttpGet get = new HttpGet( url );
        CloseableHttpResponse response = null;

        InputStream stream = null;
        try
        {
            response = client.execute( get, proxyContext( user, pass ) );
            StatusLine status = response.getStatusLine();
            System.out.println( "status >>>> " + status );

            if ( status.getStatusCode() == 404 )
            {
                return null;
            }

            stream = response.getEntity().getContent();
            File file = getTemp().newFile();
            FileOutputStream fileOutputStream = new FileOutputStream( file );
            IOUtils.copy( stream, fileOutputStream );
            fileOutputStream.close();

            return file;
        }
        finally
        {
            IOUtils.closeQuietly( stream );
            HttpResources.cleanupResources( get, response, client );
        }
    }

}
