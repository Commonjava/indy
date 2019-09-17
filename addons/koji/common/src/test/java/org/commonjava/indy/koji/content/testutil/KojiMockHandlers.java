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
package org.commonjava.indy.koji.content.testutil;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.commonjava.test.http.expect.ExpectationHandler;
import org.commonjava.test.http.expect.ExpectationServer;
import org.custommonkey.xmlunit.XMLUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.commonjava.indy.koji.content.testutil.MockScript.MOCK_SCRIPT_JSON;
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
        try( InputStream scriptIn = Thread.currentThread().getContextClassLoader().getResourceAsStream( Paths.get(resourceBase, MOCK_SCRIPT_JSON).toString() ) )
        {
            if ( scriptIn == null )
            {
                fail( "Cannot find script description file: " + MOCK_SCRIPT_JSON + " in: " + resourceBase );
            }

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

            ObjectMapper mapper = new ObjectMapper();

            MockScript mockScript = mapper.readValue( scriptIn, MockScript.class );
            mockScript.setCounter( exchangeCounter );
            server.expect( "POST", server.formatUrl( urlBase ), kojiMessageHandler( mockScript, resourceBase ) );
            server.expect( "POST", server.formatUrl( urlBase, "ssllogin" ),
                           kojiMessageHandler( mockScript, resourceBase ) );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            fail( "Failed to serve xml-rpc request. " + e.getMessage() );
        }
    }

    private static ExpectationHandler kojiMessageHandler( MockScript mockScript, String resourceBase )
    {
        return ( request, response ) -> {
            String nextBase = mockScript.getNextScriptBaseName();
            if ( nextBase == null )
            {
                fail( "Cannot retrieve next base-name in mock script: "
                              + mockScript.getHumanReadableScriptAttemptCount() + "/" + mockScript.getScriptCount() + " from: " + resourceBase );
            }

            String requestPath = Paths.get( resourceBase, String.format( "%s-request.xml", nextBase ) ).toString();
            String responsePath = Paths.get( resourceBase, String.format( "%s-response.xml", nextBase ) ).toString();

            Logger logger = LoggerFactory.getLogger( KojiMockHandlers.class );
            logger.debug( "Verifying vs request XML resource: {}\nSending response XML resource: {}\nRequest index: {}",
                          requestPath, responsePath, mockScript.getHumanReadableScriptAttemptCount() );

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

                assertXMLEqual( "Requested XML not equal to expected XML from: " + requestPath, expectedXml,
                                requestXml);
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
