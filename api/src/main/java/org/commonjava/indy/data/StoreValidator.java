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




    /*
            Validate ArtifactStore instances
         */
    public ArtifactStoreValidateData validate(ArtifactStore artifactStore)
        throws InvalidArtifactStoreException, MalformedURLException;
}
