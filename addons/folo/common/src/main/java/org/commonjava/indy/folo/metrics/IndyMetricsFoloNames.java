package org.commonjava.indy.folo.metrics;

import org.commonjava.indy.IndyMetricsNames;

/**
 * Created by xiabai on 6/2/17.
 */
public class IndyMetricsFoloNames
                extends IndyMetricsNames
{
    private static final String MODULE_PREFIX_NAME = "org.commonjava.indy.folo";

    private static final String MODULE_FOLORECORDCACHE_PREFIX_NAME = ".foloRecordCache.";

    public static final String METHOD_FOLORECORDCACHE_SEAL =
                    MODULE_PREFIX_NAME + MODULE_FOLORECORDCACHE_PREFIX_NAME + "seal.";

    public static final String METHOD_FOLORECORDCACHE_RECORDARTIFACT =
                    MODULE_PREFIX_NAME + MODULE_FOLORECORDCACHE_PREFIX_NAME + "recordArtifact.";

    public static final String METHOD_FOLORECORDCACHE_DELETE =
                    MODULE_PREFIX_NAME + MODULE_FOLORECORDCACHE_PREFIX_NAME + "delete.";

    public static final String METHOD_FOLORECORDCACHE_HASINPROGRESSRECORD =
                    MODULE_PREFIX_NAME + MODULE_FOLORECORDCACHE_PREFIX_NAME +"hasInProgressRecord.";
}
