package org.commonjava.indy.content.metrics;

import org.commonjava.indy.IndyMetricsNames;

/**
 * Created by xiabai on 6/2/17.
 */
public class IndyMetricsContentIndexNames
                extends IndyMetricsNames
{
    private static final String MODULE_PREFIX_NAME = "org.commonjava.indy.content.index";

    private static final String MODULE_INDEXINGCONTENTMANAGER_PREFIX_NAME = ".IndexingContentManager.";

    public static final String METHOD_INDEXINGCONTENTMANAGER_GETINDEXEDTRANSFER =
                    MODULE_PREFIX_NAME + MODULE_INDEXINGCONTENTMANAGER_PREFIX_NAME + "getIndexedTransfer.";

    public static final String METHOD_INDEXINGCONTENTMANAGER_GETINDEXEDMEMBERTRANSFER =
                    MODULE_PREFIX_NAME + MODULE_INDEXINGCONTENTMANAGER_PREFIX_NAME + "getIndexedMemberTransfer.";


}
