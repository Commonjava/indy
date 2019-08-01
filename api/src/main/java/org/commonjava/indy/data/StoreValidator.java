package org.commonjava.indy.data;


import org.commonjava.indy.model.core.ArtifactStore;

import javax.enterprise.context.ApplicationScoped;
import java.net.MalformedURLException;

/**
 * Store Validator  used to validate URL for for {@link ArtifactStore} instances and
 * check if appropriate http method calls are availible at validated url endpoint
 *
 * @author ggeorgie
 */
@ApplicationScoped
public interface StoreValidator {

    final static String MAILFORMED_URL = "MalformedURLException";
    final static String HTTP_GET_OR_HEAD = "IOException" ;
    final static String GENERAL = "Exception" ;
    final static String HTTP_GET_STATUS = "HTTP_GET_STATUS";
    final static String HTTP_HEAD_STATUS = "HTTP_HEAD_STATUS";
    final static String HTTP_PROTOCOL = "HTTP_PROTOCOL";
    final static String NON_SSL = "NON-SSL";

    /*
            Validate ArtifactStore instances
         */
    public ArtifactStoreValidateData validate(ArtifactStore artifactStore)
        throws InvalidArtifactStoreException, MalformedURLException;
}
