package org.commonjava.indy.jaxrs;

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.helper.HttpResources;
import org.commonjava.indy.client.core.util.UrlUtils;
import org.commonjava.indy.model.core.ArtifactStore;

public class IndySslValidationClientModule extends IndyClientModule {

    // REST API paths
    private static String HTTP_POST_REVALIDATE_ALL = "/api/admin/stores/maven/remote/revalidate/all";
    private static String HTTP_POST_REVALIDATE_STORE = "/api/admin/stores/maven/remote/test/revalidate";


    // Http Call Methods

    public HttpResources revalidateAllStores() throws IndyClientException {
        return
            http.
                postRaw( UrlUtils.buildUrl( "", HTTP_POST_REVALIDATE_ALL ), null);
    }


    public HttpResources revalidateStore(ArtifactStore store) throws IndyClientException {
        return
            http.
                postRaw( UrlUtils.buildUrl( "", HTTP_POST_REVALIDATE_STORE ), null);
    }

}
