package org.commonjava.indy.content.metrics;

import org.commonjava.indy.IndyMetricsNames;

/**
 * Created by xiabai on 6/2/17.
 */
public class IndyMetricsContentIndexNames
                extends IndyMetricsNames
{
    private static final String MODULE_PREFIX_NAME = "org.commonjava.indy.content.index";

    private static final String MODULE_CONTENTINDEXMANAGER_PREFIX_NAME = ".contentIndexManager.";

    public static final String METHOD_CONTENTINDEXMANAGER_REMOVEINDEXSTOREPATH =
                    MODULE_PREFIX_NAME + MODULE_CONTENTINDEXMANAGER_PREFIX_NAME + "removeIndexedStorePath.";

    public static final String METHOD_CONTENTINDEXMANAGER_INDEXPATHINSTORES =
                    MODULE_PREFIX_NAME + MODULE_CONTENTINDEXMANAGER_PREFIX_NAME + "indexPathInStores.";

    public static final String METHOD_CONTENTINDEXMANAGER_CLEARINDEXEDPATHFROM =
                    MODULE_PREFIX_NAME + MODULE_CONTENTINDEXMANAGER_PREFIX_NAME + "clearIndexedPathFrom.";

    public static final String METHOD_CONTENTINDEXMANAGER_DEINDEXSTOREPATH =
                    MODULE_PREFIX_NAME + MODULE_CONTENTINDEXMANAGER_PREFIX_NAME + "deIndexStorePath.";
}
