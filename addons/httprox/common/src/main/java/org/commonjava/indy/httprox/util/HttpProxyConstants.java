/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.httprox.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

public final class HttpProxyConstants
{
    public static final String PROXY_METRIC_LOGGER = "org.commonjava.topic.httprox.inbound";

    public static final String PROXY_REPO_PREFIX = "httprox_";

    public static final String GET_METHOD = "GET";

    public static final String HEAD_METHOD = "HEAD";

    public static final String CONNECT_METHOD = "CONNECT";

    public static final String OPTIONS_METHOD = "OPTIONS";

    public static final Set<String> ALLOWED_METHODS =
        Collections.unmodifiableSet( new HashSet<>( Arrays.asList( GET_METHOD, HEAD_METHOD, OPTIONS_METHOD ) ) );

    public static final String ALLOW_HEADER_VALUE = StringUtils.join( ALLOWED_METHODS, "," );

    public static final String PROXY_AUTHENTICATE_FORMAT = "Basic realm=\"%s\"";

    private HttpProxyConstants()
    {
    }

}
