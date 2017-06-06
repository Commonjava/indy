package org.commonjava.indy.httprox.metrics;

import org.commonjava.indy.IndyMetricsNames;

/**
 * Created by xiabai on 6/1/17.
 */
public class IndyMetricsHttpProxyNames extends IndyMetricsNames
{
    private static final String MODULE_PREFIX_NAME = "org.commonjava.indy.httpProxy";

    private static final String MODULE_PROXYRESPONSEWRITER_PREFIX_NAME = ".ProxyResponseWriter.";

    public static final String METHOD_PROXYRESPONSEWRITER_HANDLEEVENT =
                    MODULE_PREFIX_NAME + MODULE_PROXYRESPONSEWRITER_PREFIX_NAME + "handleEvent.";
}
