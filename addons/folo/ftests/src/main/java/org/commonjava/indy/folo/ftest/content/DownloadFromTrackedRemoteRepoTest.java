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

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.folo.client.IndyFoloContentClientModule;
import org.junit.Test;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>RemoteRepository for maven and path</li>
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
public class DownloadFromTrackedRemoteRepoTest
    extends AbstractFoloContentManagementTest
{

    @Test
    public void downloadFileFromRemoteRepository()
        throws Exception
    {
        final String trackingId = newName();
        final String path = "org/commonjava/commonjava/2/commonjava-2.pom";
        centralServer.expect( centralServer.formatUrl( path ), 200, Thread.currentThread()
                                                                          .getContextClassLoader()
                                                                          .getResourceAsStream(
                                                                                  "folo-content/commonjava-2.pom" ) );

        final InputStream result = client.module( IndyFoloContentClientModule.class )
                                         .get( trackingId, remote, CENTRAL, path );
        assertThat( result, notNullValue() );

        final String pom = IOUtils.toString( result );
        result.close();
        assertThat( pom.contains( "<groupId>org.commonjava</groupId>" ), equalTo( true ) );
    }

}
