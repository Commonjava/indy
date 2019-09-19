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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;

import java.io.InputStream;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * This case tests the tarball generation when upload
 * when: <br />
 * <ul>
 *      <li>creates a hosted repo</li>
 *      <li>stores the project's package.json (with field '_attachments') in the hosted repo</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the tarball file can be generated successfully as _attachment base64 decode</li>
 * </ul>
 */
public class NPMTarballContentGenerationWhenUploadTest
        extends AbstractContentManagementTest
{
    @Test
    public void test()
            throws Exception
    {
        final String content = IOUtils.toString(
                Thread.currentThread().getContextClassLoader().getResourceAsStream( "package-1.5.1.json" ) );

        final String path = "jquery";
        final String tarballPath = "jquery/-/jquery-1.5.1.tgz";

        final String repoName = "test-hosted";
        HostedRepository repo = new HostedRepository( NPM_PKG_KEY, repoName );

        repo = client.stores().create( repo, "adding npm hosted repo", HostedRepository.class );

        StoreKey storeKey = repo.getKey();
        assertThat( client.content().exists( storeKey, path ), equalTo( false ) );

        client.content().store( storeKey, path, IOUtils.toInputStream( content ) );

        assertThat( client.content().exists( storeKey, path ), equalTo( true ) );
        assertThat( client.content().exists( storeKey, tarballPath ), equalTo( true ) );

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree( content );
        JsonNode anode = root.path( "_attachments" );
        String tarballBase64 = anode.findPath( "data" ).asText();

        InputStream tarball = client.content().get( storeKey, tarballPath );

        assertEquals( tarballBase64, Base64.encodeBase64String( ( IOUtils.toByteArray( tarball ) ) ) );

        tarball.close();
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
