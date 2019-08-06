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
package org.commonjava.indy.pkg.npm.content;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.pkg.npm.model.DistTag;
import org.commonjava.indy.pkg.npm.model.PackageMetadata;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * This case tests if remote package.json will timeout correctly
 * when: <br />
 * <ul>
 *      <li>remote repo A contains package.json for a given package</li>
 *      <li>package.json is retrieved via remote repo A by client</li>
 *      <li>package.json metadata timeout is scheduled</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>package.json expires with metadata timeout and is removed from local storage in remote repository A</li>
 * </ul>
 */
public class NPMMetadataTimeoutTest
                extends AbstractContentManagementTest
{

    private static final String REPO = "A";

    private static final String PATH = "jquery";

    @Test
    public void test() throws Exception
    {
        IndyObjectMapper mapper = new IndyObjectMapper( true );

        final PackageMetadata src = new PackageMetadata();
        final DistTag dts = new DistTag();
        dts.setBeta( "1" );
        src.setDistTags( dts );

        server.expect( "GET", server.formatUrl( REPO, PATH ), (req, resp)->{
            resp.setStatus( 200 );
            mapper.writeValue( resp.getWriter(), src );
            resp.getWriter().flush();
        } );

        //FIXME: After using galley-0.16.8, I'm not sure the second retrieval of npm metadata will get path of
        //       "A/jquery/package.json" while the first retrieval is "A/jquery". So I add a new expectation here
        //       to let the second retrieval can work. Need further checking.
//        server.expect( "GET", server.formatUrl( REPO, PATH+"/package.json" ), (req, resp)->{
//            resp.setStatus( 200 );
//            mapper.writeValue( resp.getWriter(), src );
//            resp.getWriter().flush();
//        } );

        final RemoteRepository repo = new RemoteRepository( NPM_PKG_KEY, REPO, server.formatUrl( REPO ) );
        repo.setMetadataTimeoutSeconds( 1 );

        client.stores().create( repo, "adding npm remote repo", RemoteRepository.class );

        // First retrieval
        verifyMetadataBetaTag( "1", repo );

        // wait for repo metadata timeout
        Thread.sleep( 2000 );

        assertThat( "Metadata not cleaned up!", client.content().exists( repo.getKey(), PATH + "/package.json", true ), equalTo( false ) );

        logger.info( "\n\n\n\nRE-REQUEST STARTS HERE\n\n\n\n" );

        // Second retrieval
        dts.setBeta( "2" );
        verifyMetadataBetaTag( "2", repo );
    }

    private void verifyMetadataBetaTag( final String betaTag, RemoteRepository repo )
            throws IndyClientException, IOException
    {
        try(InputStream remote = client.content().get( repo.getKey(), PATH ))
        {
            assertThat( remote, notNullValue() );

            PackageMetadata merged =
                    new IndyObjectMapper( true ).readValue( IOUtils.toString( remote ), PackageMetadata.class );

            assertThat( merged.getDistTags().getBeta(), equalTo( betaTag ) );
        }
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
