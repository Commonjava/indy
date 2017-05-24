package org.commonjava.indy.promote.metrics;

/**
 * Created by xiabai on 5/24/17.
 */
public class IndyPromoteMetricsNames
{
    public static final String EXCEPTION = "exception";

    public static final String METER = "meter";

    public static final String TIMER = "timer";

    private static final String COMMON_NAME = "org.commonjava.indy.promote";

    public static final String METHOD_PROMTETOGROUP = COMMON_NAME + ".promoteToGroup.";

    public static final String METHOD_ROLLBACKGROUPROMOTE = COMMON_NAME + ".rollbackGroupPromote.";

    public static final String METHOD_PROMOTEPATHS = COMMON_NAME + ".promotePaths.";

    public static final String METHOD_RESUMEPATHS = COMMON_NAME + ".resumePaths.";

    public static final String METHOD_ROLLBACKPATHS = COMMON_NAME + ".rollbackPaths.";
}
