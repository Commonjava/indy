package org.commonjava.indy.data;

import org.commonjava.indy.conf.DefaultIndyConfiguration;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.conf.SslValidationConfig;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;



import javax.inject.Inject;
import java.io.IOException;
import java.net.MalformedURLException;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;

//@RunWith( WeldJUnit4Runner.class)
public class DefaultStoreValidatorTest extends AbstractIndyFunctionalTest {

//    @Inject
    StoreValidator validator;

//    @Inject
    SslValidationConfig configuration;




    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DefaultStoreValidatorTest.class);

    @Before
    public void before()
        throws Exception
    {
        configuration = lookup(SslValidationConfig.class);
        validator = lookup(StoreValidator.class);
    }

    @Test
    public void run() throws Exception {

        assertNotNull(validator);
        assertNotNull(configuration);

        final String changelog = "Create repo validation test structure";

        RemoteRepository validRepoSsl =
            new RemoteRepository( "maven", "validation", "https://repo.maven.apache.org/maven2/" );

        LOGGER.warn("=> Start Validating RemoteRepository: " + validRepoSsl.getUrl());
        RemoteRepository remoteRepository = client.stores().create(validRepoSsl, changelog, RemoteRepository.class);


//        ArtifactStoreValidateData validateDataSsl = validator.validate(remoteRepository);
//        LOGGER.warn("=> Valid SSL ArtifactStoreValidateData: " + validateDataSsl.toString());


//        assertNotNull( validateDataSsl );
//        assertTrue(validateDataSsl.isValid());
//        assertThat(Integer.toString(200), is(validateDataSsl.getErrors().get("HTTP_GET_STATUS")));
//        assertThat(Integer.toString(200), is(validateDataSsl.getErrors().get("HTTP_HEAD_STATUS")));

        RemoteRepository validRepo =
            new RemoteRepository( "maven", "validation-test-nossl", "http://repo.maven.apache.org/maven2/" );

        LOGGER.warn("=> Start Validating RemoteRepository: " + validRepo.getUrl());
        RemoteRepository remoteRepository1 = client.stores().create(validRepo, changelog, RemoteRepository.class);


//        ArtifactStoreValidateData validateData = validator.validate(remoteRepository1);
//        LOGGER.warn("=> Not Valid SSL ArtifactStoreValidateData: " + validateData.toString());


//        assertNotNull( validateData );
//        assertFalse(validateData.isValid());
//        assertNull(validateData.getErrors().get("HTTP_GET_STATUS"));
//        assertNull(validateData.getErrors().get("HTTP_HEAD_STATUS"));
//        assertNotNull( validateData.getErrors().get("disabled") );

        RemoteRepository notValidUrlRepo =
            new RemoteRepository( "maven", "validation-test-url", "not.valid.url" );

        LOGGER.warn("=> Start Validating RemoteRepository: " + notValidUrlRepo.getUrl());
        RemoteRepository remoteRepository2 = client.stores().create(notValidUrlRepo, changelog, RemoteRepository.class);

//        ArtifactStoreValidateData validateUrl = null;
//        try {
//            LOGGER.warn("=> Start Validating RemoteRepository: " + remoteRepository2.getUrl());
//            validateUrl = validator.validate(remoteRepository2);
//            LOGGER.warn("=> Not Valid URL ArtifactStoreValidateData: " + validateUrl.toString());
//        } catch (MalformedURLException mue) {
//            if(validateUrl != null ) {
//                LOGGER.warn("=> Not Valid URL Exception in ArtifactStoreValidateData: " + validateUrl.toString());
//            }
//        }


        RemoteRepository allowedRemoteRepo =
            new RemoteRepository( "maven", "validation-indy-allowed", "http://127.0.0.1" );
        LOGGER.warn("=> Start Validating RemoteRepository: " + allowedRemoteRepo.getUrl());
        RemoteRepository remoteRepository3 = client.stores().create(allowedRemoteRepo, changelog,
            RemoteRepository.class);


//        ArtifactStoreValidateData validateAllowedRepo = validator.validate(remoteRepository3);
//        LOGGER.warn("=> Allowed Not Valid SSL ArtifactStoreValidateData: " + validateAllowedRepo.toString());

    }


    @Override
    protected void initBaseTestConfig( CoreServerFixture fixture )
        throws IOException
    {

        writeConfigFile( "conf.d/ssl.conf", "[ssl]\nremote.nossl.hosts=localhost,127.0.0.1\nremote.ssl.required=true\n");
        writeConfigFile( "conf.d/storage.conf", "[storage-default]\nstorage.dir=" + fixture.getBootOptions().getHomeDir() + "/var/lib/indy/storage" );
        writeConfigFile( "conf.d/internal_validation.conf", "[internal]\n_internal.store.validation.enabled=true" );


    }

    @Override
    protected void initTestConfig(CoreServerFixture fixture) throws IOException {
        writeConfigFile( "conf.d/ssl.conf", "[ssl]\nremote.nossl.hosts=localhost,127.0.0.1\nremote.ssl.required=true\n");
        writeConfigFile( "conf.d/default-main.conf", "[ssl]\nremote.nossl.hosts=localhost,127.0.0.1\nremote.ssl" +
            ".required=true\n");

    }
}
