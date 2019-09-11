package org.commonjava.indy.data;

import org.commonjava.indy.model.core.RemoteRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;


import java.util.logging.Logger;

import static org.junit.Assert.*;

public class DefaultStoreValidatorTest {

    StoreValidator validator;

    private StoreDataManager manager;

    Logger logger;


    @Before
    public void setup()
        throws Exception
    {
        logger = logger.getLogger(DefaultStoreValidatorTest.class.getName());
        validator = new DefaultStoreValidator();
    }


    @Test
    public void testRemoteRepositoryValidation() throws Exception {

        assertNotNull(validator);

        RemoteRepository validRepo = new RemoteRepository( "maven", "test", "https://repo.maven.apache.org" );

        ArtifactStoreValidateData validateData = validator.validate(validRepo);

        logger.info("Valid ArtifactStoreValidateData: " + validateData.toString());

        assertNotNull( validateData );
//        assertTrue(validateData.isValid());
//        assertEquals(Integer.toString(200), validateData.getErrors().get("HTTP_GET_STATUS"));
//        assertEquals(Integer.toString(200), validateData.getErrors().get("HTTP_HEAD_STATUS"));

    }

}
