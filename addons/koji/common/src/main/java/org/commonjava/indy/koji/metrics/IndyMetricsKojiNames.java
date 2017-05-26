package org.commonjava.indy.koji.metrics;

import org.commonjava.indy.IndyMetricsNames;

/**
 * Created by xiabai on 5/26/17.
 */
public class IndyMetricsKojiNames
                extends IndyMetricsNames
{
    private static final String MODULE_PREFIX_NAME = INDY_METRICS_NAME_PREFIX + ".koji";

    private static final String MODULE_CONTENT_PREFIX_NAME = ".content.";

    public static final String METHOD_CONTENT_GETMETADATA =
                    MODULE_PREFIX_NAME + MODULE_CONTENT_PREFIX_NAME + "getMetadata.";

}
