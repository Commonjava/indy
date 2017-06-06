package org.commonjava.indy.core.bind.jaxrs.metrics;

import org.commonjava.indy.IndyMetricsNames;

/**
 * Created by xiabai on 6/2/17.
 */
public class IndyMetricsBindingsNames extends IndyMetricsNames
{
    private static final String MODULE_PREFIX_NAME = "org.commonjava.indy.bindings.jaxrs";

    private static final String MODULE_TRANSFERSTREAMING_PREFIX_NAME = ".transferStreaming.";

    public static final String METHOD_TRANSFERSTREAMING_WRITE =
                    MODULE_PREFIX_NAME + MODULE_TRANSFERSTREAMING_PREFIX_NAME + "write.";
}
