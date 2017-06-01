package org.commonjava.indy.bind.jaxrs.metrics;

import org.commonjava.indy.IndyMetricsNames;

/**
 * Created by xiabai on 6/1/17.
 */
public class IndyMetricsJaxrsNames extends IndyMetricsNames
{
    private static final String MODULE_PREFIX_NAME = "org.commonjava.indy.subsys.jaxrs";

    private static final String MODULE_RESOURCEMANAGEMENT_PREFIX_NAME = ".resourceManagement.";

    private static final String MODULE_PROXYRESPONSEWRITER_PREFIX_NAME = ".ProxyResponseWriter.";

    public static final String METHOD_RESOURCEMANAGEMENT_DOFILTERE =
                    MODULE_PREFIX_NAME + MODULE_RESOURCEMANAGEMENT_PREFIX_NAME + "doFilter.";

    public static final String METHOD_PROXYRESPONSEWRITER_HANDLEEVENT =
                    MODULE_PREFIX_NAME + MODULE_PROXYRESPONSEWRITER_PREFIX_NAME + "handleEvent.";
}
