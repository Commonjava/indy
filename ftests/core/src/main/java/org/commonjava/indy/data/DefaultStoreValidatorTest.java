package org.commonjava.indy.data;

import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;



import javax.inject.Inject;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;

@RunWith( WeldJUnit4Runner.class)
public class DefaultStoreValidatorTest extends AbstractIndyFunctionalTest {

    @Inject
    StoreValidator validator;


    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DefaultStoreValidatorTest.class);



    @Test
    public void run() throws Exception {

        assertNotNull(validator);

        final String changelog = "Create repo validation test structure";

        RemoteRepository validRepoSsl =
            new RemoteRepository( "maven", "validation", "https://repo.maven.apache.org/maven2/" );

        RemoteRepository remoteRepository = client.stores().create(validRepoSsl, changelog, RemoteRepository.class);

//        RemoteRepository rr = client.stores().load( remote, "validation", RemoteRepository.class );


        ArtifactStoreValidateData validateDataSsl = validator.validate(remoteRepository);
        LOGGER.warn("=> Valid ArtifactStoreValidateData: " + validateDataSsl.toString());


        assertNotNull( validateDataSsl );
        assertTrue(validateDataSsl.isValid());
        assertThat(Integer.toString(200), is(validateDataSsl.getErrors().get("HTTP_GET_STATUS")));
        assertThat(Integer.toString(200), is(validateDataSsl.getErrors().get("HTTP_HEAD_STATUS")));

        RemoteRepository validRepo =
            new RemoteRepository( "maven", "validation-test-nossl", "http://repo.maven.apache.org/maven2/" );

        RemoteRepository remoteRepository1 = client.stores().create(validRepo, changelog, RemoteRepository.class);

//        RemoteRepository rrnossl = client.stores().load( remote, "validation-test-nossl", RemoteRepository.class );


        ArtifactStoreValidateData validateData = validator.validate(remoteRepository1);
        LOGGER.warn("=> Valid ArtifactStoreValidateData: " + validateData.toString());


        assertNotNull( validateData );
        assertFalse(validateData.isValid());
        assertNull(validateData.getErrors().get("HTTP_GET_STATUS"));
        assertNull(validateData.getErrors().get("HTTP_HEAD_STATUS"));
    }


    @Override
    protected void initBaseTestConfig( CoreServerFixture fixture )
        throws IOException
    {

        writeConfigFile( "conf.d/storage.conf", "[storage-default]\nstorage.dir=" + fixture.getBootOptions().getHomeDir() + "/var/lib/indy/storage" );
        writeConfigFile( "conf.d/internal_validation.conf", "[internal]\n_internal.store.validation.enabled=true" );


    }

}
