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
package org.commonjava.indy.data;


import org.commonjava.indy.model.core.ArtifactStore;

import javax.enterprise.context.ApplicationScoped;
import java.net.MalformedURLException;

/**
 * Store Validator  used to validate URL for for {@link ArtifactStore} instances and
 * check if appropriate http method calls are available at validated url endpoint
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
