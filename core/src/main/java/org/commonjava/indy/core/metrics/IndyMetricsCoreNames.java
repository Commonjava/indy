package org.commonjava.indy.core.metrics;

import org.commonjava.indy.IndyMetricsNames;

/**
 * Created by xiabai on 6/1/17.
 */
public class IndyMetricsCoreNames extends IndyMetricsNames
{
    private static final String MODULE_PREFIX_NAME = "org.commonjava.indy.core";

    private static final String MODULE_DEFAULTCONTENTMANAGER_PREFIX_NAME = ".defaultContentManager.";

    private static final String MODULE_STORECONTENTLISTENER_PREFIX_NAME = ".storeContentListener.";

    public static final String METHOD_DEFAULTCONTENTMANAGER_RETRIEVE =
                    MODULE_PREFIX_NAME + MODULE_DEFAULTCONTENTMANAGER_PREFIX_NAME    + "retrieve.";

    public static final String METHOD_DEFAULTCONTENTMANAGER_DELETE =
                    MODULE_PREFIX_NAME + MODULE_DEFAULTCONTENTMANAGER_PREFIX_NAME    + "delete.";

    public static final String METHOD_DEFAULTCONTENTMANAGER_STORE =
                    MODULE_PREFIX_NAME + MODULE_DEFAULTCONTENTMANAGER_PREFIX_NAME    + "store.";

    public static final String METHOD_DEFAULTCONTENTMANAGER_LIST =
                    MODULE_PREFIX_NAME + MODULE_DEFAULTCONTENTMANAGER_PREFIX_NAME    + "list.";


    public static final String METHOD_STORECONTENTLISTENER_ONSTOREUPDATE =
                    MODULE_PREFIX_NAME + MODULE_STORECONTENTLISTENER_PREFIX_NAME    + "onStoreUpdate.";

    public static final String METHOD_STORECONTENTLISTENER_ONSTOREDISABLE =
                    MODULE_PREFIX_NAME + MODULE_STORECONTENTLISTENER_PREFIX_NAME    + "onStoreDisable.";

    public static final String METHOD_STORECONTENTLISTENER_ONSTOREDELETION =
                    MODULE_PREFIX_NAME + MODULE_STORECONTENTLISTENER_PREFIX_NAME    + "onStoreDeletion.";
}
