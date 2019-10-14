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
import org.commonjava.indy.client.core.helper.HttpResources;
import org.commonjava.indy.client.core.util.UrlUtils;
import org.commonjava.indy.data.ArtifactStoreValidateData;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.RemoteRepository;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class IndySslValidationClientModule extends IndyClientModule {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(IndySslValidationClientModule.class);


    // REST API paths
    private static String HTTP_POST_REVALIDATE_ALL = "admin/stores/maven/remote/revalidate/all";
    private static String HTTP_POST_REVALIDATE_STORE = "admin/stores/maven/remote/";
    private static String HTTP_POST_REVALIDATE_ALL_DISABLED = "admin/stores/maven/remote/revalidate/all/disabled";

    // Http Call Methods

    public HashMap<String, ArtifactStoreValidateData> revalidateAllStores() throws IndyClientException {
        LOGGER.info("=> Sending API Call  to: " + UrlUtils.buildUrl( "", HTTP_POST_REVALIDATE_ALL ));
        return
            http.postWithResponse(
                UrlUtils.buildUrl( "", HTTP_POST_REVALIDATE_ALL), "", HashMap.class);
    }


    public ArtifactStoreValidateData revalidateStore(RemoteRepository store) throws IndyClientException {
        LOGGER.info("=> Sending API Call  to: " + UrlUtils.buildUrl( "",
            HTTP_POST_REVALIDATE_STORE + store.getName() + "/revalidate" ));
        return
            http.postWithResponse(
                UrlUtils.buildUrl( "", HTTP_POST_REVALIDATE_STORE + store.getName() + "/revalidate"), "",ArtifactStoreValidateData.class);
    }

}
