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
package org.commonjava.indy.koji.content.testutil;

import org.apache.commons.io.IOUtils;
import org.commonjava.test.http.expect.ExpectationHandler;
import org.commonjava.test.http.expect.ExpectationServer;
import org.custommonkey.xmlunit.XMLUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.fail;

/**
 * Created by jdcasey on 11/3/16.
 */
public final class KojiMockHandlers
{
    private KojiMockHandlers()
    {
    }

    public static void configureKojiServer( ExpectationServer server, String urlBase, AtomicInteger exchangeCounter,
                                            String resourceBase, boolean verifyArtifacts, String verifyBasepath )
    {
        try
        {
            if ( verifyArtifacts )
            {
                Properties checksums = new Properties();
                String checksumsProperties = resourceBase + "/checksums.properties";
                try(InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream( checksumsProperties ) )
                {
                    if ( in == null )
                    {
                        fail( "Cannot find checksums resource in classpath: '" + checksumsProperties + "'" );
                    }

                    checksums.load( in );
                }

                for ( String path: checksums.stringPropertyNames() )
                {
                    server.expect("GET", server.formatUrl( verifyBasepath, path ), 200, checksums.getProperty( path ) );
                }
            }

            server.expect( "POST", server.formatUrl( urlBase ), kojiMessageHandler( exchangeCounter, resourceBase ) );
            server.expect( "POST", server.formatUrl( urlBase, "ssllogin" ),
                           kojiMessageHandler( exchangeCounter, resourceBase ) );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            fail( "Failed to serve xml-rpc request. " + e.getMessage() );
        }
    }

    private static ExpectationHandler kojiMessageHandler( AtomicInteger exchangeCounter, String resourceBase )
    {
        return ( request, response ) -> {
            int idx = exchangeCounter.getAndIncrement();

            String requestPath = Paths.get( resourceBase, String.format( "%02d-request.xml", idx ) ).toString();
            String responsePath = Paths.get( resourceBase, String.format( "%02d-response.xml", idx ) ).toString();

            Logger logger = LoggerFactory.getLogger( KojiMockHandlers.class );
            logger.debug( "Verifying vs request XML resource: {}\nSending response XML resource: {}\nRequest index: {}",
                          requestPath, responsePath, idx );

            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream( requestPath );
            if ( in == null )
            {
                fail( "Cannot find request XML for comparison: " + requestPath );
            }

            String expectedXml = IOUtils.toString( in );
            String requestXml = IOUtils.toString( request.getInputStream() );

            logger.debug( "Comparing request XML:\n\n{}\n\nTo expected XML:\n\n{}\n\n", requestXml, expectedXml );

            try
            {
                XMLUnit.setIgnoreWhitespace( true );
                XMLUnit.setIgnoreDiffBetweenTextAndCDATA( true );
                XMLUnit.setIgnoreAttributeOrder( true );
                XMLUnit.setIgnoreComments( true );

                assertXMLEqual( "Requested XML not equal to expected XML from: " + requestPath, requestXml,
                                expectedXml );
            }
            catch ( SAXException e )
            {
                e.printStackTrace();
                fail( "Cannot find parse either requested XML or expected XML from: " + requestPath );
            }

            in = Thread.currentThread().getContextClassLoader().getResourceAsStream( responsePath );
            if ( in == null )
            {
                fail( "Cannot find response XML: " + responsePath );
            }

            response.setStatus( 200 );
            OutputStream out = response.getOutputStream();
            IOUtils.copy( in, out );
        };
    }
}
