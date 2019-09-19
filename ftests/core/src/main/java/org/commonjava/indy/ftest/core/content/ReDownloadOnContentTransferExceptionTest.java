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
package org.commonjava.indy.ftest.core.content;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

/**
 * If indy download file from remote repo is not correct (lose some byte), Indy will
 * delete the target file and throw TransferContentException.
 *
 * Then client can download it again immediately (NFC will not prevent).
 *
 * <b>GIVEN:</b>
 * <ul>
 *     <li>A remote repo</li>
 *     <li>remote file: maven-metadata.xml</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>first download the file, the case will break the outPutStream and throw Exception and delete the target file(incorrect file)</li>
 *     <li>second download the file, the case can return correct file </li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>When first download the file, Indy client responds with null and Indy does not store partial download.</li>
 *     <li>When second download the file, Indy stores the full file and returns content to the user.</li>
 * </ul>
 */
public class ReDownloadOnContentTransferExceptionTest
                extends AbstractContentManagementTest
{
    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    private final static String responseContent =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<metadata>\n" + "  <groupId>org.foo</groupId>\n"
                                    + "  <artifactId>bar</artifactId>\n" + "  <versioning>\n"
                                    + "    <latest>1.0</latest>\n" + "    <release>1.0</release>\n" + "    <versions>\n"
                                    + "      <version>1.0</version>\n" + "    </versions>\n"
                                    + "    <lastUpdated>20150722164334</lastUpdated>\n" + "  </versioning>\n"
                                    + "</metadata>\n";

    @Test
    public void run() throws Exception
    {
        final String path = "org/foo/bar/maven-metadata.xml";

        final AtomicInteger count = new AtomicInteger( 0 );
        server.expect( "GET", server.formatUrl( STORE, path ), (request, response)->{
            response.setStatus( 200 );
            response.setHeader( "Content-Length", Integer.toString( responseContent.length() ) );

            int idx = count.getAndIncrement();
            if ( idx < 1 )
            {

                try
                {
                    logger.info( "ContenlengthExpectationHandlerExecutor call index =" + idx + " url:"
                                         + request.getRequestURI() );

                    response.getWriter().write( responseContent.substring( 0, responseContent.length() / 2 ) );
                }
                catch ( Throwable t )
                {
                    throw new ServletException( t.getMessage() );
                }

            }
            else
            {
                logger.info( "ContenlengthExpectationHandlerExecutor call index =  " + idx + " url:"
                                     + request.getRequestURI() );

                response.getWriter().write( responseContent );
            }
        } );

        RemoteRepository remote =
                new RemoteRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, STORE, server.formatUrl( STORE ) );

//        remote.setMetadata( Location.CONNECTION_TIMEOUT_SECONDS, Integer.toString( -1 ) );
        client.stores().create( remote, "adding remote", RemoteRepository.class );

        StoreKey sk = new StoreKey( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, StoreType.remote, STORE );

        assertThat( client.content().get( sk, path ), nullValue() );

        String result = IOUtils.toString( client.content().get( sk, path ) );

        logger.info( "runWithMismacthByRemoteRespository ---- result :{}", result );
        assertThat( result, notNullValue() );
    }
}
