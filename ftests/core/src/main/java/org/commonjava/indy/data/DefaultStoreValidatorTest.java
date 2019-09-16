package org.commonjava.indy.data;

import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;


import javax.inject.Inject;
import java.io.IOException;
import java.util.logging.Logger;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.junit.Assert.*;

public class DefaultStoreValidatorTest extends AbstractIndyFunctionalTest {

    @Inject
    StoreValidator validator;

//    private StoreDataManager manager;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DefaultStoreValidator.class);


//    @Rule
//    public ExpectationServer server = new ExpectationServer( "repos" );


    @Before
    public void setup()
        throws Exception
    {
        validator = new DefaultStoreValidator();
    }


    @Test
    public void run() throws Exception {

        assertNotNull(validator);

        final String changelog = "Create validation test structure";

        RemoteRepository validRepoSsl =
            new RemoteRepository( "maven", "validation-test", "https://repo.maven.apache.org/maven2/" );

        client.stores().create( validRepoSsl, changelog,RemoteRepository.class );

        RemoteRepository rr = client.stores().load( remote, "validation-test", RemoteRepository.class );


        ArtifactStoreValidateData validateDataSsl = validator.validate(rr);
        LOGGER.warn("Valid ArtifactStoreValidateData: " + validateDataSsl.toString());


        assertNotNull( validateDataSsl );
        assertFalse(validateDataSsl.isValid());
//        assertEquals(Integer.toString(200), validateDataSsl.getErrors().get("HTTP_GET_STATUS"));
//        assertEquals(Integer.toString(200), validateDataSsl.getErrors().get("HTTP_HEAD_STATUS"));

        RemoteRepository validRepo =
            new RemoteRepository( "maven", "validation-test-nossl", "http://repo.maven.apache.org/maven2/" );
        client.stores().create( validRepo, changelog,RemoteRepository.class );

        RemoteRepository rrnossl = client.stores().load( remote, "validation-test-nossl", RemoteRepository.class );


        ArtifactStoreValidateData validateData = validator.validate(rrnossl);
        LOGGER.warn("Valid ArtifactStoreValidateData: " + validateData.toString());


        assertNotNull( validateData );
        assertFalse(validateData.isValid());
//        assertEquals(Integer.toString(200), validateData.getErrors().get("HTTP_GET_STATUS"));
//        assertEquals(Integer.toString(200), validateData.getErrors().get("HTTP_HEAD_STATUS"));
    }

    protected void initBaseTestConfig( CoreServerFixture fixture )
        throws IOException
    {
        writeConfigFile( "conf.d/storage.conf", "[storage-default]\nstorage.dir=" + fixture.getBootOptions().getHomeDir() + "/var/lib/indy/storage" );
        writeConfigFile( "conf.d/internal_validation.conf", "[internal]\n_internal.store.validation.enabled=true" );

    }

}
