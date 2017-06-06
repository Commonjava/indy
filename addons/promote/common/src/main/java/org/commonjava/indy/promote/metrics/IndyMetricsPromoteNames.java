package org.commonjava.indy.promote.metrics;

import org.commonjava.indy.IndyMetricsNames;

/**
 * Created by xiabai on 6/1/17.
 */
public class IndyMetricsPromoteNames
                extends IndyMetricsNames
{
    private static final String MODULE_PREFIX_NAME = "org.commonjava.indy.promote";

    private static final String MODULE_PROMOTIONMANAGER_PREFIX_NAME = ".promotionManager.";

    private static final String MODULE_PROMOTIONVALIDATOR_PREFIX_NAME = ".PromotionValidator.";

    public static final String METHOD_PROMOTIONMANAGER_PROMOTEPATHS =
                    MODULE_PREFIX_NAME + MODULE_PROMOTIONMANAGER_PREFIX_NAME + "promotePaths.";

    public static final String METHOD_PROMOTIONMANAGER_PROMOTTOGROUP =
                    MODULE_PREFIX_NAME + MODULE_PROMOTIONMANAGER_PREFIX_NAME + "promoteToGroup.";

    public static final String METHOD_PROMOTIONVALIDATOR_VALIDATE =
                    MODULE_PREFIX_NAME + MODULE_PROMOTIONMANAGER_PREFIX_NAME + "validate.";

}
