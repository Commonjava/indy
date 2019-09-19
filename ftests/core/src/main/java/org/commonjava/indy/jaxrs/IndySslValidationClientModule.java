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


    // Http Call Methods

    public HashMap<String, ArtifactStoreValidateData> revalidateAllStores() throws IndyClientException {
        LOGGER.info("=> Sending API Call  to: " + UrlUtils.buildUrl( "", HTTP_POST_REVALIDATE_ALL ));
        return
            http.postWithResponse(
                UrlUtils.buildUrl( "", HTTP_POST_REVALIDATE_ALL
            ), "", HashMap.class);
    }


    public ArtifactStoreValidateData revalidateStore(RemoteRepository store) throws IndyClientException {
        LOGGER.info("=> Sending API Call  to: " + UrlUtils.buildUrl( "",
            HTTP_POST_REVALIDATE_STORE + store.getName() + "/revalidate" ));
        return
            http.postWithResponse(
                UrlUtils.buildUrl( "", HTTP_POST_REVALIDATE_STORE + store.getName() + "/revalidate"
            ), "",ArtifactStoreValidateData.class);
    }

}
