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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.helper.HttpResources;
import org.commonjava.indy.folo.client.IndyFoloAdminClientModule;
import org.commonjava.indy.folo.dto.TrackedContentDTO;
import org.commonjava.indy.folo.dto.TrackedContentEntryDTO;
import org.junit.Test;

public class RetrievedPomInAlwaysOnTrackingReportTest
    extends AbstractHttproxTrackingFunctionalTest
{

    private static final String USER = "user";

    private static final String PASS = "password";

    @Test
    public void run()
        throws Exception
    {
        final String testRepo = "test";
        final PomRef pom = loadPom( "simple.pom", Collections.<String, String> emptyMap() );
        final String url = server.formatUrl( testRepo, pom.path );
        server.expect( url, 200, pom.pom );

        final HttpGet get = new HttpGet( url );
        final CloseableHttpClient client = proxiedHttp();
        CloseableHttpResponse response = null;

        InputStream stream = null;
        try
        {
            response = client.execute( get, proxyContext( USER, PASS ) );
            assertThat( response.getStatusLine()
                                .getStatusCode(), equalTo( 200 ) );

            stream = response.getEntity()
                             .getContent();
            final String resultingPom = IOUtils.toString( stream );

            assertThat( resultingPom, notNullValue() );
            assertThat( resultingPom, equalTo( pom.pom ) );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
            HttpResources.cleanupResources( get, response, client );
        }

        assertThat( this.client.module( IndyFoloAdminClientModule.class ).sealTrackingRecord( USER ),
                    equalTo( true ) );

        final TrackedContentDTO content = this.client.module( IndyFoloAdminClientModule.class )
                                                       .getRawTrackingContent( USER );
        assertThat( content, notNullValue() );

        final Set<TrackedContentEntryDTO> downloads = content.getDownloads();
        assertThat( downloads, notNullValue() );
        assertThat( downloads.size(), equalTo( 1 ) );

        final TrackedContentEntryDTO entry = downloads.iterator().next();
        assertThat( entry, notNullValue() );

        final String downloadPath = entry.getPath();
        assertThat( downloadPath, notNullValue() );
        assertThat( downloadPath, equalTo( "/test/" + pom.path ) );

        //TODO: As new api format for "GET /folo/admin/track/record", these tests are deperacated, will keep for a while and remove later
//        final String repoName = "httprox_127-0-0-1";

//        final TrackedContentRecord record = this.client.module( IndyFoloAdminClientModule.class )
//                                                       .getRawTrackingRecord( USER );

//        final Map<StoreKey, AffectedStoreRecord> affectedStores = record.getAffectedStores();
//        assertThat( affectedStores, notNullValue() );
//        assertThat( affectedStores.size(), equalTo( 1 ) );

//        final AffectedStoreRecord storeRecord = affectedStores.get( new StoreKey( StoreType.remote, repoName ) );
//        assertThat( storeRecord, notNullValue() );
//
//        final Set<String> downloads = storeRecord.getDownloadedPaths();
//        assertThat( downloads, notNullValue() );
//        assertThat( downloads.size(), equalTo( 1 ) );
//        assertThat( downloads.iterator()
//                             .next(), equalTo( "/test/" + pom.path ) );
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Collections.<IndyClientModule> singleton( new IndyFoloAdminClientModule() );
    }

    @Override
    protected String getAdditionalHttproxConfig()
    {
        return super.getAdditionalHttproxConfig() + "\ntracking.type=always";
    }

}
