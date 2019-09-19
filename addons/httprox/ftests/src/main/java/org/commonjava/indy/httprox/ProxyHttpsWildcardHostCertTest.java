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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.indy.client.core.helper.HttpResources;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.dto.StoreListingDTO;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_PKG_KEY;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ProxyHttpsWildcardHostCertTest
                extends AbstractHttproxFunctionalTest
{

    private static final String USER = "user";

    private static final String PASS = "password";

    String https_url = "https://github.com/Medium/phantomjs/releases/download/v2.1.1/phantomjs-2.1.1-linux-x86_64.tar.bz2"; // 23M

    /**
     * To verify below error is gone.
     *
     * Caused by: javax.net.ssl.SSLPeerUnverifiedException:
     * Host name 'github-production-release-asset-2e65be.s3.amazonaws.com' does not match the certificate subject
     * provided by the peer (CN=*.s3.amazonaws.com, O=Amazon.com Inc., L=Seattle, ST=Washington, C=US)
     */
    @Ignore
    @Test
    public void run() throws Exception
    {
        String ret = head( https_url, true, USER, PASS );
        //System.out.println( ">>>> " + ret );
        // HttpResponseProxy{HTTP/1.1 200 Ok [Content-Length: 23415665, Last-Modified: Mon, 22 May 2017 00:09:50 GMT,
        // Content-Type: application/octet-stream]}
        assertTrue( ret.contains( "200 Ok" ) );
    }

    protected String head( String url, boolean withCACert, String user, String pass ) throws Exception
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

        HttpHead req = new HttpHead( url );
        CloseableHttpResponse response = null;

        InputStream stream = null;
        try
        {
            response = client.execute( req, proxyContext( user, pass ) );
            /*stream = response.getEntity().getContent();
            final String resulting = IOUtils.toString( stream );

            assertThat( resulting, notNullValue() );
            System.out.println( "\n\n>>>>>>>\n\n" + resulting + "\n\n" );*/

            return response.toString();
        }
        finally
        {
            IOUtils.closeQuietly( stream );
            HttpResources.cleanupResources( req, response, client );
        }
    }

    private KeyStore getTrustStore( File jks ) throws Exception
    {
        KeyStore trustStore = KeyStore.getInstance( KeyStore.getDefaultType() );
        try (FileInputStream instream = new FileInputStream( jks ))
        {
            trustStore.load( instream, "passwd".toCharArray() );
        }
        return trustStore;
    }

    @Override
    protected int getTestTimeoutMultiplier()
    {
        return 1;
    }

    @Override
    protected String getAdditionalHttproxConfig()
    {
        return "MITM.enabled=true\nMITM.ca.key=${indy.home}/etc/indy/ssl/ca.der\n"
                        + "MITM.ca.cert=${indy.home}/etc/indy/ssl/ca.crt\n"
                        + "MITM.dn.template=CN=<host>, O=Test Org";
    }

    @Override
    protected void initTestData( CoreServerFixture fixture ) throws IOException
    {
        copyToConfigFile( "ssl/ca.der", "ssl/ca.der" );
        copyToConfigFile( "ssl/ca.crt", "ssl/ca.crt" );
        copyToConfigFile( "ssl/ca.jks", "ssl/ca.jks" );
    }
}
