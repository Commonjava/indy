/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.jaxrs;


import org.apache.http.client.methods.HttpHead;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientHttp;
import org.commonjava.indy.client.core.helper.HttpResources;
import org.commonjava.indy.client.core.module.IndyRawHttpModule;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.RemoteRepository;
import static org.junit.Assert.*;

import static org.hamcrest.core.Is.*;

import org.hamcrest.core.Is;
import org.junit.Test;

import javax.validation.constraints.AssertTrue;

public class ContentBrowseHeadRequestTest extends AbstractIndyFunctionalTest {

    private static final String REMOTE_MAVEN_CENTRAL = "/browse/maven/remote/central";
    private static final String REMOTE_NPM_CENTRAL = "/browse/npm/remote/npmjs";

    private static final String REMOTE_MAVEN_CENTRAL_URL = "https://repo.maven.apache.org/maven2/";
    private static final String REMOTE_NPM_CENTRAL_URL = "https://npmjs.com";


    private static final String REMOTE_MAVEN_TEST_SUB_URL = "/browse/maven/remote/m-central/test/test";
    private static final String REMOTE_NPM_TEST_SUB_URL = "/browse/npm/remote/n-central/test/test";

    @Test
    public void run() {

        final String changelog = "Create repo validation test structure";

        try {
            RemoteRepository mavenCentral =
              new RemoteRepository( "maven", "m-central", REMOTE_MAVEN_CENTRAL_URL );
            RemoteRepository npmCentral =
              new RemoteRepository( "npm", "n-central", REMOTE_NPM_CENTRAL_URL );


            client.stores().create(mavenCentral,changelog,RemoteRepository.class);
            client.stores().create(npmCentral,changelog,RemoteRepository.class);


            IndyClientHttp client = getHttp();


            HttpHead httpMavenHeadReq = new HttpHead(REMOTE_MAVEN_CENTRAL);
            HttpResources mavenResponse = client.execute(httpMavenHeadReq);
            assertThat(200, is(mavenResponse.getStatusCode()));

            HttpHead httpNpmHeadReq = new HttpHead(REMOTE_NPM_CENTRAL);
            HttpResources npmResponse = client.execute(httpNpmHeadReq);
            assertThat(200, is(npmResponse.getStatusCode()));


            HttpHead httpMavenHeadSubReq = new HttpHead(REMOTE_MAVEN_TEST_SUB_URL);
            HttpResources mavenSubResponse = client.execute(httpMavenHeadSubReq);
            assertThat(200, is(mavenSubResponse.getStatusCode()));

            HttpHead httpNpmHeadSubReq = new HttpHead(REMOTE_NPM_TEST_SUB_URL);
            HttpResources npmSubResponse = client.execute(httpNpmHeadSubReq);
            assertThat(200, is(npmSubResponse.getStatusCode()));




        } catch (IndyClientException e) {
            logger.error( e.getMessage(), e );
        }


    }


    protected IndyClientHttp getHttp()
      throws IndyClientException
    {
        return client.module( IndyRawHttpModule.class )
          .getHttp();
    }
}
