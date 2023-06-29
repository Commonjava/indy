/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
import org.commonjava.indy.ftest.core.category.TimingDependent;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.pkg.npm.model.DistTag;
import org.commonjava.indy.pkg.npm.model.PackageMetadata;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This case tests if remote package.json will time out by using ONLY storage level timeout.
 * We need to disable scheduler in order to test it.
 * Given: <br />
 * <ul>
 *      <li>remote repo A contains package.json for a given package</li>
 *      <li>remote repo A metadata timeout is 1s</li>
 * </ul>
 * When: <br />
 * <ul>
 *      <li>the first time package.json is retrieved via remote repo A by client</li>
 *      <li>expect the remote server with new version in package.json</li>
 *      <li>wait for 3s for the cached file to timeout</li>
 *      <li>the second time package.json is retrieved via remote repo A</li>
 * </ul>
 * Then: <br />
 * <ul>
 *     <li>the version 1 of package.json expires in remote repository A, the version 2 is retrieved successfully</li>
 * </ul>
 */
public class NPMMetadataTimeoutNoSchedulerTest
        extends AbstractContentManagementTest
{

    private static final String REPO = "A";

    private static final String PATH_JQUERY = "jquery";

    private static final String PATH_BABEL_PARSER = "@babel/parser";

    // !!!IMPORTANT: disable scheduler in order to test storage level timeout
    @Override
    protected boolean isSchedulerEnabled()
    {
        return false;
    }

    @Test
    @Category( TimingDependent.class )
    public void test()
            throws Exception
    {
        IndyObjectMapper mapper = new IndyObjectMapper( true );

        final PackageMetadata src = new PackageMetadata();
        final DistTag dts = new DistTag();
        dts.setBeta( "1" );
        src.setDistTags( dts );

        server.expect( "GET", server.formatUrl( REPO, PATH_JQUERY ), ( req, resp ) -> {
            resp.setStatus( 200 );
            mapper.writeValue( resp.getWriter(), src );
            resp.getWriter().flush();
        } );

        server.expect( "GET", server.formatUrl( REPO, PATH_BABEL_PARSER ), ( req, resp ) -> {
            resp.setStatus( 200 );
            mapper.writeValue( resp.getWriter(), src );
            resp.getWriter().flush();
        } );

        final RemoteRepository repo = new RemoteRepository( NPM_PKG_KEY, REPO, server.formatUrl( REPO ) );
        repo.setMetadataTimeoutSeconds( 1 );

        client.stores().create( repo, "adding npm remote repo", RemoteRepository.class );

        // First retrieval
        verifyMetadataBetaTag( "1", PATH_JQUERY, repo );
        assertThat( "Metadata not retrieved!", client.content().exists( repo.getKey(), PATH_JQUERY, true ),
                    equalTo( true ) );
        assertThat( "Metadata not retrieved!",
                    client.content().exists( repo.getKey(), PATH_JQUERY + "/package.json", true ), equalTo( true ) );

        verifyMetadataBetaTag( "1", PATH_BABEL_PARSER, repo );
        assertThat( "Metadata not retrieved!", client.content().exists( repo.getKey(), PATH_BABEL_PARSER, true ),
                    equalTo( true ) );
        assertThat( "Metadata not retrieved!",
                    client.content().exists( repo.getKey(), PATH_BABEL_PARSER + "/package.json", true ),
                    equalTo( true ) );

        // wait for repo metadata timeout
        Thread.sleep( 3000 );

        assertThat( "Metadata not cleaned up!", client.content().exists( repo.getKey(), PATH_JQUERY, true ),
                    equalTo( false ) );
        assertThat( "Metadata not cleaned up!",
                    client.content().exists( repo.getKey(), PATH_JQUERY + "/package.json", true ), equalTo( false ) );

        assertThat( "Metadata not cleaned up!", client.content().exists( repo.getKey(), PATH_BABEL_PARSER, true ),
                    equalTo( false ) );
        assertThat( "Metadata not cleaned up!",
                    client.content().exists( repo.getKey(), PATH_BABEL_PARSER + "/package.json", true ),
                    equalTo( false ) );


        logger.info( "\n\n\n\nRE-REQUEST STARTS HERE\n\n\n\n" );

        // Second retrieval
        dts.setBeta( "2" );
        verifyMetadataBetaTag( "2", PATH_JQUERY, repo );
        verifyMetadataBetaTag( "2", PATH_BABEL_PARSER, repo );
    }

    private void verifyMetadataBetaTag( final String betaTag, final String path, RemoteRepository repo )
            throws IndyClientException, IOException
    {
        try (InputStream remote = client.content().get( repo.getKey(), path ))
        {
            assertThat( remote, notNullValue() );
            String json = IOUtils.toString( remote, Charset.defaultCharset() );
            PackageMetadata merged =
                    new IndyObjectMapper( true ).readValue( json, PackageMetadata.class );

            assertThat( merged.getDistTags().getBeta(), equalTo( betaTag ) );
        }
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
