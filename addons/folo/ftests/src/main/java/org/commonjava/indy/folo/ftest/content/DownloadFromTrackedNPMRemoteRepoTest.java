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
package org.commonjava.indy.folo.ftest.content;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.folo.client.IndyFoloContentClientModule;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>RemoteRepository for npm and path</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Access path through folo track</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>The path can be got correctly</li>
 * </ul>
 */
public class DownloadFromTrackedNPMRemoteRepoTest
        extends AbstractNPMFoloContentManagementTest
{

    @Test
    public void downloadFileFromRemoteRepository()
            throws Exception
    {
        final String packageContent =
                "{\"name\": \"jquery\",\n" + "\"description\": \"JavaScript library for DOM operations\",\n" + "\"license\": \"MIT\"}";
        final String versionContent = "{\"name\": \"jquery\",\n" + "\"url\": \"jquery.com\",\n" + "\"version\": \"1.1.0\"}";

        final String packagePath = "jquery";
        final String versionPath = "jquery/1.1.0";

        final String trackingId = newName();

        npmjsServer.expect( npmjsServer.formatUrl( packagePath ), 200, new ByteArrayInputStream( packageContent.getBytes() ) );
        npmjsServer.expect( npmjsServer.formatUrl( versionPath ), 200, new ByteArrayInputStream( versionContent.getBytes() ) );

        final StoreKey storeKey = new StoreKey( NPM_PKG_KEY, remote, NPMJS );

        IndyFoloContentClientModule folo = client.module( IndyFoloContentClientModule.class );

        final InputStream packageStream = folo.get( trackingId, storeKey,  packagePath );
        final InputStream versionStream = folo.get( trackingId, storeKey, versionPath );

        assertThat( packageStream, notNullValue() );
        assertThat( versionStream, notNullValue() );

        assertThat( IOUtils.toString( packageStream ), equalTo( packageContent ) );
        assertThat( IOUtils.toString( versionStream ), equalTo( versionContent ) );

        packageStream.close();
        versionStream.close();
    }

}
