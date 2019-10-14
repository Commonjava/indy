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
package org.commonjava.indy.jaxrs;

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.conf.InternalFeatureConfig;
import org.commonjava.indy.conf.SslValidationConfig;
import org.commonjava.indy.data.ArtifactStoreValidateData;
import org.commonjava.indy.data.StoreValidator;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;
import static org.commonjava.indy.model.core.StoreType.remote;

public class IndySslValidationApiTest extends AbstractIndyFunctionalTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(IndySslValidationApiTest.class);


    StoreValidator validator;

    SslValidationConfig configuration;

    InternalFeatureConfig featureConfig;

    @Before
    public void before()
        throws Exception
    {
        configuration = lookup(SslValidationConfig.class);
        validator = lookup(StoreValidator.class);
        featureConfig = lookup(InternalFeatureConfig.class);
    }

    @Test
    public void run() {


        RemoteRepository storedTestRepo = null;
        RemoteRepository storedTestSslRepo = null;
        RemoteRepository storedTestAllowedRepo = null;

        try {
            HashMap<String, ArtifactStoreValidateData> remoteRepositoriesValidated =
                client.module(IndySslValidationClientModule.class).revalidateAllStores();

            LOGGER.info("=> All Validated Remote Repositories Response: " +remoteRepositoriesValidated);
            LOGGER.info("=> RESULT: " + remoteRepositoriesValidated.get(String.valueOf("maven:remote:central")));
            assertNotNull(remoteRepositoriesValidated);
            assertTrue(!remoteRepositoriesValidated.isEmpty());

        } catch (IndyClientException e) {LOGGER.warn("=> Exception in revalidating all store API call");}


        // REPO TESTING URL - http://repo.maven.apache.org/maven2 - NOT VALID NOT ALLOWED SSL REPO
        try {

            // first there is need for config variables to be set to false (remote.ssl.required , _internal.store.validation.enabled )
            if(configuration.isSSLRequired() == true) { configuration.setSslRequired(false); }
            if(featureConfig.getStoreValidation() == true) { featureConfig.setStoreValidation( false); }

            RemoteRepository testRepo = new RemoteRepository("maven", "test", "http://repo.maven.apache.org/maven2");

            LOGGER.info("=> Storing Remote RemoteRepository: " + testRepo.getUrl());
            client.stores().create(testRepo, "Testing Remote RemoteRepository", RemoteRepository.class);

            storedTestRepo = client.stores().load(remote, testRepo.getName(), RemoteRepository.class);

            // now there is need for config varables to be set to true (remote.ssl.required , _internal.store.validation.enabled )
            if(configuration.isSSLRequired() == false) { configuration.setSslRequired(true); }
            if(featureConfig.getStoreValidation() == false) { featureConfig.setStoreValidation( true); }

            LOGGER.info("=> Validating Remote RemoteRepository: " + testRepo.getUrl());
            ArtifactStoreValidateData remoteRepoAPIResult =
                client.module(IndySslValidationClientModule.class).revalidateStore(storedTestRepo);


            LOGGER.info("=> API Returned Result [Validate Remote Repo]: " +  remoteRepoAPIResult  );


            assertNotNull(remoteRepoAPIResult);
            assertFalse(remoteRepoAPIResult.isValid());
            assertThat(remoteRepoAPIResult.getRepositoryUrl(), is(remoteRepoAPIResult.getErrors().get("NOT_ALLOWED_SSL")));

        } catch (IndyClientException e) {
            LOGGER.warn("=> Exception in revalidating store " + storedTestRepo.getUrl() +" API call");
        }

        // REPO TESTING URL - https://repo.maven.apache.org/maven2 - VALID SSL REPO
        try {

            RemoteRepository testRepoSsl = new RemoteRepository("maven", "test-ssl", "https://repo.maven.apache" +
                ".org/maven2");

            LOGGER.info("=> Storing Remote RemoteRepository: " + testRepoSsl.getUrl());
            client.stores().create(testRepoSsl, "Testing SSL Remote RemoteRepository", RemoteRepository.class);

            storedTestSslRepo = client.stores().load(remote, testRepoSsl.getName(), RemoteRepository.class);

            LOGGER.info("=> Validating Remote RemoteRepository: " + testRepoSsl.getUrl());
            ArtifactStoreValidateData remoteSslRepoAPIResult =
                client.module(IndySslValidationClientModule.class).revalidateStore(storedTestSslRepo);

            LOGGER.info("=> API Returned Result [Validate Remote Repo]: " +  remoteSslRepoAPIResult  );

            assertNotNull(remoteSslRepoAPIResult);
            assertTrue(remoteSslRepoAPIResult.isValid());
            assertThat(String.valueOf(200), is(remoteSslRepoAPIResult.getErrors().get("HTTP_HEAD_STATUS")));
            assertThat(String.valueOf(200), is(remoteSslRepoAPIResult.getErrors().get("HTTP_GET_STATUS")));


        } catch (IndyClientException ice) {
            LOGGER.warn("=> Exception in revalidating store " + storedTestSslRepo.getUrl() +" API call");
        }


        // REPO TESTING URL - https://repo.maven.apache.org/maven2 - NOT VALID , ALLOWED , NOT AVAILABLE NON-SSL REPO
        try {

            RemoteRepository testRepoAllowed = new RemoteRepository("maven", "test-ssl", "http://127.0.0.1/maven2");

            // first there is need for config variables to be set to false (remote.ssl.required , _internal.store.validation.enabled )
            if(configuration.isSSLRequired() == true) { configuration.setSslRequired(false); }
            if(featureConfig.getStoreValidation() == true) { featureConfig.setStoreValidation( false); }

            LOGGER.info("=> Storing Remote RemoteRepository: " + testRepoAllowed.getUrl());
            client.stores().create(testRepoAllowed, "Testing SSL Remote RemoteRepository", RemoteRepository.class);

            storedTestAllowedRepo = client.stores().load(remote, testRepoAllowed.getName(), RemoteRepository.class);


            // now there is need for config varables to be set to true (remote.ssl.required , _internal.store.validation.enabled )
            if(configuration.isSSLRequired() == false) { configuration.setSslRequired(true); }
            if(featureConfig.getStoreValidation() == false) { featureConfig.setStoreValidation( true); }

            LOGGER.info("=> Validating Remote RemoteRepository: " + testRepoAllowed.getUrl());
            ArtifactStoreValidateData remoteAllowedRepoAPIResult =
                client.module(IndySslValidationClientModule.class).revalidateStore(storedTestSslRepo);

            LOGGER.info("=> API Returned Result [Validate Remote Repo]: " +  remoteAllowedRepoAPIResult  );

            assertNotNull(remoteAllowedRepoAPIResult);
            assertFalse(remoteAllowedRepoAPIResult.isValid());
            assertTrue(remoteAllowedRepoAPIResult.getErrors().keySet().contains("Exception"));


        } catch (IndyClientException ice) {
            LOGGER.warn("=> Exception in revalidating store " + storedTestAllowedRepo.getUrl() +" API call");
        }

        // GET ALL VALIDATED REMOTE REPOSITORIES
        try {

            HashMap<String, ArtifactStoreValidateData> remoteSslRepositoriesValidated =
                client.module(IndySslValidationClientModule.class).revalidateAllStores();

            LOGGER.info("=> All Validated Remote Repositories Response: " + remoteSslRepositoriesValidated);

            assertNotNull(remoteSslRepositoriesValidated);
            assertTrue(!remoteSslRepositoriesValidated.isEmpty());


        } catch (IndyClientException ice) {
            LOGGER.warn("=> Exception in revalidating SECOND all store API call");
        }


    }


    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Arrays.<IndyClientModule>asList( new IndySslValidationClientModule() );
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture ) throws IOException
    {
        writeConfigFile( "conf.d/internal-features.conf", "[_internal]\nstore.validation.enabled=true" );
        writeConfigFile( "conf.d/ssl.conf", "[ssl]\nremote.nossl.hosts=localhost,127.0.0.1\nremote.ssl.required=true\n");
    }
}
