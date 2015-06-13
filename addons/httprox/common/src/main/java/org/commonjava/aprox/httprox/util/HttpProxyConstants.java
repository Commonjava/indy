package org.commonjava.aprox.httprox.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

public final class HttpProxyConstants
{

    public static final String PROXY_REPO_PREFIX = "httprox_";

    public static final String GET_METHOD = "GET";

    public static final String HEAD_METHOD = "HEAD";

    public static final String OPTIONS_METHOD = "OPTIONS";

    public static final Set<String> ALLOWED_METHODS =
        Collections.unmodifiableSet( new HashSet<>( Arrays.asList( GET_METHOD, HEAD_METHOD, OPTIONS_METHOD ) ) );

    public static final String ALLOW_HEADER_VALUE = StringUtils.join( ALLOWED_METHODS, "," );

    public static final String PROXY_AUTHENTICATE_FORMAT = "Basic realm=\"%s\"";

    private HttpProxyConstants()
    {
    }

}
